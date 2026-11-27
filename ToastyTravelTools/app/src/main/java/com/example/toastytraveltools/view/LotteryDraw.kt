/*
LotteryDraw.kt

Purpose: DrawLottery Picks numberOfWinners from the waitlist randomly. Changes status
to "drawn" so it isn't drawn again. Records data like winners, waitlist and
spotsRemaining, and reports success or error drawDueLotteries finds all the lotteries
that are ready to be drawn. Apply drawLottery to that lottery and report result
claimLotterySpot allows the winner of the lottery to book dates. Validation
check for valid user, winner and applicable booking dates. Creates reservation
and changes spotsRemaining listenForLotteryWins and listenForLotteryLosses checks
Firestore and sends notification per person, not counting those with no notifications on
declineLotterySpot lets a winner turn down their reservation, they are recorded in
declinedWinners and can no longer reserve for that lottery
loadLotteryCampsites finds every campsite in a lottery so the winner can pick one, and
loadBookedDates finds the dates already reserved at those campsites so the winner is only
shown dates that are still available

Issues:
 - winners of lottery cannot be notified if they have notifications off
 - booked dates are matched by campsite name, so two campsites with the same name share bookings
*/


package com.example.toastytraveltools.view

import android.content.Context
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.example.toastytraveltools.model.Reservation
import com.example.toastytraveltools.model.UserProfile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun drawLottery(
    lotteryId: String,
    onSuccess: (Int) -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val lotteryRef = db.collection("lotteries").document(lotteryId)

    // all in one transaction so the lottery can only ever be drawn once
    db.runTransaction { tx ->
        val lottery = tx.get(lotteryRef).toObject(Lottery::class.java)
            ?: error("Lottery not found")

        check(lottery.status != "drawn") { "This lottery has already been drawn" }
        check(lottery.numberOfWinners > 0) { "Number of winners must be at least 1" }

        // the owner can't win their own lottery
        val entrants = lottery.waitlist.distinct().filter { it != lottery.ownerId }

        // pick the winners at random
        val winners = entrants
            .shuffled(SecureRandom())
            .take(lottery.numberOfWinners)

        tx.update(
            lotteryRef,
            mapOf(
                "status" to "drawn",
                "winners" to winners,
                "notifiedWinners" to emptyList<String>(),
                "notifiedLosers" to emptyList<String>(),
                "waitlist" to lottery.waitlist.filter { it !in winners },
                "spotsRemaining" to winners.size.toLong(),
                "drawnAt" to Timestamp.now()
            )
        )
        winners.size
    }
        .addOnSuccessListener { count -> onSuccess(count) }
        .addOnFailureListener { onError(it) }
}

// draws every lottery whose registration period has ended and hasn't been drawn yet
// call this when the app opens, drawLottery makes sure each lottery is only drawn once
fun drawDueLotteries(
    onDrawn: (String, Int) -> Unit = { _, _ -> },
    onError: (Exception) -> Unit = {}
) {
    FirebaseFirestore.getInstance()
        .collection("lotteries")
        .whereLessThanOrEqualTo("registrationEnd", Timestamp.now())
        .get()
        .addOnSuccessListener { snapshot ->

            snapshot.toObjects(Lottery::class.java)
                .filter { it.status != "drawn" && it.numberOfWinners > 0 }
                .forEach { lottery ->

                    drawLottery(
                        lotteryId = lottery.id,
                        onSuccess = { winners -> onDrawn(lottery.id, winners) },
                        onError = onError
                    )
                }
        }
        .addOnFailureListener { onError(it) }
}

