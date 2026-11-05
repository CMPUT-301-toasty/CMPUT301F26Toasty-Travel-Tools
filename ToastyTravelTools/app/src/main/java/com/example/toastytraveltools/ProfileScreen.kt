package com.example.toastytraveltools

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.google.firebase.firestore.FirebaseFirestore

fun validateName(name: String): String? =
    if (name.isBlank()) "Name cannot be empty" else null

fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Email cannot be empty"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
    else -> null
}

fun validatePhone(phone: String): String? = when {
    phone.isBlank() -> "Phone number cannot be empty"
    !Patterns.PHONE.matcher(phone.trim()).matches() -> "Enter a valid phone number"
    phone.count() !in 10..15 -> "Enter a valid phone number"
    else -> null
}

fun loadProfile(
    userId: String,
    onResult: (UserProfile) -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("entrants")
        .document(userId)
        .get()
        .addOnSuccessListener { doc ->
            onResult(doc.toObject(UserProfile::class.java) ?: UserProfile())
        }
        .addOnFailureListener { onError(it) }
}

fun saveProfile(
    userId: String,
    profile: UserProfile,
    onSaved: () -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("entrants")
        .document(userId)
        .set(profile)
        .addOnSuccessListener { onSaved() }
        .addOnFailureListener { onError(it) }
}

fun deleteProfile(
    userId: String,
    onDeleted: () -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("entrants")
        .document(userId)
        .delete()
        .addOnSuccessListener {
            onDeleted()
        }
}

@Composable
fun ProfileScreen(userId: String) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var isEditing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // user provided
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        loadProfile(
            userId = userId,
            onResult = { profile = it },
            onError = { loadError = "Could not load profile: ${it.message}" }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        val current = profile

        when {
            loadError != null -> Text(loadError!!, color = MaterialTheme.colorScheme.error)

            current == null -> CircularProgressIndicator()

            !isEditing -> {
                Text("Name", style = MaterialTheme.typography.labelMedium)
                Text(current.name)

                Text("Email", style = MaterialTheme.typography.labelMedium)
                Text(current.email)

                Text("Phone", style = MaterialTheme.typography.labelMedium)
                Text(current.phone)

                Button(
                    onClick = {
                        name = current.name
                        email = current.email
                        phone = current.phone
                        nameError = null
                        emailError = null
                        phoneError = null
                        message = null
                        isEditing = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit")
                }

                Spacer(Modifier.padding(8.dp))

                Button(
                    onClick = {
                        deleteProfile(
                            userId = userId,
                            onDeleted = {
                                message = "Profile deleted"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Profile")
                }
            }
            // on deleting a profile the details keep showing on the screen. Will change that

            else -> { //  editing view
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = null },
                    label = { Text("Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    supportingText = { phoneError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            nameError = validateName(name)
                            emailError = validateEmail(email)
                            phoneError = validatePhone(phone)

                            if (nameError == null && emailError == null && phoneError == null) {
                                val updated = UserProfile(
                                    name = name.trim(),
                                    email = email.trim(),
                                    phone = phone.trim()
                                )
                                saving = true
                                saveProfile(
                                    userId = userId,
                                    profile = updated,
                                    onSaved = {
                                        saving = false
                                        profile = updated
                                        isEditing = false
                                        message = "Profile saved"
                                    },
                                    onError = {
                                        saving = false
                                        message = "Could not save profile: ${it.message}"
                                    }
                                )
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (saving) "Saving..." else "Save")
                    }
                }
            }
        }

        message?.let { Text(it) }
    }
}