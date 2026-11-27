package com.example.toastytraveltools.view

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.controller.MessageRepository
import com.example.toastytraveltools.model.Message
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Two-way chat between an entrant and a campsite owner about one reservation.
 *
 * @param reservationId the reservation this conversation belongs to
 * @param onBack navigates back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    reservationId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }
    val repository = remember { MessageRepository() }

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var ownerId by remember { mutableStateOf<String?>(null) }
    var entrantId by remember { mutableStateOf<String?>(null)}
    var campsiteName by remember { mutableStateOf("")}
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Live updates; stop listening when the screen closes
    DisposableEffect(reservationId) {
        val registration = repository.listenToConversation(reservationId) { messages = it }
        onDispose { registration.remove() }
    }

    // Load who the entrant is and which campsite, then find its owner
    LaunchedEffect(reservationId) {
        FirebaseFirestore.getInstance()
            .collection("reservations")
            .document(reservationId)
            .get()
            .addOnSuccessListener { doc ->
                entrantId = doc.getString("entrant_id") ?: doc.getString("entrantId") ?: ""
                val name = doc.getString("campsiteName") ?: ""
                campsiteName = name
                repository.findOwnerId(name) { ownerId = it }
            }
            .addOnFailureListener { error = "Could not load this conversation" }
    }

    // Entrant talks to the owner; owner talks to the entrant
    val receiverId = when (entrantId) {
        null -> null                 // still loading
        deviceId -> ownerId          // I'm the entrant
        else -> entrantId            // I'm the owner
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(campsiteName.ifBlank{"Messages"}) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            "No messages yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(messages, key = { it.id }) { message ->
                    val mine = message.senderId == deviceId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (mine) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = if (mine) null
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            // Figma bubbles: 14dp corners with a 4dp "tail" on the sender's side
                            shape = if (mine) RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)
                            else RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message.text,
                                color = if (mine) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = {
                        val to = receiverId
                        if (to != null) {
                            error = null
                            repository.sendMessage(
                                reservationId = reservationId,
                                senderId = deviceId,
                                receiverId = to,
                                text = draft.trim(),
                                onError = { error = "Could not send: ${it.message}" }
                            )
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank() && receiverId != null
                ) {
                    Text("Send")
                }
            }
        }
    }
}

