package com.example.toastytraveltools

import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.example.toastytraveltools.model.Reservation
import com.example.toastytraveltools.model.UserProfile
import com.example.toastytraveltools.view.validateEmail
import com.example.toastytraveltools.view.validateName
import com.example.toastytraveltools.view.validatePhone
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class ModelAndValidationTests {

    // --- Model Tests ---

    @Test
    fun testCampsiteModel_defaultValues() {
        val campsite = Campsite()
        assertEquals("", campsite.id)
        assertEquals("", campsite.Name)
        assertEquals(0, campsite.OccupancyLimit)
        assertEquals(0.0, campsite.Rating, 0.0)
        assertTrue(campsite.Tags.isEmpty())
    }

    @Test
    fun testCampsiteModel_assignedValues() {
        val campsite = Campsite(
            id = "c1",
            Name = "Pine Creek",
            OccupancyLimit = 4,
            Tags = listOf("wifi", "water")
        )
        assertEquals("c1", campsite.id)
        assertEquals("Pine Creek", campsite.Name)
        assertEquals(4, campsite.OccupancyLimit)
        assertEquals(2, campsite.Tags.size)
    }

    @Test
    fun testLotteryModel_defaultValues() {
        val lottery = Lottery()
        assertEquals("", lottery.id)
        assertTrue(lottery.campsiteIds.isEmpty())
        assertNull(lottery.registrationStart)
        assertEquals(0, lottery.numberOfWinners)
    }

    @Test
    fun testLotteryModel_dateAssignments() {
        val now = Timestamp(Date())
        val lottery = Lottery(
            id = "L1",
            registrationStart = now,
            numberOfWinners = 5
        )
        assertEquals("L1", lottery.id)
        assertEquals(now, lottery.registrationStart)
        assertEquals(5, lottery.numberOfWinners)
    }

    @Test
    fun testReservationModel_defaultValues() {
        val reservation = Reservation()
        assertEquals("", reservation.id)
        assertEquals(Timestamp(0, 0), reservation.startDate)
        assertEquals(0, reservation.guests)
    }

    @Test
    fun testUserProfileModel_defaultValues() {
        val profile = UserProfile()
        assertEquals("", profile.name)
        assertEquals("", profile.email)
        assertEquals("", profile.phone)
    }

    // --- Controller/View Validation Logic Tests ---

    @Test
    fun testValidateName_emptyReturnsError() {
        assertNotNull(validateName(""))
        assertNotNull(validateName("   "))
    }

    @Test
    fun testValidateName_validReturnsNull() {
        assertNull(validateName("John Doe"))
    }

    @Test
    fun testValidateEmail_emptyReturnsError() {
        assertNotNull(validateEmail(""))
    }
}
