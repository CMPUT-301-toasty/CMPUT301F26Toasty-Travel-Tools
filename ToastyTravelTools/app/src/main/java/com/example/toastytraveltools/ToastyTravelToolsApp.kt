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
    NavHost(navController = navController, startDestination = "search") {
        composable("search") { SearchScreen(onCampsiteClick = { id -> navController.navigate("details/$id")}) }

        // details screen, pass in campsite id
        composable("details/{campsiteId}") { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            CampsiteDetailsScreen(campsiteId = campsiteId, onError = {})
        }
    }
}