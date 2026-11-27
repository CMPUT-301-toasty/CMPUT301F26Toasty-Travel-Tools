/*
Ownership.kt

Purpose: How the app decides who owns a campsite. There is no login: the current user is the
device (Settings.Secure.ANDROID_ID), and a campsite's ownerId is set to the creating device's
id. Anyone who lists a campsite becomes its owner.
 */

package com.example.toastytraveltools.view

import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Reservation

/**
 * True if the device [deviceId] owns a campsite whose ownerId is [ownerId].
 * Blank ids never match, so legacy campsites with no ownerId have no owner.
 */
fun isCampsiteOwner(ownerId: String, deviceId: String): Boolean =
    ownerId.isNotBlank() && deviceId.isNotBlank() && ownerId == deviceId

/** Campsites owned by [deviceId]. */
fun ownedCampsites(campsites: List<Campsite>, deviceId: String): List<Campsite> =
    campsites.filter { isCampsiteOwner(it.ownerId, deviceId) }

/**
 * Narrows owner bookings to one campsite. Reservations only store the campsite name,
 * so the match is by name. A null [campsiteName] keeps every booking.
 */
fun reservationsForCampsite(reservations: List<Reservation>, campsiteName: String?): List<Reservation> =
    if (campsiteName == null) reservations
    else reservations.filter { it.campsiteName == campsiteName }
