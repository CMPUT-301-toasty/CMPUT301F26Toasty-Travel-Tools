package com.example.toastytraveltools

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.controller.LotteryRepository
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.view.CampsiteActionsSection
import com.example.toastytraveltools.view.OwnerScreenAddCampsite
import com.example.toastytraveltools.view.OwnerScreenCreateLottery
import com.example.toastytraveltools.model.Lottery
import com.example.toastytraveltools.view.ProfileHostingSection
import com.example.toastytraveltools.view.ProfileScreen
import com.example.toastytraveltools.view.SearchScreen
import com.example.toastytraveltools.view.components.AdminGate
import com.example.toastytraveltools.view.components.MainTab
import com.example.toastytraveltools.view.components.ToastyMainBottomBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the single-app flow (no User/Owner/Admin modes).
 * Ownership and admin role are passed in as parameters so these don't depend on live
 * Firestore data.
 */
@RunWith(AndroidJUnit4::class)
class UITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ---- Explore (start screen) ----

    @Test
    fun explore_showsAppTitle_addAction_searchField_andBottomBar() {
        composeTestRule.setContent {
            SearchScreen(onCampsiteClick = {})
        }

        composeTestRule.onNodeWithText("Toasty Travel Tools").assertIsDisplayed()
        composeTestRule.onNodeWithTag("addCampsiteAction").assertIsDisplayed().assertHasClickAction()
        // accessible label for the add action (what TalkBack reads)
        composeTestRule.onNodeWithContentDescription("List a campsite").assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Search for campsite").assertIsDisplayed()
        composeTestRule.onNode(hasText("Search").and(hasClickAction())).assertIsDisplayed()
        composeTestRule.onNodeWithText("Explore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reservations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun explore_hasNoModeButtons() {
        composeTestRule.setContent {
            SearchScreen(onCampsiteClick = {})
        }

        composeTestRule.onNodeWithText("User Mode").assertDoesNotExist()
        composeTestRule.onNodeWithText("Owner Mode").assertDoesNotExist()
        composeTestRule.onNodeWithText("Admin Mode").assertDoesNotExist()
    }

    @Test
    fun explore_addAction_opensAddCampsite() {
        var addClicked = false
        composeTestRule.setContent {
            SearchScreen(onCampsiteClick = {}, onAddCampsiteClick = { addClicked = true })
        }

        composeTestRule.onNodeWithTag("addCampsiteAction").performClick()
        assertTrue(addClicked)
    }

    @Test
    fun explore_bottomBar_tabsTriggerCallbacks() {
        var reservations = false
        var profileId: String? = null
        composeTestRule.setContent {
            SearchScreen(
                onCampsiteClick = {},
                onReservationClick = { reservations = true },
                onProfileClick = { profileId = it }
            )
        }

        composeTestRule.onNodeWithText("Reservations").performClick()
        composeTestRule.onNodeWithText("Profile").performClick()
        assertTrue(reservations)
        assertTrue(profileId != null)
    }

    @Test
    fun mainBottomBar_marksSelectedTab_andCallsEachTab() {
        val clicks = mutableListOf<String>()
        composeTestRule.setContent {
            ToastyMainBottomBar(
                selected = MainTab.Profile,
                onExploreClick = { clicks += "explore" },
                onReservationsClick = { clicks += "reservations" },
                onProfileClick = { clicks += "profile" }
            )
        }

        composeTestRule.onNodeWithText("Profile").assertIsSelected()
        composeTestRule.onNodeWithText("Explore").assertIsNotSelected()
        composeTestRule.onNodeWithText("Explore").performClick()
        composeTestRule.onNodeWithText("Reservations").performClick()
        composeTestRule.onNodeWithText("Profile").performClick()
        assertEquals(listOf("explore", "reservations", "profile"), clicks)
    }

    // ---- Add campsite form ----

    @Test
    fun addCampsite_emptySubmission_showsError() {
        composeTestRule.setContent {
            OwnerScreenAddCampsite(
                repository = CampsiteRepository(),
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Save Campsite").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Please fill in all required fields.").assertIsDisplayed()
    }

    // ---- Ownership on Campsite Details ----

    @Test
    fun details_owner_seesOwnerActions_andNoGuestActions() {
        composeTestRule.setContent {
            CampsiteActionsSection(
                isOwner = true,
                campsiteName = "Lakeview",
                onEdit = {},
                onDeleteConfirmed = {},
                onCreateLottery = {},
                onViewBookings = {}
            ) {
                Text("Join")
            }
        }

        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("View bookings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join").assertDoesNotExist()
    }

    @Test
    fun details_nonOwner_seesGuestView_andNoOwnerActions() {
        composeTestRule.setContent {
            CampsiteActionsSection(
                isOwner = false,
                campsiteName = "Lakeview",
                onEdit = {},
                onDeleteConfirmed = {},
                onCreateLottery = {},
                onViewBookings = {}
            ) {
                Text("Join")
            }
        }

        composeTestRule.onNodeWithText("Join").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit").assertDoesNotExist()
        composeTestRule.onNodeWithText("Delete").assertDoesNotExist()
        composeTestRule.onNodeWithText("Create lottery").assertDoesNotExist()
        composeTestRule.onNodeWithText("View bookings").assertDoesNotExist()
    }

    @Test
    fun details_ownerActions_triggerCallbacks() {
        val clicks = mutableListOf<String>()
        composeTestRule.setContent {
            CampsiteActionsSection(
                isOwner = true,
                campsiteName = "Lakeview",
                onEdit = { clicks += "edit" },
                onDeleteConfirmed = { clicks += "delete" },
                onCreateLottery = { clicks += "lottery" },
                onViewBookings = { clicks += "bookings" }
            ) {}
        }

        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Create lottery").performClick()
        composeTestRule.onNodeWithText("View bookings").performClick()
        assertEquals(listOf("edit", "lottery", "bookings"), clicks)
    }

    @Test
    fun details_delete_asksForConfirmation_andCancelKeepsCampsite() {
        var deleted = false
        composeTestRule.setContent {
            CampsiteActionsSection(
                isOwner = true,
                campsiteName = "Lakeview",
                onEdit = {},
                onDeleteConfirmed = { deleted = true },
                onCreateLottery = {},
                onViewBookings = {}
            ) {}
        }

        composeTestRule.onNodeWithText("Delete").performClick()

        composeTestRule.onNodeWithText("Delete campsite?").assertIsDisplayed()
        assertFalse("must not delete before confirming", deleted)

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete campsite?").assertDoesNotExist()
        assertFalse(deleted)
    }

    @Test
    fun details_delete_confirmed_deletes() {
        var deleted = false
        composeTestRule.setContent {
            CampsiteActionsSection(
                isOwner = true,
                campsiteName = "Lakeview",
                onEdit = {},
                onDeleteConfirmed = { deleted = true },
                onCreateLottery = {},
                onViewBookings = {}
            ) {}
        }

        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete campsite").performClick()

        assertTrue(deleted)
        composeTestRule.onNodeWithText("Delete campsite?").assertDoesNotExist()
    }

    // ---- Create lottery ----

    @Test
    fun createLottery_emptySubmission_showsError() {
        composeTestRule.setContent {
            OwnerScreenCreateLottery(
                lotteryRepository = LotteryRepository(),
                campsiteRepository = CampsiteRepository(),
                onNavigateBack = {}
            )
        }

        composeTestRule.onNode(hasText("Create Lottery").and(hasClickAction())).performScrollTo().performClick()

        composeTestRule.onNodeWithText("Please fill in all required fields.").assertIsDisplayed()
    }

    @Test
    fun createLottery_preselectedCampsite_stillRequiresDates() {
        composeTestRule.setContent {
            OwnerScreenCreateLottery(
                lotteryRepository = LotteryRepository(),
                campsiteRepository = CampsiteRepository(),
                onNavigateBack = {},
                initialCampsiteId = "some-campsite-id"
            )
        }

        composeTestRule.onNode(hasText("Create Lottery").and(hasClickAction())).performScrollTo().performClick()

        composeTestRule.onNodeWithText("Please fill in all required fields.").assertIsDisplayed()
    }

    // ---- Profile: My campsites, hosting entries, admin gating ----

    @Test
    fun profile_admin_seesAdminEntry() {
        var adminClicked = false
        composeTestRule.setContent {
            ProfileScreen(userId = "testUserId", isAdmin = true, onAdminClick = { adminClicked = true })
        }

        composeTestRule.onNodeWithText("Admin tools").performScrollTo().assertIsDisplayed()
        // the profile loads from Firestore and shifts the layout, so click via the semantics action
        composeTestRule.onNodeWithText("Admin tools").performScrollTo().performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.OnClick
        )
        assertTrue(adminClicked)
    }

    @Test
    fun profile_nonAdmin_doesNotSeeAdminEntry() {
        composeTestRule.setContent {
            ProfileScreen(userId = "testUserId", isAdmin = false)
        }

        composeTestRule.onNodeWithText("Bookings for my campsites").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Admin tools").assertDoesNotExist()
        composeTestRule.onNodeWithText("Admin", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun profile_listsMyCampsites_andOpensDetails() {
        var opened: String? = null
        var bookings = false
        composeTestRule.setContent {
            ProfileScreen(
                userId = "testUserId",
                myCampsites = listOf(
                    Campsite(id = "c1", name = "Lakeview", location = "Jasper", ownerId = "testUserId")
                ),
                onCampsiteClick = { opened = it },
                onOwnerBookingsClick = { bookings = true }
            )
        }

        composeTestRule.onNodeWithText("My campsites", ignoreCase = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Lakeview").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        assertEquals("c1", opened)
        composeTestRule.onNodeWithText("Bookings for my campsites").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        assertTrue(bookings)
    }

    @Test
    fun profile_noCampsites_showsEmptyState() {
        composeTestRule.setContent {
            ProfileScreen(userId = "testUserId")
        }

        composeTestRule.onNodeWithText("You haven't listed a campsite yet.").performScrollTo().assertIsDisplayed()
        // no campsites means nothing to run a lottery for
        composeTestRule.onNodeWithText("Create a lottery").assertDoesNotExist()
    }

    @Test
    fun profile_withCampsites_showsCreateLottery() {
        var createClicked = false
        composeTestRule.setContent {
            ProfileScreen(
                userId = "testUserId",
                myCampsites = listOf(
                    Campsite(id = "c1", name = "Lakeview", location = "Jasper", ownerId = "testUserId"),
                    Campsite(id = "c2", name = "Pine Ridge", location = "Banff", ownerId = "testUserId")
                ),
                onCreateLotteryClick = { createClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Create a lottery").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        assertTrue(createClicked)
    }

    @Test
    fun profile_listsMyLotteries_underBookings_andOpensFirstCampsite() {
        var opened: String? = null
        val sites = listOf(
            Campsite(id = "c1", name = "Lakeview", ownerId = "testUserId"),
            Campsite(id = "c2", name = "Pine Ridge", ownerId = "testUserId")
        )
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                ProfileHostingSection(
                    myCampsites = sites,
                    onCampsiteClick = { opened = it },
                    onOwnerBookingsClick = {},
                    onAddCampsiteClick = {},
                    myLotteries = listOf(Lottery(id = "l1", campsiteIds = listOf("c1", "c2")))
                )
            }
        }

        composeTestRule.onNodeWithText("My lotteries", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Lakeview, Pine Ridge").performClick()
        assertEquals("c1", opened)
    }

    @Test
    fun profile_noLotteries_showsEmptyState() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                ProfileHostingSection(
                    myCampsites = emptyList(),
                    onCampsiteClick = {},
                    onOwnerBookingsClick = {},
                    onAddCampsiteClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("You haven't created a lottery yet.").assertIsDisplayed()
    }

    // ---- Admin route guard ----

    @Test
    fun adminGate_nonAdmin_redirects_andHidesContent() {
        var redirected = false
        composeTestRule.setContent {
            AdminGate(isAdmin = false, onDenied = { redirected = true }) {
                Text("Admin tools")
            }
        }

        composeTestRule.waitForIdle()
        assertTrue(redirected)
        composeTestRule.onNodeWithText("Admin tools").assertDoesNotExist()
    }

    @Test
    fun adminGate_roleLoading_rendersNoAdminContent() {
        var redirected = false
        composeTestRule.setContent {
            AdminGate(isAdmin = null, onDenied = { redirected = true }) {
                Text("Admin tools")
            }
        }

        composeTestRule.onNodeWithTag("adminGateLoading").assertExists()
        composeTestRule.onNodeWithText("Admin tools").assertDoesNotExist()
        assertFalse(redirected)
    }

    @Test
    fun adminGate_admin_rendersContent() {
        var redirected = false
        composeTestRule.setContent {
            AdminGate(isAdmin = true, onDenied = { redirected = true }) {
                Text("Admin tools")
            }
        }

        composeTestRule.onNodeWithText("Admin tools").assertIsDisplayed()
        assertFalse(redirected)
    }
}
