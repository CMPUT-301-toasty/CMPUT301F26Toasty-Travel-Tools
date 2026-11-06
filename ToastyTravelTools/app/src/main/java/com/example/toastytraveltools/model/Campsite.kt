package com.example.toastytraveltools.model

import com.google.firebase.firestore.DocumentId

data class Campsite (
    @DocumentId var id : String = "",
    val name : String = "",
    val description : String = "",
    val location : String = "",
    val occupancyLimit : Int = 0,
    val imagesUrl: String = "",
    var rating: Double = 0.0,
    val tags: List<String> = emptyList(),
    val qrCodeUrl: String = "",
    val ownerId: String = ""
)