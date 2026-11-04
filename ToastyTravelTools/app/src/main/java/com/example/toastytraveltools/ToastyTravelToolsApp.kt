package com.example.toastytraveltools

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost


@Composable
fun ToastyTravelToolsApp(reservationRepository: ReservationRepository) {
    val navController = rememberNavController()

    // set the start screen to the search screen since we don't have other screens yet
    // this can always be changed
    NavHost(navController = navController, startDestination = "confirmation/{reservationId}") {
        composable("search") { SearchScreen() }
        composable("confirmation/{reservationId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("reservationId")
            val reservation = reservationRepository.reservations.find { it.id == id }

            if (reservation != null) {
                ConfirmationScreen(reservation = reservation)
            } else {
                Text("Loading...")
            }
        }
    }
}