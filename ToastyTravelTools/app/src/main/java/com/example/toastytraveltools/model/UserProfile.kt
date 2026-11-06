package com.example.toastytraveltools.model

data class UserProfile (
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val deviceId: String = "",
    val role: String = "entrant", // entrant, owner, or admin
    val notificationsEnabled: Boolean = true
)