/*
ToastyTravelToolsApp.kt

Purpose: This is the main file for the Toasty Travel Tools app. It contains the navigation.
There are no modes: everyone starts on Explore ("search") with one bottom bar
(Explore / Reservations / Profile). Owner actions appear on campsites you own, and admin
tools are reachable from Profile only for profiles whose role is "admin".

Issues:
 - no outstanding issues currently
 */


package com.example.toastytraveltools.view

import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.toastytraveltools.view.components.AdminGate
import com.example.toastytraveltools.view.components.rememberIsAdmin
import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.controller.LotteryRepository
import com.example.toastytraveltools.controller.ReservationRepository
import com.example.toastytraveltools.model.Reservation

@Composable
fun ToastyTravelToolsApp(notificationsAllowed: Boolean = true) {

    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    // draw any lottery whose registration period has ended, every time the app comes
    // to the foreground (reopening from recents doesn't recreate the activity)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) drawDueLotteries()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // notify this user if they won a lottery
    // wait until notifications are allowed, otherwise the win would be marked as
    // notified while the permission dialog is still showing and the notification dropped
    DisposableEffect(deviceId, notificationsAllowed) {
        if (!notificationsAllowed) return@DisposableEffect onDispose { }
        val listener = listenForLotteryWins(context, deviceId)
        val lossListener = listenForLotteryLosses(context.applicationContext, deviceId)
        onDispose {
            listener.remove()
            lossListener.remove()
        }
    }

    val navController = rememberNavController()

    var claimedReservation by remember { mutableStateOf<Reservation?>(null) }
    var cancelledReservation by remember { mutableStateOf<Reservation?>(null) }

    val campsiteRepository = remember { CampsiteRepository() }
    val lotteryRepository = remember { LotteryRepository() }
    val reservationRepository = remember { ReservationRepository() }

    // role read from entrants/{deviceId}; null while loading. Gates the Admin entry and routes.
    val isAdmin = rememberIsAdmin(deviceId)

    // first launch on a new device: make the entrant create a profile before anything else
    FirstRunProfileDialog(deviceId = deviceId)

    // top-level tabs and "back to Explore" never stack copies of the same screen
    fun goToTab(route: String) {
        navController.navigate(route) {
            popUpTo("search") { inclusive = false }
            launchSingleTop = true
        }
    }
    fun goToExplore() = goToTab("search")
    val goToProfile: (String) -> Unit = { id -> goToTab("profile/$id") }

    // admin routes: render only for admins, send everyone else back to Explore
    @Composable
    fun adminOnly(content: @Composable () -> Unit) =
        AdminGate(isAdmin = isAdmin, onDenied = { goToExplore() }, content = content)

    val optionalCampsiteArg = listOf(
        navArgument("campsiteId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )

    NavHost(
        navController = navController,
        startDestination = "search"
    ) {

        // Explore: the start screen for everyone
        composable("search") {
            SearchScreen(
                onCampsiteClick = { id -> navController.navigate("details/$id") },
                onAddCampsiteClick = { navController.navigate("owner_add") },
                onSearchClick = { goToExplore() },
                onReservationClick = { goToTab("reservation") },
                onProfileClick = goToProfile
            )
        }

        // Campsite details: guest view, or owner actions if this device owns the campsite
        composable("details/{campsiteId}") { backStackEntry ->
            val campsiteId =
                backStackEntry.arguments?.getString("campsiteId") ?: ""

            CampsiteDetailsScreen(
                campsiteId = campsiteId,
                onBack = { navController.popBackStack() },
                onError = {},
                campsiteRepository = campsiteRepository,
                onEditCampsite = { id -> navController.navigate("owner_edit_campsite?campsiteId=$id") },
                onCreateLottery = { id -> navController.navigate("owner_create_lottery?campsiteId=$id") },
                onViewBookings = { id -> navController.navigate("owner_booking?campsiteId=$id") },
                onCampsiteDeleted = { goToExplore() }
            )
        }

        // Owner forms. Anyone can list a campsite; ownerId is this device's id.
        composable("owner_add") {
            OwnerScreenAddCampsite(
                repository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("owner_edit_campsite?campsiteId={campsiteId}", arguments = optionalCampsiteArg) { backStackEntry ->
            OwnerScreenEditCampsite(
                repository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() },
                initialCampsiteId = backStackEntry.arguments?.getString("campsiteId")
            )
        }

        composable("owner_create_lottery?campsiteId={campsiteId}", arguments = optionalCampsiteArg) { backStackEntry ->
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() },
                initialCampsiteId = backStackEntry.arguments?.getString("campsiteId")
            )
        }

        // Bookings for my campsites (all of them, or one when campsiteId is given)
        composable("owner_booking?campsiteId={campsiteId}", arguments = optionalCampsiteArg) { backStackEntry ->
            OwnerScreenBooking(
                onNavigateBack = { navController.popBackStack() },
                userId = deviceId,
                onMessageClick = { id -> navController.navigate("chat/$id") },
                campsiteIdFilter = backStackEntry.arguments?.getString("campsiteId"),
                campsiteRepository = campsiteRepository
            )
        }

        // Admin screens, reachable from Profile for admins only
        composable("admin") {
            adminOnly { AdminDashboardScreen(navController) }
        }

        composable("admin_campsites") {
            adminOnly {
                AdminCampsitesScreen(
                    repository = campsiteRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("admin_profiles") {
            adminOnly {
                AdminProfilesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("admin_images") {
            adminOnly {
                AdminImagesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("admin_reviews") {
            adminOnly {
                AdminReviewsScreen(
                    reservationRepository = reservationRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Reservation confirmation
        composable("confirmation/{reservationId}") { backStackEntry ->

            val id =
                backStackEntry.arguments?.getString("reservationId")

            val reservation =
                reservationRepository.reservations.find { it.id == id }

            if (reservation != null) {
                ConfirmationScreen(reservation = reservation,
                    onHome = { goToExplore() })
            } else {
                Text("Loading...")
            }
        }

        // Reservations tab
        composable("reservation") {
            ReservationScreen(
                onNavigateBack = { navController.popBackStack() },
                onSearchClick = { goToExplore() },
                onReservationClick = { goToTab("reservation") },
                onProfileClick = goToProfile,
                onClaimed = { reservation ->
                    claimedReservation = reservation
                    navController.navigate("claimed_confirmation")
                },
                onCancelled = { reservation ->
                    cancelledReservation = reservation
                    navController.navigate("cancelled_confirmation")
                },
                onMessageClick = { id -> navController.navigate("chat/$id") }
            )
        }

        // Profile tab, with My campsites, hosting entries and (for admins) Admin tools
        composable("profile/{deviceId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("deviceId") ?: ""
            ProfileScreen(
                userId = id,
                onNavigateBack = { navController.popBackStack() },
                onSearchClick = { goToExplore() },
                onReservationClick = { goToTab("reservation") },
                onProfileClick = goToProfile,
                isAdmin = isAdmin == true && id == deviceId,
                myCampsites = ownedCampsites(campsiteRepository.campsites, id),
                onCampsiteClick = { campsiteId -> navController.navigate("details/$campsiteId") },
                onOwnerBookingsClick = { navController.navigate("owner_booking") },
                onAddCampsiteClick = { navController.navigate("owner_add") },
                onAdminClick = { navController.navigate("admin") }
            )
        }

        // shown after the entrant cancels a reservation
        composable("cancelled_confirmation") {
            val reservation = cancelledReservation
            if (reservation != null) {
                CancellationScreen(reservation = reservation,
                    onHome = { goToExplore() })
            } else {
                Text("Loading...")
            }
        }

        composable("claimed_confirmation") {
            val reservation = claimedReservation
            if (reservation != null) {
                ConfirmationScreen(reservation = reservation,
                    onHome = { goToExplore() })
            } else {
                Text("Loading...")
            }
        }
        // Chat between entrant and owner about one reservation
        composable("chat/{reservationId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("reservationId") ?: ""
            ChatScreen(
                reservationId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
