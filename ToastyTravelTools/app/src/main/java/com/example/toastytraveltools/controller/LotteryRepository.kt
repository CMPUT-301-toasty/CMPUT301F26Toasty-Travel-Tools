/*
* LotteryRepository.kt
*
* Purpose: Controller repository between views and
* Firebase "lotteries" collection. Handles creating
* new lotteries
*
* Issues: None at the moment
 */
package com.example.toastytraveltools.controller

import com.example.toastytraveltools.model.Lottery
import com.google.firebase.firestore.FirebaseFirestore

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
}