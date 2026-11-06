/*
 * Campsite.kt
 *
 * Purpose: Model class that represents a campsite
 * Relates back to documents in Firestore "campsites" collection
 *
 * Issues:
 * - ImagesURL could be altered to hold multiple images later on
 * - Should be more error checking/validation on OccupancyLimit and Rating
 */
package com.example.toastytraveltools.model

import com.google.firebase.firestore.DocumentId

/**
 * A campsite that entrants can browse and enter lotteries for
 *
 * @property id Firestore document ID, automatically made in creation of collection
 * @property Name display name of the campsite
 * @property Description text detailing the campsite
 * @property Location where the campsite is
 * @property OccupancyLimit max number of people in a campsite
 * @property ImagesUrl URL of the campsite image(s)
 * @property Rating campsite rating on a scale
 * @property Tags labels for each campsite
 * @property QrCodeUrl URL of the campsite's QR code image
 * @property ownerId user ID of the campsite owner who created the campsite
 */

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