
package com.example.toastytraveltools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.toastytraveltools.model.AdminProfile


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfilesScreen(onBack: () -> Unit) {

    var profiles by remember {
        mutableStateOf<List<AdminProfile>>(emptyList())
    }

    var selectedProfile by remember {
        mutableStateOf<AdminProfile?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // Listen for real Firebase profile changes
    DisposableEffect(Unit) {

        val listener = FirebaseFirestore.getInstance()
            .collection("entrants")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    errorMessage = error.localizedMessage
                        ?: "Failed to load profiles."
                    isLoading = false
                    return@addSnapshotListener
                }

                profiles = snapshot?.documents?.map { document ->

                    AdminProfile(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        email = document.getString("email") ?: "",
                        phone = document.getString("phone") ?: "",
                        role = document.getString("role") ?: "Entrant"
                    )

                } ?: emptyList()

                errorMessage = null
                isLoading = false
            }

        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uploaded Images") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            if (selectedProfile == null) {

                Text(
                    text = "User Profiles",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("All profiles (${profiles.size})")

                Spacer(modifier = Modifier.height(12.dp))

                when {

                    isLoading -> {
                        CircularProgressIndicator()
                        Text("Loading profiles...")
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    profiles.isEmpty() -> {
                        Text("No profiles available.")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            items(
                                items = profiles,
                                key = { it.id }
                            ) { profile ->

                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {

                                        Text(
                                            text = profile.name.ifBlank {
                                                "Unnamed profile"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(profile.email)
                                        Text("Role: ${profile.role}")

                                        Spacer(
                                            modifier = Modifier.height(8.dp)
                                        )

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
                }

            } else {

                val profile = selectedProfile!!

                Text(
                    text = "Profile Details",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Name: ${profile.name}")
                Text("Email: ${profile.email}")
                Text("Phone: ${profile.phone}")
                Text("Role: ${profile.role}")
                Text("Profile ID: ${profile.id}")

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
}