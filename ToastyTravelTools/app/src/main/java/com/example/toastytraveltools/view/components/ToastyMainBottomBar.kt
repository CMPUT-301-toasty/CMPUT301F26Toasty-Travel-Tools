package com.example.toastytraveltools.view.components

import androidx.compose.runtime.Composable

/** The three top-level tabs of the app. */
enum class MainTab { Explore, Reservations, Profile }

/**
 * The single bottom bar shared by every top-level screen: Explore, Reservations, Profile.
 * Owner and admin tools are reached from Profile or from a campsite's details, not from here.
 */
@Composable
fun ToastyMainBottomBar(
    selected: MainTab,
    onExploreClick: () -> Unit,
    onReservationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    ToastyBottomBar {
        ToastyNavItem(label = "Explore", selected = selected == MainTab.Explore, onClick = onExploreClick)
        ToastyNavItem(label = "Reservations", selected = selected == MainTab.Reservations, onClick = onReservationsClick)
        ToastyNavItem(label = "Profile", selected = selected == MainTab.Profile, onClick = onProfileClick)
    }
}
