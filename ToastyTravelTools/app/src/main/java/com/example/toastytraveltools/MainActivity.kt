package com.example.toastytraveltools

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.example.toastytraveltools.view.theme.ToastyTravelToolsTheme
import com.example.toastytraveltools.view.ToastyTravelToolsApp

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            ToastyTravelToolsTheme {
                ToastyTravelToolsApp()
            }
        }
    }
}
