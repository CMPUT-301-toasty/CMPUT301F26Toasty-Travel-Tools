/*
 * UserProfile.kt
 *
 * Purpose: Model class for the user, could be an entrant, campsite owner
 * or admin), as well as their contact info/settings
 *
 * Issues:
 * - Could enforce role to be specific strings instead of anything
 * - Validate/error check email and phone number format
 */

package com.example.toastytraveltools.model

/**
 * A user's profile information and settings
 *
 * @property name the user's name
 * @property email the user's email
 * @property phone the user's phone number; can be null
 * @property deviceId use the device to identify the user
 * @property role the user's role, can be entrant(default), owner, admin
 * @property notificationsEnabled whether the user wants notifications, default is true
 *
 */

class UserProfile (
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val deviceId: String = "",
    val role: String = "entrant", // entrant, owner, or admin
    val notificationsEnabled: Boolean = true
)