fun claimLotterySpot(
    lotteryId: String,
    entrantId: String,
    campsiteId: String,
    startMillis: Long,
    endMillis: Long,
    guests: Int,
    onSuccess: (Reservation) -> Unit,
    onError: (Exception) -> Unit
) {
    if (endMillis < startMillis) {
        onError(IllegalArgumentException("End date must be on or after the start date"))
        return
    }
    if (startMillis < System.currentTimeMillis() - 24L * 60 * 60 * 1000) {
        onError(IllegalArgumentException("Start date cannot be in the past"))
        return
    }
    if (guests < 1) {
        onError(IllegalArgumentException("There must be at least 1 guest"))
        return
    }

    val db = FirebaseFirestore.getInstance()
    val lotteryRef = db.collection("lotteries").document(lotteryId)

    db.runTransaction { tx ->

        // reads first, writes after
        val lottery = tx.get(lotteryRef).toObject(Lottery::class.java)
            ?: error("Lottery not found")

        // the winner picks one of the lottery's campsites, it has to be one of them
        check(campsiteId in lottery.campsiteIds) { "That campsite is not part of this lottery" }
        val campsiteDoc = tx.get(db.collection("campsites").document(campsiteId))
        check(campsiteDoc.exists()) { "Campsite not found" }
        val campsite = campsiteDoc.toCampsiteSafely()

        val entrant = tx.get(db.collection("entrants").document(entrantId))
            .toObject(UserProfile::class.java)

        // the reservations other winners already made, read from the "reservations" collection
        // so a reservation that was cancelled doesn't block those dates anymore
        val otherReservations = lottery.reservations
            .filter { it.entrant_id != entrantId }
            .mapNotNull { r ->
                tx.get(db.collection("reservations").document(r.id))
                    .toObject(Reservation::class.java)
            }

        check(entrantId in lottery.winners) { "Only lottery winners can claim a spot" }

        // one reservation per winner
        check(lottery.reservations.none { it.entrant_id == entrantId }) {
            "You have already claimed your spot"
        }

        // winners who declined can't change their mind later
        check(entrantId !in lottery.declinedWinners) {
            "You declined this reservation, so you can no longer reserve for this lottery"
        }

        // a cancelled reservation still uses up the win
        check(entrantId !in lottery.cancelledWinners) {
            "Your reservation for this lottery was cancelled, so you can no longer reserve for it"
        }

        // dates have to be inside the booking window (compared by day)
        val bookingStart = lottery.bookingStartDate?.toDate()?.time?.let { startOfDay(it) }
        val bookingEnd = lottery.bookingEndDate?.toDate()?.time?.let { endOfDay(it) }
        check(bookingStart == null || startMillis >= bookingStart) {
            "Your start date is before the booking dates open"
        }
        check(bookingEnd == null || endMillis <= bookingEnd) {
            "Your end date is after the booking dates close"
        }

        check(campsite.occupancyLimit <= 0 || guests <= campsite.occupancyLimit) {
            "This campsite allows a maximum of ${campsite.occupancyLimit} guests"
        }

        check(lottery.spotsRemaining > 0) { "All spots have been claimed by other winners" }

        // two winners can't book the same days at the same campsite
        val bookedByOthers = otherReservations
            .filter { r -> r.campsiteName == campsite.name }
            .map { r ->
                Pair(startOfDay(r.startDate.toDate().time), endOfDay(r.endDate.toDate().time))
            }
        check(!overlapsBookedDates(startMillis, endMillis, bookedByOthers)) {
            "Some of those dates were just booked by another winner, please pick different dates"
        }

        val reservation = Reservation(
            id = "${lotteryId}_$entrantId",
            campsiteName = campsite.name,
            location = campsite.location,
            entrantName = entrant?.name ?: "",
            entrant_id = entrantId,
            startDate = Timestamp(Date(startMillis)),
            endDate = Timestamp(Date(endMillis)),
            guests = guests
        )

        tx.update(
            lotteryRef,
            mapOf(
                "spotsRemaining" to (lottery.spotsRemaining - 1).toLong(),
                "reservations" to FieldValue.arrayUnion(reservation)
            )
        )

        // the reservations screen reads from the "reservations" collection
        tx.set(
            db.collection("reservations").document(reservation.id),
            mapOf(
                "lotteryId" to lotteryId,
                "campsiteName" to reservation.campsiteName,
                "location" to reservation.location,
                "entrantName" to reservation.entrantName,
                "entrantId" to entrantId,
                "entrant_id" to entrantId,
                "startDate" to reservation.startDate,
                "endDate" to reservation.endDate,
                "guests" to guests.toLong()
            )
        )
        reservation
    }
        .addOnSuccessListener { reservation -> onSuccess(reservation) }
        .addOnFailureListener { onError(it) }
}

// finds the names of the lottery's campsites, joined with commas, empty if none can be found
private fun withCampsiteName(lottery: Lottery, onResult: (String) -> Unit) {
    loadLotteryCampsites(
        lottery = lottery,
        onResult = { campsites -> onResult(campsites.joinToString(", ") { it.name }) },
        onError = { onResult("") }
    )
}

fun loadCampsiteName(
    campsiteId: String,
    onResult: (String) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("campsites")
        .document(campsiteId)
        .get()
        .addOnSuccessListener { doc ->
            onResult(doc.toObject(Campsite::class.java)?.name ?: "")
        }
        .addOnFailureListener { onResult("") }
}

