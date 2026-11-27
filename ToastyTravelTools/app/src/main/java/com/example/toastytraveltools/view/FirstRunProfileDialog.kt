/*
FirstRunProfileDialog.kt

Purpose: Shown on first launch when this device has no entrants/{deviceId} profile yet.
The entrant has to create a profile before using the app. Moved out of the old mode-picker
home screen unchanged: same validation (validateName/Email/Phone) and same Firestore write.
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FirstRunProfileDialog(deviceId: String) {
    var showRegistration by remember { mutableStateOf(false) }

    LaunchedEffect(deviceId) {
        val ref = FirebaseFirestore.getInstance()
            .collection("entrants")
            .document(deviceId)

        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                showRegistration = true
            }
        }
    }

    if (!showRegistration) return

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
        title = { Text("Create Profile") },
        text = {
            // scrolls so the fields stay reachable on small screens with the keyboard open
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
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
                    supportingText = { nameError?.let { Text(it) } },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    supportingText = { phoneError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                shape = MaterialTheme.shapes.small,
                onClick = {
                    nameError = validateName(name)
                    emailError = validateEmail(email)
                    phoneError = validatePhone(phone)

                    if (nameError == null && emailError == null && phoneError == null) {
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
                                showRegistration = false
                            }
                            .addOnFailureListener {
                                saving = false
                            }
                    }
                },
                enabled = !saving
            ) {
                Text(if (saving) "Saving..." else "Create Profile")
            }
        }
    )
}
