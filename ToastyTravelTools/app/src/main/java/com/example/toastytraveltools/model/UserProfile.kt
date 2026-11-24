/**
 * Represents a user's profile and application preferences.
 *
 * Used to store information for entrants, campsite owners,
 * and administrators in Firestore.
 *
 * @property name User's display name.
 * @property email User's email address.
 * @property phone User's phone number.
 * @property deviceId Device identifier associated with the user.
 * @property role User's application role (entrant, owner, or admin).
 * @property notificationsEnabled Whether the user has enabled notifications.
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