package com.example.toastytraveltools

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost


@Composable
fun ToastyTravelToolsApp(reservationRepository: ReservationRepository) {
    val navController = rememberNavController()
    val campsiteRepository = remember { CampsiteRepository() }
    val lotteryRepository = remember { LotteryRepository() }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToUser = { navController.navigate("search") },
                onNavigateToOwner = { navController.navigate("owner_home") }
            )
        }

        composable("owner_home") {
            OwnerHomeScreen(
                onNavigateToAddCampsite = { navController.navigate("owner_add") },
                onNavigateToCreateLottery = { navController.navigate("owner_create_lottery") },
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

        composable("owner_create_lottery") {
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("search") { 
            SearchScreen(
                onCampsiteClick = { id -> navController.navigate("details/$id") }, 
                onSearchClick = { navController.navigate("search") }, 
                onReservationClick = { navController.navigate("reservation") }, 
                onProfileClick = { navController.navigate("profile") }
            ) 
        }
        
        composable("owner_create_lottery") {
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // details screen, pass in campsite id
        composable("details/{campsiteId}") { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            CampsiteDetailsScreen(
                campsiteId = campsiteId, 
                onBack = { navController.popBackStack() }, 
                onError = {}
            )
        }
        
        composable("owner_create_lottery") {
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("confirmation/{reservationId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("reservationId")
            val reservation = reservationRepository.reservations.find { it.id == id }

            if (reservation != null) {
                ConfirmationScreen(reservation = reservation)
            } else {
                Text("Loading...")
            }
        }

        composable("owner_create_lottery") {
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
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

        composable("reservation") {
            ReservationScreen( 
                onSearchClick = { navController.navigate("search") }, 
                onReservationClick = { navController.navigate("reservation") }, 
                onProfileClick = { navController.navigate("profile") }
            )
        }
        
        composable("owner_create_lottery") {
            OwnerScreenCreateLottery(
                lotteryRepository = lotteryRepository,
                campsiteRepository = campsiteRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

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
