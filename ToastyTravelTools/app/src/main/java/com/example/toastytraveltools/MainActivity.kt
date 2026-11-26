package com.example.toastytraveltools

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.toastytraveltools.view.LotteryNotifier
import com.example.toastytraveltools.view.theme.ToastyTravelToolsTheme
import com.example.toastytraveltools.view.ToastyTravelToolsApp

class MainActivity : ComponentActivity() {

    // whether we're allowed to post notifications, the lottery listeners wait for this
    private var notificationsAllowed by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsAllowed = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        notificationsAllowed = LotteryNotifier.canPost(this)
        if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            ToastyTravelToolsTheme {
                ToastyTravelToolsApp(notificationsAllowed = notificationsAllowed)
            }
        }
    }
}
