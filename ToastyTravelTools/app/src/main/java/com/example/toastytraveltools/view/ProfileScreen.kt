/*
ProfileScreen.kt

Purpose: One of the screens the user can access, and it allows you to view your information, turn
off/on notifications, view app policy, edit information, and delete profile.

Issues:
 - delete profile button should come with some type of confirmation because you cannot undo it and
 it is really easy to accidently delete your profile
 */

package com.example.toastytraveltools.view

import com.example.toastytraveltools.view.components.ConfirmDestructiveDialog
import com.example.toastytraveltools.view.components.MainTab
import com.example.toastytraveltools.view.components.ToastyMainBottomBar
import com.example.toastytraveltools.view.components.ToastyNavRow
import com.example.toastytraveltools.view.components.ToastySectionLabel
import com.example.toastytraveltools.model.Campsite
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
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
 * NEED TO ADD COMMENTS
 */
fun uploadProfileImage(
    userId: String,
    imageUri: Uri,
    onDone: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val imageRef = FirebaseStorage.getInstance()
        .reference
        .child("profile_images/$userId.jpg")

    imageRef.putFile(imageUri)
        .continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
            imageRef.downloadUrl
        }
        .addOnSuccessListener { downloadUri ->
            val url = downloadUri.toString()
            FirebaseFirestore.getInstance()
                .collection("entrants")
                .document(userId)
                .set(mapOf("profileImageUrl" to url), SetOptions.merge())
                .addOnSuccessListener { onDone(url) }
                .addOnFailureListener { onError(it) }
        }
        .addOnFailureListener { onError(it) }
}

/**
 * NEED TO ADD COMMENTS
 */