fun loadClaimableLotteries(
    entrantId: String,
    onResult: (List<Lottery>) -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("lotteries")
        .whereArrayContains("winners", entrantId)
        .get()
        .addOnSuccessListener { snapshot ->
            onResult(
                snapshot.toObjects(Lottery::class.java).filter { lottery ->
                    lottery.spotsRemaining > 0 &&
                            lottery.reservations.none { it.entrant_id == entrantId } &&
                            entrantId !in lottery.declinedWinners &&
                            entrantId !in lottery.cancelledWinners
                }
            )
        }
        .addOnFailureListener { onError(it) }
}

// a winner turns down their reservation
// they are added to declinedWinners so they can never reserve for this lottery again
fun declineLotterySpot(
    lotteryId: String,
    entrantId: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val lotteryRef = db.collection("lotteries").document(lotteryId)

    db.runTransaction { tx ->
        val lottery = tx.get(lotteryRef).toObject(Lottery::class.java)
            ?: error("Lottery not found")

        check(entrantId in lottery.winners) { "Only lottery winners can decline a reservation" }
        check(lottery.reservations.none { it.entrant_id == entrantId }) {
            "You have already reserved your spot, cancel it from your reservations instead"
        }
        check(entrantId !in lottery.declinedWinners) { "You have already declined this reservation" }
        check(entrantId !in lottery.cancelledWinners) { "Your reservation for this lottery was already cancelled" }

        tx.update(
            lotteryRef,
            mapOf(
                "declinedWinners" to FieldValue.arrayUnion(entrantId),
                // this winner's spot won't be used anymore
                "spotsRemaining" to maxOf(lottery.spotsRemaining - 1, 0).toLong()
            )
        )
        null
    }
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

// turns a date saved in Firestore into millis
// works with Timestamps and the older yyyy-MM-dd strings
private fun Any?.toBookingMillis(): Long? {
    return when (this) {
        is Timestamp -> this.toDate().time
        is Date -> this.time
        is String -> {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(this)?.time
            } catch (e: Exception) {
                null
            }
        }
        else -> null
    }
}

// loads every campsite that is part of the lottery, so the winner can pick one
fun loadLotteryCampsites(
    lottery: Lottery,
    onResult: (List<Campsite>) -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val reads = lottery.campsiteIds.distinct().map { id ->
        db.collection("campsites").document(id).get()
    }

    Tasks.whenAllSuccess<DocumentSnapshot>(reads)
        .addOnSuccessListener { docs ->
            // skip campsites that were deleted
            onResult(docs.filter { it.exists() }.map { it.toCampsiteSafely() })
        }
        .addOnFailureListener { onError(it) }
}

// loads the dates that are already reserved at each campsite
// the map key is the campsite id, each pair is (start of the first day, end of the last day) in millis
fun loadBookedDates(
    campsites: List<Campsite>,
    onResult: (Map<String, List<Pair<Long, Long>>>) -> Unit,
    onError: (Exception) -> Unit
) {
    val names = campsites.map { it.name }.filter { it.isNotBlank() }.distinct()
    if (names.isEmpty()) {
        onResult(emptyMap())
        return
    }

    FirebaseFirestore.getInstance()
        .collection("reservations")
        .whereIn("campsiteName", names)
        .get()
        .addOnSuccessListener { snapshot ->
            val booked = mutableMapOf<String, MutableList<Pair<Long, Long>>>()
            for (doc in snapshot.documents) {
                val start = (doc.get("startDate") ?: doc.get("StartDate")).toBookingMillis()
                val end = (doc.get("endDate") ?: doc.get("EndDate")).toBookingMillis()
                if (start == null || end == null) continue

                // reservations are linked to a campsite by its name
                val resCampsiteName = doc.getString("campsiteName") ?: ""
                val campsite = campsites.find { it.name == resCampsiteName } ?: continue

                booked.getOrPut(campsite.id) { mutableListOf() }
                    .add(Pair(startOfDay(start), endOfDay(end)))
            }
            onResult(booked)
        }
        .addOnFailureListener { onError(it) }
}

// true if any part of start..end lands on a booked date
fun overlapsBookedDates(start: Long, end: Long, booked: List<Pair<Long, Long>>): Boolean {
    return booked.any { (bookedStart, bookedEnd) ->
        start <= bookedEnd && end >= bookedStart
    }
}

