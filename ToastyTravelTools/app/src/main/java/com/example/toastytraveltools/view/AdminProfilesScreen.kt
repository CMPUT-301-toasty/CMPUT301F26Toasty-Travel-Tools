package com.example.toastytraveltools.view

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Tasks

/**
 * Removes an entrant's profile and associated Firestore records.
 */
fun removeEntrantProfile(
    userId: String,
    onDeleted: () -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("reservations")
        .whereEqualTo("entrant_id", userId)
        .get()
        .addOnSuccessListener { firstSnapshot ->

            db.collection("reservations")
                .whereEqualTo("entrantId", userId)
                .get()
                .addOnSuccessListener { secondSnapshot ->

                    val reservationDocs =
                        (firstSnapshot.documents + secondSnapshot.documents)
                            .distinctBy { it.id }

                    db.collection("lotteries")
                        .get()
                        .addOnSuccessListener { lotterySnapshot ->

                            val operations = mutableListOf<(WriteBatch) -> Unit>()

                            // Delete the entrant's reservations.
                            reservationDocs.forEach { document ->
                                operations.add { batch ->
                                    batch.delete(document.reference)
                                }
                            }

                            // Remove the entrant from lottery records.
                            lotterySnapshot.documents.forEach { document ->
                                val updates = mutableMapOf<String, Any>()

                                listOf(
                                    "waitlist",
                                    "winners",
                                    "notifiedWinners",
                                    "notifiedLosers"
                                ).forEach { field ->
                                    val ids = document.get(field) as? List<*>

                                    if (ids?.contains(userId) == true) {
                                        updates[field] =
                                            FieldValue.arrayRemove(userId)
                                    }
                                }

                                // Remove embedded reservations.
                                val lotteryReservations =
                                    document.get("reservations") as? List<*>

                                if (lotteryReservations != null) {
                                    val remainingReservations =
                                        lotteryReservations.filterNot { item ->
                                            val reservation = item as? Map<*, *>

                                            reservation?.get("entrantId") == userId ||
                                                    reservation?.get("entrant_id") == userId
                                        }

                                    if (remainingReservations.size !=
                                        lotteryReservations.size
                                    ) {
                                        updates["reservations"] =
                                            remainingReservations
                                    }
                                }

                                if (updates.isNotEmpty()) {
                                    operations.add { batch ->
                                        batch.update(document.reference, updates)
                                    }
                                }
                            }

                            // Delete the profile document.
                            val profileRef =
                                db.collection("entrants").document(userId)

                            operations.add { batch ->
                                batch.delete(profileRef)
                            }

                            // Commit in batches of at most 450 writes.
                            val commitTasks = operations.chunked(450).map { chunk ->
                                val batch = db.batch()

                                chunk.forEach { operation ->
                                    operation(batch)
                                }

                                batch.commit()
                            }

                            Tasks.whenAll(*commitTasks.toTypedArray())
                                .addOnSuccessListener {
                                    // Profile image cleanup is best-effort.
                                    FirebaseStorage.getInstance()
                                        .reference
                                        .child("profile_images/$userId.jpg")
                                        .delete()
                                        .addOnCompleteListener {
                                            onDeleted()
                                        }
                                }
                                .addOnFailureListener { onError(it) }
                        }
                        .addOnFailureListener { onError(it) }
                }
                .addOnFailureListener { onError(it) }
        }
        .addOnFailureListener { onError(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfilesScreen(onBack: () -> Unit) {

    var profiles by remember {
        mutableStateOf<List<AdminProfile>>(emptyList())
    }

    var selectedProfile by remember {
        mutableStateOf<AdminProfile?>(null)
    }

    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    var deleteError by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // Listen for real Firebase profile changes.
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
                title = { Text("User Profiles") },
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
                .padding(20.dp)
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
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                                            shape = MaterialTheme.shapes.small,
                                            onClick = {
                                                selectedProfile = profile
                                                deleteError = null
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
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
                        shape = MaterialTheme.shapes.small,
                        onClick = {
                            selectedProfile = null
                            deleteError = null
                        },
                        enabled = !isDeleting
                    ) {
                        Text("Back to profiles")
                    }

                    // Only Entrant profiles can be removed here.
                    if (profile.role.equals("entrant", ignoreCase = true)) {

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            shape = MaterialTheme.shapes.small,
                            onClick = {
                                deleteError = null
                                showDeleteConfirmation = true
                            },
                            enabled = !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Profile")
                        }
                    }

                    deleteError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog.
    if (showDeleteConfirmation && selectedProfile != null) {

        val profile = selectedProfile!!

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteConfirmation = false
                }
            },
            title = {
                Text("Remove Profile?")
            },
            text = {
                Text(
                    "Are you sure you want to remove ${profile.name}? " +
                            "Their reservations and lottery entries will also be removed. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        isDeleting = true

                        removeEntrantProfile(
                            userId = profile.id,
                            onDeleted = {
                                isDeleting = false
                                showDeleteConfirmation = false
                                selectedProfile = null
                                deleteError = null
                            },
                            onError = { error ->
                                isDeleting = false
                                deleteError = error.localizedMessage
                                    ?: "Could not remove profile."
                                showDeleteConfirmation = false
                            }
                        )
                    }
                ) {
                    Text(
                        if (isDeleting) "Removing..." else "Remove"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
