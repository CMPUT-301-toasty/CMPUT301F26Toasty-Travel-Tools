package com.example.toastytraveltools

import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Reservation
import com.example.toastytraveltools.view.isCampsiteOwner
import com.example.toastytraveltools.view.ownedCampsites
import com.example.toastytraveltools.view.reservationsForCampsite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnershipTests {

    @Test
    fun owner_isTheDeviceThatListedTheCampsite() {
        assertTrue(isCampsiteOwner(ownerId = "device-a", deviceId = "device-a"))
    }

    @Test
    fun otherDevice_isNotOwner() {
        assertFalse(isCampsiteOwner(ownerId = "device-a", deviceId = "device-b"))
    }

    @Test
    fun blankIds_neverMatch() {
        // legacy campsites have no ownerId; a missing device id must not claim them
        assertFalse(isCampsiteOwner(ownerId = "", deviceId = ""))
        assertFalse(isCampsiteOwner(ownerId = "", deviceId = "device-a"))
        assertFalse(isCampsiteOwner(ownerId = "device-a", deviceId = ""))
    }

    @Test
    fun ownedCampsites_keepsOnlyThisDevicesCampsites() {
        val sites = listOf(
            Campsite(id = "1", name = "Mine", ownerId = "me"),
            Campsite(id = "2", name = "Theirs", ownerId = "them"),
            Campsite(id = "3", name = "Legacy", ownerId = ""),
            Campsite(id = "4", name = "Also mine", ownerId = "me")
        )
        assertEquals(listOf("1", "4"), ownedCampsites(sites, "me").map { it.id })
        assertTrue(ownedCampsites(sites, "").isEmpty())
    }

    @Test
    fun reservationsForCampsite_filtersByName_orKeepsAll() {
        val reservations = listOf(
            Reservation(id = "r1", campsiteName = "Lakeview"),
            Reservation(id = "r2", campsiteName = "Pine Ridge"),
            Reservation(id = "r3", campsiteName = "Lakeview")
        )
        assertEquals(listOf("r1", "r3"), reservationsForCampsite(reservations, "Lakeview").map { it.id })
        assertEquals(3, reservationsForCampsite(reservations, null).size)
        assertTrue(reservationsForCampsite(reservations, "Unknown").isEmpty())
    }
}