// true if at least one day in the booking window is not in the past and not booked
fun hasAvailableDates(lottery: Lottery, booked: List<Pair<Long, Long>>): Boolean {
    val today = startOfDay(System.currentTimeMillis())
    val windowStart = lottery.bookingStartDate?.toDate()?.time?.let { startOfDay(it) } ?: today
    // no end date means the booking window never closes
    val windowEnd = lottery.bookingEndDate?.toDate()?.time?.let { endOfDay(it) } ?: return true

    val c = Calendar.getInstance()
    c.timeInMillis = maxOf(windowStart, today)
    while (c.timeInMillis <= windowEnd) {
        val dayStart = startOfDay(c.timeInMillis)
        if (!overlapsBookedDates(dayStart, endOfDay(dayStart), booked)) {
            return true
        }
        c.add(Calendar.DAY_OF_MONTH, 1)
    }
    return false
}

fun startOfDay(millis: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun endOfDay(millis: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 999)
    return c.timeInMillis
}

// listens for lotteries this entrant has won and sends one notification per win
fun listenForLotteryWins(
    context: Context,
    entrantId: String
): ListenerRegistration {

    val db = FirebaseFirestore.getInstance()

    return db.collection("lotteries")
        .whereArrayContains("winners", entrantId)
        .addSnapshotListener { snapshot, error ->

            if (error != null) return@addSnapshotListener

            snapshot?.toObjects(Lottery::class.java)?.forEach { lottery ->

                if (entrantId in lottery.notifiedWinners) return@forEach

                val lotteryRef = db.collection("lotteries").document(lottery.id)

                // add this entrant to the notified list first so it is only ever sent once
                db.runTransaction { tx ->
                    val fresh = tx.get(lotteryRef).toObject(Lottery::class.java)

                    if (fresh == null || entrantId in fresh.notifiedWinners) {
                        false
                    } else {
                        tx.update(lotteryRef, "notifiedWinners", FieldValue.arrayUnion(entrantId))
                        true
                    }
                }.addOnSuccessListener { claimed ->
                    if (!claimed) return@addOnSuccessListener

                    // people who turned notifications off don't get one
                    db.collection("entrants").document(entrantId).get()
                        .addOnSuccessListener { profile ->
                            val enabled = profile.toObject(UserProfile::class.java)
                                ?.notificationsEnabled ?: true
                            if (!enabled) return@addOnSuccessListener

                            // look up which campsite they won
                            // look up which campsite they won
                            withCampsiteName(lottery) { campsiteName ->
                                LotteryNotifier.send(context, lottery, true, campsiteName)
                            }
                        }
                }
            }
        }
}


// listens for drawn lotteries this entrant didn't win and sends one "you did not win" notification
fun listenForLotteryLosses(
    context: Context,
    entrantId: String
): ListenerRegistration {

    val db = FirebaseFirestore.getInstance()

    // after the draw, everyone who didn't win is left on the waitlist
    return db.collection("lotteries")
        .whereArrayContains("waitlist", entrantId)
        .addSnapshotListener { snapshot, error ->

            if (error != null) return@addSnapshotListener

            snapshot?.toObjects(Lottery::class.java)
                ?.filter { it.status == "drawn" && it.ownerId != entrantId }
                ?.forEach { lottery ->

                    if (entrantId in lottery.notifiedLosers) return@forEach

                    val lotteryRef = db.collection("lotteries").document(lottery.id)

                    // add this entrant to the notified list first so it is only ever sent once
                    db.runTransaction { tx ->
                        val fresh = tx.get(lotteryRef).toObject(Lottery::class.java)

                        if (fresh == null || fresh.status != "drawn" ||
                            entrantId in fresh.notifiedLosers
                        ) {
                            false
                        } else {
                            tx.update(
                                lotteryRef,
                                "notifiedLosers",
                                FieldValue.arrayUnion(entrantId)
                            )
                            true
                        }
                    }.addOnSuccessListener { claimed ->
                        if (!claimed) return@addOnSuccessListener

                        // people who turned notifications off don't get one
                        db.collection("entrants").document(entrantId).get()
                            .addOnSuccessListener { profile ->
                                val enabled = profile.toObject(UserProfile::class.java)
                                    ?.notificationsEnabled ?: true
                                if (enabled) {
                                    if (enabled) {
                                        withCampsiteName(lottery) { campsiteName ->
                                            LotteryNotifier.send(context, lottery, false, campsiteName)
                                        }
                                    }
                                }
                            }
                    }
                }
        }
}