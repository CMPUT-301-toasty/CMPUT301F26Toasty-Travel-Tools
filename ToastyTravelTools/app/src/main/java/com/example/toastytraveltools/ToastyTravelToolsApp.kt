
package com.example.toastytraveltools

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ToastyTravelToolsApp(reservationRepository: ReservationRepository) {
    val navController = rememberNavController()
    val campsiteRepository = remember { CampsiteRepository() }
    val lotteryRepository = remember { LotteryRepository() }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        // Main home screen
        composable("home") {
            HomeScreen(
                onNavigateToUser = { navController.navigate("search") },
                onNavigateToOwner = { navController.navigate("owner_home") }
            )
        }

        // Owner screens
        composable("owner_home") {
            OwnerHomeScreen(
                onNavigateToAddCampsite = { navController.navigate("owner_add") },
                onNavigateToCreateLottery = {
                    navController.navigate("owner_create_lottery")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("owner_add") {
            OwnerScreenAddCampsite(
                repository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("owner_create_lottery") {
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Admin screens
        composable("admin") {
            AdminDashboardScreen(navController)
        }

        composable("admin_campsites") {
            AdminCampsitesScreen(
                onBack = { navController.navigate("admin") }
            )
        }

        composable("admin_profiles") {
            AdminProfilesScreen(
                onBack = { navController.navigate("admin") }
            )
        }

        composable("admin_images") {
            AdminImagesScreen(
                onBack = { navController.navigate("admin") }
            )
        }

        // Search screen
        composable("search") {
            SearchScreen(
                onCampsiteClick = { id ->
                    navController.navigate("details/$id")
                },
                onSearchClick = { navController.navigate("search") },
                onReservationClick = { navController.navigate("reservation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }

        // Campsite details
        composable("details/{campsiteId}") { backStackEntry ->
            val campsiteId =
                backStackEntry.arguments?.getString("campsiteId") ?: ""

            CampsiteDetailsScreen(
                campsiteId = campsiteId,
                onBack = { navController.popBackStack() },
                onError = {}
            )
        }

        // Reservation confirmation
        composable("confirmation/{reservationId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("reservationId")
            val reservation =
                reservationRepository.reservations.find { it.id == id }

            if (reservation != null) {
                ConfirmationScreen(reservation = reservation)
            } else {
                Text("Loading...")
            }
        }

        // Reservation screen
        composable("reservation") {
            ReservationScreen(
                onSearchClick = { navController.navigate("search") },
                onReservationClick = { navController.navigate("reservation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }

        // Profile screen
        composable("profile") {
            ProfileScreen(
                userId = "test",
                onSearchClick = { navController.navigate("search") },
                onReservationClick = { navController.navigate("reservation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
    }
}
