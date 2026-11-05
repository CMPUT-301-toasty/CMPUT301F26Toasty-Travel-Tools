package com.example.toastytraveltools

import com.google.firebase.firestore.FirebaseFirestore

class LotteryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val lotteriesRef = db.collection("lotteries")

    fun addLottery(lottery: Lottery, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val documentRef = lotteriesRef.document()
        val newLottery = lottery.copy(id = documentRef.id)
        documentRef.set(newLottery)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
