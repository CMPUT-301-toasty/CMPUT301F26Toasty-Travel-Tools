/*
ProfileScreen.kt

Purpose: One of the screens the user can access, and it allows you to view your information, turn
off/on notifications, view app policy, edit information, and delete profile.

Issues:
 - delete profile button should come with some type of confirmation because you cannot undo it and
 it is really easy to accidently delete your profile
 */

package com.example.toastytraveltools.view

import android.hardware.usb.UsbDevice.getDeviceId
import android.provider.Settings
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
/**
 * ProfileScreen.kt
 *
 * Purpose:
 * Displays and manages entrant profile information and preferences.
 *
 * Design:
 * Uses Jetpack Compose for the interface and Firebase Firestore
 * for loading and saving profile information.
 *
 * Outstanding issues:
 * Profile deletion currently does not automatically navigate away
 * from the screen.
 */

/**
 * Checks whether a profile name is valid.
 *
 * @param name Name entered by the user.
 * @return An error message if the name is blank, otherwise null.
 */
fun validateName(name: String): String? =
    if (name.isBlank()) "Name cannot be empty" else null
/**
 * Validates an email address using Android's email pattern.
 *
 * @param email Email address entered by the user.
 * @return An error message if invalid, otherwise null.
 */
fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Email cannot be empty"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
    else -> null
}
/**
 * Validates a phone number using Android's phone pattern and length.
 *
 * @param phone Phone number entered by the user.
 * @return An error message if invalid, otherwise null.
 */
fun validatePhone(phone: String): String? = when {
    phone.isBlank() -> "Phone number cannot be empty"
    !Patterns.PHONE.matcher(phone.trim()).matches() -> "Enter a valid phone number"
    phone.count() !in 10..15 -> "Enter a valid phone number"
    else -> null
}
/**
 * Updates the notification preference for an entrant in Firestore.
 *
 * @param userId ID of the entrant.
 * @param enabled Whether notifications should be enabled.
 * @param onDone Callback invoked when the preference is saved.
 * @param onError Callback invoked if saving fails.
 */
fun setNotificationsEnabled(
    userId: String,
    enabled: Boolean,
    onDone: () -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("entrants")
        .document(userId)
        .set(mapOf("notificationsEnabled" to enabled), SetOptions.merge())
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { onError(it) }
}
/**
 * Loads an entrant's profile from Firestore.
 *
 * @param userId ID of the entrant whose profile is requested.
 * @param onResult Callback receiving the loaded profile.
 * @param onError Callback receiving an exception if loading fails.
 */
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
/**
 * Saves updated entrant profile information to Firestore.
 *
 * @param userId ID of the entrant.
 * @param profile Updated profile information.
 * @param onSaved Callback invoked after saving succeeds.
 * @param onError Callback invoked if saving fails.
 */
fun saveProfile(
    userId: String,
    profile: UserProfile,
    onSaved: () -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("entrants")
        .document(userId)
        .set(profile, SetOptions.merge())
        .addOnSuccessListener { onSaved() }
        .addOnFailureListener { onError(it) }
}
/**
 * Deletes an entrant's profile document from Firestore.
 *
 * @param userId ID of the entrant whose profile should be deleted.
 * @param onDeleted Callback invoked after deletion succeeds.
 */
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

/**
 * Displays the navigation buttons for the search, reservation,
 * and profile screens.
 *
 * @param onSearchClick Callback for opening the search screen.
 * @param onReservationClick Callback for opening reservations.
 * @param onProfileClick Callback for opening the profile using the device ID.
 */
@Composable
fun BottomButtons_Profile(
    onSearchClick: () -> Unit,
    onReservationClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // filled button = the screen you're on
        OutlinedButton(
            onClick = onSearchClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Search", maxLines = 1)
        }
        OutlinedButton(
            onClick = onReservationClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Reservation", maxLines = 1)
        }
        Button(
            onClick = { onProfileClick(deviceId) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RectangleShape
        ) {
            Text("Profile", maxLines = 1)
        }
    }
}
/**
 * Displays the entrant's profile and allows editing of personal
 * information and notification preferences.
 *
 * @param userId ID of the entrant whose profile is displayed.
 * @param onSearchClick Callback for navigating to search.
 * @param onNavigateBack Callback for navigating back.
 * @param onReservationClick Callback for navigating to reservations.
 * @param onProfileClick Callback for profile navigation using a device ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onSearchClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onReservationClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var campsiteDeletionNotice by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var isViewingPolicy by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var togglingNotifications by remember { mutableStateOf(false) }

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

        FirebaseFirestore.getInstance()
            .collection("entrants")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                campsiteDeletionNotice =
                    doc.getString("campsiteDeletionNotice")
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomButtons_Profile(
                onSearchClick = onSearchClick,
                onReservationClick = onReservationClick,
                onProfileClick = onProfileClick
            )
        }
    ){ innerpadding ->
        Column(
            modifier = Modifier
                .padding(innerpadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium)
            campsiteDeletionNotice?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Notifications about lottery results", modifier = Modifier.weight(1f))
                        Switch(
                            checked = current.notificationsEnabled,
                            enabled = !togglingNotifications,
                            onCheckedChange = { enabled ->
                                togglingNotifications = true
                                profile = UserProfile(
                                    name = current.name,
                                    email = current.email,
                                    phone = current.phone,
                                    notificationsEnabled = enabled
                                )
                                setNotificationsEnabled(
                                    userId = userId,
                                    enabled = enabled,
                                    onDone = { togglingNotifications = false },
                                    onError = {
                                        togglingNotifications = false
                                        profile = UserProfile(
                                            name = current.name,
                                            email = current.email,
                                            phone = current.phone,
                                            notificationsEnabled = !enabled
                                        )
                                        message = "Could not update notifications: ${it.message}"
                                    }
                                )
                            }
                        )
                    }

                    Button(
                        onClick = {
                            isViewingPolicy = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View App Policy")
                    }

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
                                        phone = phone.trim(),
                                        notificationsEnabled = current.notificationsEnabled
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
    if(isViewingPolicy == true){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = true) {isViewingPolicy = false},
            contentAlignment = Alignment.Center
        ){
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight(0.7f)
                    .background(Color.White.copy()),
                contentAlignment = Alignment.Center
            ){
                Text(
                    text = "App Policy\n\n\nBe Respectful\n\nBe Kind\n\nTake Care of campsites\n\n\nWithin the app, you can join lotteries for a chance to be able to reserve a spot in a campsite. We aim to be fair to all contestants.\n Click anywhere to close this message",
                    color = Color.Black
                )
            }
        }

    }

}