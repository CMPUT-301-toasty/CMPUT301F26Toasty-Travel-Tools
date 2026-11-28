/*
* LotteryRepository.kt
*
* Purpose: Controller repository between views and
* Firebase "lotteries" collection. Handles creating
* new lotteries, editing them, joining a waitlist (which
* respects the waitlist cap) and listening to lotteries
* for the lottery details screen
*
* Issues: None at the moment
 */
package com.example.toastytraveltools.controller

import com.example.toastytraveltools.model.Lottery
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

/**
 * Manages lottery data stored in the "lotteries" collection
 */

class LotteryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val lotteriesRef = db.collection("lotteries")

    /**
     * Adds new lottery to Firebase
     *
     * New document ID is created and stored in the 'id' field
     *
     * @param lottery the lottery to add
     * @param onSuccess called after the lottery is saved
     * @param onFailure called with exception when the save doesn't work
     */
    fun addLottery(lottery: Lottery, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val documentRef = lotteriesRef.document()
        val newLottery = lottery.copy(id = documentRef.id)
        documentRef.set(newLottery)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Listens to one lottery so its details stay up to date
     *
     * @param lotteryId Firestore document ID of the lottery
     * @param onResult called with the lottery, or null if it doesn't exist
     * @param onFailure called with exception when the read doesn't work
     * @return the registration, remove it when the screen closes
     */
    fun listenToLottery(
        lotteryId: String,
        onResult: (Lottery?) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration =
        lotteriesRef.document(lotteryId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onFailure(error)
                return@addSnapshotListener
            }
            onResult(snapshot?.toObject(Lottery::class.java)?.apply { id = snapshot.id })
        }

    /**
     * Listens to every lottery that includes a campsite
     *
     * @param campsiteId Firestore document ID of the campsite
     * @param onResult called with the lotteries, newest registration first
     * @param onFailure called with exception when the read doesn't work
     * @return the registration, remove it when the screen closes
     */
    fun listenToLotteriesForCampsite(
        campsiteId: String,
        onResult: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration =
        lotteriesRef.whereArrayContains("campsiteIds", campsiteId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onFailure(error)
                return@addSnapshotListener
            }
            val lotteries = snapshot?.documents.orEmpty().mapNotNull { doc ->
                doc.toObject(Lottery::class.java)?.apply { id = doc.id }
            }
            onResult(lotteries.sortedByDescending { it.registrationStart })
        }

    /**
     * Saves an owner's changes to a lottery
     *
     * @param lotteryId Firestore document ID of the lottery
     * @param updates the fields to change, a null value clears that field
     * @param onSuccess called after the changes are saved
     * @param onFailure called with exception when the save doesn't work
     */
    fun updateLottery(
        lotteryId: String,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        lotteriesRef.document(lotteryId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Adds an entrant to a lottery's waitlist, unless the waitlist is at its cap.
     * Runs in a transaction so two people can't both take the last place.
     *
     * @param lotteryId Firestore document ID of the lottery
     * @param entrantId device ID of the entrant joining
     * @param onSuccess called after the entrant is on the waitlist
     * @param onFull called when the waitlist has no places left
     * @param onFailure called with exception when the join doesn't work
     */
    fun joinWaitlist(
        lotteryId: String,
        entrantId: String,
        onSuccess: () -> Unit,
        onFull: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val lotteryRef = lotteriesRef.document(lotteryId)
        db.runTransaction { tx ->
            val lottery = tx.get(lotteryRef).toObject(Lottery::class.java)
                ?: throw FirebaseFirestoreException("Lottery not found", FirebaseFirestoreException.Code.NOT_FOUND)
            if (entrantId in lottery.waitlist) return@runTransaction true
            val cap = lottery.waitlistCapacity
            if (cap != null && lottery.waitlist.distinct().size >= cap) return@runTransaction false
            tx.update(lotteryRef, "waitlist", FieldValue.arrayUnion(entrantId))
            true
        }
            .addOnSuccessListener { joined -> if (joined) onSuccess() else onFull() }
            .addOnFailureListener { onFailure(it) }
    }
}
