package com.example.toastytraveltools.model

import com.google.firebase.Timestamp

data class Reservation (
    val id: String = "",
    val campsiteName: String = "",
    val location: String = "",
    val entrantName: String = "",
    val startDate: Timestamp = Timestamp(0, 0),
    val endDate: Timestamp = Timestamp(0, 0),
    val guests: Int = 0,
    var reviewScore: Int? = null
)