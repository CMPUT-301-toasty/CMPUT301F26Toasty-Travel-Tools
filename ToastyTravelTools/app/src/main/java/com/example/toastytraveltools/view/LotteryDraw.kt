package com.example.toastytraveltools.view

import android.content.Context
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.example.toastytraveltools.model.Reservation
import com.example.toastytraveltools.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.SecureRandom
import java.util.Calendar
import java.util.Date

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

        val campsiteId = lottery.campsiteIds.firstOrNull() ?: error("Lottery has no campsite")
        val campsite = tx.get(db.collection("campsites").document(campsiteId))
            .toObject(Campsite::class.java) ?: error("Campsite not found")

        val entrant = tx.get(db.collection("entrants").document(entrantId))
            .toObject(UserProfile::class.java)

        check(entrantId in lottery.winners) { "Only lottery winners can claim a spot" }

        // one reservation per winner
        check(lottery.reservations.none { it.entrantId == entrantId }) {
            "You have already claimed your spot"
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

        val reservation = Reservation(
            id = "${lotteryId}_$entrantId",
            campsiteName = campsite.name,
            location = campsite.location,
            entrantName = entrant?.name ?: "",
            entrantId = entrantId,
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

// finds the name of the lottery's campsite, empty if it can't be found
private fun withCampsiteName(lottery: Lottery, onResult: (String) -> Unit) {
    val campsiteId = lottery.campsiteIds.firstOrNull()
    if (campsiteId == null) {
        onResult("")
    } else {
        loadCampsiteName(campsiteId, onResult)
    }
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
                            lottery.reservations.none { it.entrantId == entrantId }
                }
            )
        }
        .addOnFailureListener { onError(it) }
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