fun removeProfileImage(
    userId: String,
    onDone: () -> Unit,
    onError: (Exception) -> Unit
) {
    FirebaseStorage.getInstance()
        .reference
        .child("profile_images/$userId.jpg")
        .delete()

    FirebaseFirestore.getInstance()
        .collection("entrants")
        .document(userId)
        .set(mapOf("profileImageUrl" to ""), SetOptions.merge())
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
    FirebaseStorage.getInstance()
        .reference
        .child("profile_images/$userId.jpg")
        .delete()

    FirebaseFirestore.getInstance()
        .collection("entrants")
        .document(userId)
        .delete()
        .addOnSuccessListener {
            onDeleted()
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
 * @param isAdmin Whether this device's role is "admin". Only then is the Admin entry shown.
 * @param myCampsites Campsites this device owns, listed under "My campsites".
 * @param onCampsiteClick Opens a campsite's Details page, where the owner actions are.
 * @param onOwnerBookingsClick Opens bookings (and guest messages) for the owner's campsites.
 * @param onAddCampsiteClick Opens the add-campsite form.
 * @param onAdminClick Opens the admin dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onSearchClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onReservationClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    isAdmin: Boolean = false,
    myCampsites: List<Campsite> = emptyList(),
    onCampsiteClick: (String) -> Unit = {},
    onOwnerBookingsClick: () -> Unit = {},
    onAddCampsiteClick: () -> Unit = {},
    onAdminClick: () -> Unit = {}
) {
    var confirmDeleteProfile by remember { mutableStateOf(false) }
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

    var uploadingPhoto by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uploadingPhoto = true
            message = null
            uploadProfileImage(
                userId = userId,
                imageUri = uri,
                onDone = { url ->
                    uploadingPhoto = false
                    profile = profile?.copy(profileImageUrl = url)
                    message = "Profile picture updated"
                },
                onError = {
                    uploadingPhoto = false
                    message = "Could not upload picture: ${it.message}"
                }
            )
        }
    }

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
                title = { Text("Profile") }
            )
        },
        bottomBar = {
            ToastyMainBottomBar(
                selected = MainTab.Profile,
                onExploreClick = onSearchClick,
                onReservationsClick = onReservationClick,
                onProfileClick = { onProfileClick(userId) }
            )
        }
    ){ innerpadding ->
        Column(
            modifier = Modifier
                .padding(innerpadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (current.profileImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = current.profileImageUrl,
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(96.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(current.name.take(1).uppercase())
                                }
                            }
                            if (uploadingPhoto) {
                                CircularProgressIndicator()
                            }
                        }

                        TextButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !uploadingPhoto
                        ) {
                            Text(if (uploadingPhoto) "Uploading..." else "Change Photo")
                        }


                        if (current.profileImageUrl.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    uploadingPhoto = true
                                    removeProfileImage(
                                        userId = userId,
                                        onDone = {
                                            uploadingPhoto = false
                                            profile = profile?.copy(profileImageUrl = "")
                                            message = "Profile picture removed"
                                        },
                                        onError = {
                                            uploadingPhoto = false
                                            message = "Could not remove picture: ${it.message}"
                                        }
                                    )
                                },
                                enabled = !uploadingPhoto
                            ) {
                                Text("Remove Photo")
                            }
                        }
                    }
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
                                profile = current.copy(notificationsEnabled = enabled)
                                setNotificationsEnabled(
                                    userId = userId,
                                    enabled = enabled,
                                    onDone = { togglingNotifications = false },
                                    onError = {
                                        togglingNotifications = false
                                        profile = current.copy(notificationsEnabled = !enabled)
                                        message = "Could not update notifications: ${it.message}"
                                    }
                                )
                            }
                        )
                    }

                    Button(
                        shape = MaterialTheme.shapes.small,
                        onClick = {
                            isViewingPolicy = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View App Policy")
                    }

                    Button(
                        shape = MaterialTheme.shapes.small,
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

                    OutlinedButton(
                        shape = MaterialTheme.shapes.small,
                        onClick = { confirmDeleteProfile = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
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
                            shape = MaterialTheme.shapes.small,
                            onClick = { isEditing = false },
                            enabled = !saving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            shape = MaterialTheme.shapes.small,
                            onClick = {
                                nameError = validateName(name)
                                emailError = validateEmail(email)
                                phoneError = validatePhone(phone)

                                if (nameError == null && emailError == null && phoneError == null) {
                                    val updated = current.copy(
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

            // Hosting: anyone who lists a campsite owns it and manages it from here
            ProfileHostingSection(
                myCampsites = myCampsites,
                onCampsiteClick = onCampsiteClick,
                onOwnerBookingsClick = onOwnerBookingsClick,
                onAddCampsiteClick = onAddCampsiteClick
            )

            // Admin tools exist only for admin profiles; nobody else sees the entry
            if (isAdmin) {
                ToastySectionLabel("Admin")
                ToastyNavRow(
                    title = "Admin tools",
                    subtitle = "Manage campsites, profiles, images and reviews",
                    onClick = onAdminClick
                )
            }
        }
    }

    if (confirmDeleteProfile) {
        ConfirmDestructiveDialog(
            title = "Delete profile?",
            message = "Your profile and photo will be removed. This cannot be undone.",
            confirmLabel = "Delete profile",
            onConfirm = {
                confirmDeleteProfile = false
                deleteProfile(
                    userId = userId,
                    onDeleted = {
                        message = "Profile deleted"
                    }
                )
            },
            onDismiss = { confirmDeleteProfile = false }
        )
    }
    if(isViewingPolicy == true){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .clickable(enabled = true) {isViewingPolicy = false},
            contentAlignment = Alignment.Center
        ){
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight(0.7f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ){
                Text(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    text = "App Policy\n\n\nBe Respectful\n\nBe Kind\n\nTake Care of campsites\n\n\nWithin the app, you can join lotteries for a chance to be able to reserve a spot in a campsite. We aim to be fair to all contestants.\n Click anywhere to close this message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

    }

}

/**
 * "My campsites" plus the owner entries on Profile. Tapping a campsite opens its Details
 * page, where Edit / Delete / Create lottery / View bookings live.
 */
@Composable
fun ProfileHostingSection(
    myCampsites: List<Campsite>,
    onCampsiteClick: (String) -> Unit,
    onOwnerBookingsClick: () -> Unit,
    onAddCampsiteClick: () -> Unit
) {
    ToastySectionLabel("My campsites")
    if (myCampsites.isEmpty()) {
        Text(
            text = "You haven't listed a campsite yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        myCampsites.forEach { site ->
            ToastyNavRow(
                title = site.name,
                subtitle = site.location.ifBlank { null },
                onClick = { onCampsiteClick(site.id) }
            )
        }
    }
    OutlinedButton(
        onClick = onAddCampsiteClick,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("List a campsite")
    }
    ToastyNavRow(
        title = "Bookings for my campsites",
        subtitle = "See who's booked, cancel, and message your guests",
        onClick = onOwnerBookingsClick
    )
}
