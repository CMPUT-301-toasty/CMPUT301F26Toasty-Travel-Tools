
package com.example.toastytraveltools.view

import com.example.toastytraveltools.AdminDashboardScreen
import com.example.toastytraveltools.AdminCampsitesScreen
import com.example.toastytraveltools.AdminProfilesScreen
import com.example.toastytraveltools.AdminImagesScreen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.controller.LotteryRepository
import com.example.toastytraveltools.controller.ReservationRepository

@Composable
fun ToastyTravelToolsApp() {

    val navController = rememberNavController()

    val campsiteRepository = remember { CampsiteRepository() }
    val lotteryRepository = remember { LotteryRepository() }
    val reservationRepository = remember { ReservationRepository() }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        // Main home screen
        composable("home") {
            HomeScreen(
                onNavigateToUser = {
                    navController.navigate("search")
                },
                onNavigateToOwner = {
                    navController.navigate("owner_home")
                },
                onNavigateToAdmin = {
                    navController.navigate("admin")
                }
            )
        }

        // Owner screens
        composable("owner_home") {
            OwnerHomeScreen(
                onNavigateToAddCampsite = {
                    navController.navigate("owner_add")
                },
                onNavigateToCreateLottery = {
                    navController.navigate("owner_create_lottery")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("owner_add") {
            OwnerScreenAddCampsite(
                repository = campsiteRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Keep exactly one owner_create_lottery route
        composable("owner_create_lottery") {
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Admin screens
        composable("admin") {
            AdminDashboardScreen(navController)
        }

        composable("admin_campsites") {
            AdminCampsitesScreen(
                repository = campsiteRepository,
                onBack = {
                    navController.navigate("admin")
                }
            )
        }

        composable("admin_profiles") {
            AdminProfilesScreen(
                onBack = {
                    navController.navigate("admin")
                }
            )
        }

        composable("admin_images") {
            AdminImagesScreen(
                onBack = {
                    navController.navigate("admin")
                }
            )
        }

        // Search screen
        composable("search") {
            SearchScreen(
                onNavigateBack = { navController.popBackStack("home", inclusive = false) },
                onCampsiteClick = { id -> navController.navigate("details/$id") },
                onSearchClick = { navController.navigate("search") },
                onReservationClick = { navController.navigate("reservation") },
                onProfileClick = { id -> navController.navigate("profile/$id") }
            ) 
        }

        // Campsite details
        composable("details/{campsiteId}") { backStackEntry ->

            val campsiteId =
                backStackEntry.arguments?.getString("campsiteId") ?: ""

            CampsiteDetailsScreen(
                campsiteId = campsiteId,
                onBack = {
                    navController.popBackStack()
                },
                onError = {}
            )
        }

        // Reservation confirmation
        composable("confirmation/{reservationId}") { backStackEntry ->

            val id =
                backStackEntry.arguments?.getString("reservationId")

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
                onNavigateBack = { navController.popBackStack("home", inclusive = false) },
                onSearchClick = { navController.navigate("search") },
                onReservationClick = { navController.navigate("reservation") },
                onProfileClick = { id -> navController.navigate("profile/$id") }
            )
        }

        composable("profile/{deviceId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("deviceId") ?: ""
            ProfileScreen(
                userId = id,
                onNavigateBack = { navController.popBackStack("home", inclusive = false) },
                onSearchClick = { navController.navigate("search") },
                onReservationClick = { navController.navigate("reservation") },
                onProfileClick = { profileId -> navController.navigate("profile/$profileId") }
            )
        }
    }
}
