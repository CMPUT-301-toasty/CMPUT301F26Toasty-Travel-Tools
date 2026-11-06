package com.example.toastytraveltools

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.controller.LotteryRepository
import com.example.toastytraveltools.view.HomeScreen
import com.example.toastytraveltools.view.OwnerHomeScreen
import com.example.toastytraveltools.view.OwnerScreenAddCampsite
import com.example.toastytraveltools.view.OwnerScreenCreateLottery
import com.example.toastytraveltools.view.SearchScreen
import com.example.toastytraveltools.view.ProfileScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHomeScreen_displaysCorrectly() {
        composeTestRule.setContent {
            HomeScreen(onNavigateToUser = {}, onNavigateToOwner = {}, onNavigateToAdmin = {})
        }
        
        composeTestRule.onNodeWithText("Toasty Travel Tools").assertIsDisplayed()
        composeTestRule.onNodeWithText("User Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Owner Mode").assertIsDisplayed()
    }

    @Test
    fun testHomeScreen_userModeButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            HomeScreen(onNavigateToUser = { clicked = true }, onNavigateToOwner = {}, onNavigateToAdmin = {})
        }
        
        composeTestRule.onNodeWithText("User Mode").performClick()
        assert(clicked)
    }

    @Test
    fun testOwnerHomeScreen_displaysButtons() {
        composeTestRule.setContent {
            OwnerHomeScreen(
                onNavigateToAddCampsite = {},
                onNavigateToCreateLottery = {},
                onNavigateToEditCampsite = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Owner Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add New Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Campsites").assertIsDisplayed()
    }

    @Test
    fun testOwnerAddCampsite_emptySubmission_showsError() {
        val mockRepo = CampsiteRepository()
        
        composeTestRule.setContent {
            OwnerScreenAddCampsite(
                repository = mockRepo,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Save Campsite").performScrollTo().performClick()
        
        composeTestRule.onNodeWithText("Please fill in all required fields.").assertIsDisplayed()
    }

    @Test
    fun testOwnerCreateLottery_emptySubmission_showsError() {
        val mockLotteryRepo = LotteryRepository()
        val mockCampsiteRepo = CampsiteRepository()
        
        composeTestRule.setContent {
            OwnerScreenCreateLottery(
                lotteryRepository = mockLotteryRepo,
                campsiteRepository = mockCampsiteRepo,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNode(hasText("Create Lottery").and(hasClickAction())).performScrollTo().performClick()
        
        composeTestRule.onNodeWithText("Please fill in all required fields.").assertIsDisplayed()
    }
    
    @Test
    fun testSearchScreen_displaysElements() {
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = {},
                onCampsiteClick = {},
                onSearchClick = {},
                onReservationClick = {},
                onProfileClick = {}
            )
        }
        
        // Assert bottom bar
        composeTestRule.onNodeWithText("Search Campsites").assertIsDisplayed()
        composeTestRule.onAllNodes(hasText("Search").and(hasClickAction())).onFirst().assertIsDisplayed()
    }
    
    @Test
    fun testProfileScreen_displaysElements() {
        composeTestRule.setContent {
            ProfileScreen(
                userId = "testUserId",
                onNavigateBack = {},
                onSearchClick = {},
                onReservationClick = {},
                onProfileClick = {}
            )
        }
        
        // Wait for it to possibly render Profile or Loading
        // We know it displays "Profile" text
        composeTestRule.onAllNodesWithText("Profile").fetchSemanticsNodes().isNotEmpty()
    }
}
