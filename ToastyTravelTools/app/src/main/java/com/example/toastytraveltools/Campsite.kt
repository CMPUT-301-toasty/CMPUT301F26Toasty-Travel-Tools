package com.example.toastytraveltools

import com.google.firebase.firestore.DocumentId

data class Campsite (
    @DocumentId var id : String = "",
    val Name : String = "",
    val Description : String = "",
    val Location : String = "",
    val OccupancyLimit : Int = 0,
    val ImagesUrl: String = "",
    val Rating: Double = 0.0,
    val Tags: List<String> = emptyList(),
    val QrCodeUrl: String = ""
)
