package com.example.toastytraveltools.view

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    onNavigateToUser: () -> Unit,
    onNavigateToOwner: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    var showRegistration by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<UserProfile?>(null)}
    var profile by remember {
        mutableStateOf<UserProfile?>(null)
    }

    LaunchedEffect(deviceId) {
        val ref = FirebaseFirestore.getInstance()
            .collection("entrants")
            .document(deviceId)

        ref.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                currentUser = doc.toObject(UserProfile::class.java)
            } else {
                showRegistration = true
            }
        }
    }

    if (showRegistration) {

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        var nameError by remember { mutableStateOf<String?>(null) }
        var emailError by remember { mutableStateOf<String?>(null) }
        var phoneError by remember { mutableStateOf<String?>(null) }

        var saving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                // Keep empty so a new entrant can't dismiss registration
            },

            title = {
                Text("Create Profile")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text("Enter your information to continue.")

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = {
                            nameError?.let {
                                Text(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        isError = emailError != null,
                        supportingText = {
                            emailError?.let {
                                Text(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                        },
                        label = { Text("Phone number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        isError = phoneError != null,
                        supportingText = {
                            phoneError?.let {
                                Text(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = {

                        nameError = validateName(name)
                        emailError = validateEmail(email)
                        phoneError = validatePhone(phone)

                        if (
                            nameError == null &&
                            emailError == null &&
                            phoneError == null
                        ) {

                            val newUser = UserProfile(
                                deviceId = deviceId,
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                role = "entrant"
                            )

                            saving = true

                            FirebaseFirestore.getInstance()
                                .collection("entrants")
                                .document(deviceId)
                                .set(newUser)
                                .addOnSuccessListener {
                                    saving = false
                                    profile = newUser
                                    showRegistration = false
                                }
                                .addOnFailureListener {
                                    saving = false
                                }
                        }
                    },
                    enabled = !saving
                ) {
                    Text(
                        if (saving) "Saving..." else "Create Profile"
                    )
                }
            }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Toasty Travel Tools",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Button(
                onClick = onNavigateToUser,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("User Mode")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToOwner,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Owner Mode")
            }
        }
    }
}