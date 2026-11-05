
package com.example.toastytraveltools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AdminProfile(
    val name: String,
    val email: String,
    val role: String
)

private val sampleProfiles = listOf(
    AdminProfile("Jordan Lee", "jordan.lee@email.com", "Entrant"),
    AdminProfile("Maya Patel", "maya.patel@email.com", "Owner"),
    AdminProfile("Lily Brooks", "lily.brooks@email.com", "Entrant"),
    AdminProfile("Evan Cole", "evan.cole@email.com", "Owner")
)

@Composable
fun AdminProfilesScreen(onBack: () -> Unit) {
    var selectedProfile by remember {
        mutableStateOf<AdminProfile?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back to Dashboard")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keep the rest of your existing screen code below
        if (selectedProfile == null) {
            Text(
                "User Profiles",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (sampleProfiles.isEmpty()) {
                Text("No profiles")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sampleProfiles) { profile ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(profile.name)
                                Text("Role: ${profile.role}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        selectedProfile = profile
                                    }
                                ) {
                                    Text("View Profile")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "Profile Details",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Name: ${selectedProfile!!.name}")
            Text("Email: ${selectedProfile!!.email}")
            Text("Role: ${selectedProfile!!.role}")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    selectedProfile = null
                }
            ) {
                Text("Back to profiles")
            }
        }
    }
}
