package com.example.toastytraveltools

import java.util.Date

data class Lottery(
    val campsiteName: String = "",
    val registrationStartDate: Date = Date(),
    val registrationEndDate: Date = Date(),
    val bookStartDate: Date = Date(),
    val bookEndDate: Date = Date()
)