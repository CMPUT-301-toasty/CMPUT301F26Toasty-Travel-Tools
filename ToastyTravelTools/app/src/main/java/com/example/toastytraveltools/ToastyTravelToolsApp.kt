package com.example.toastytraveltools

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ToastyTravelToolsApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "admin"
    ) {
        composable("search") {
            SearchScreen()
        }

        composable("admin") {
            AdminDashboardScreen(navController)
        }

        composable("admin_campsites") {
            AdminCampsitesScreen(
                onBack = {
                    navController.navigate("admin") {
                        popUpTo("admin") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("admin_profiles") {
            AdminProfilesScreen(
                onBack = {
                    navController.navigate("admin") {
                        popUpTo("admin") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("admin_images") {
            AdminImagesScreen(
                onBack = {
                    navController.navigate("admin") {
                        popUpTo("admin") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}