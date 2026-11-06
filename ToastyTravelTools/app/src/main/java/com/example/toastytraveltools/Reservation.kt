package com.example.toastytraveltools

data class Reservation (
    val id: String = "",
    val campsiteName: String = "",
    val location: String = "",
    val entrantName: String = "",
    val entrantId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val guests: Int = 0,
    val campsiteId: String = ""
)