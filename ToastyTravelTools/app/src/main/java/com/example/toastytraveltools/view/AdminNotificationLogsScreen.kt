
package com.example.toastytraveltools.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

private data class NotificationLog(
    val id: String,
    val sender: String,
    val recipientName: String,
    val message: String,
    val timestamp: Timestamp?
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationLogsScreen(onBack: () -> Unit) {
    val logs = remember { mutableStateListOf<NotificationLog>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener: ListenerRegistration =
            FirebaseFirestore.getInstance()
                .collection("notification_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        errorMessage = "Could not load notifications: ${error.message}"
                        isLoading = false
                        return@addSnapshotListener
                    }

                    logs.clear()

                    snapshot?.documents?.forEach { document ->
                        logs.add(
                            NotificationLog(
                                id = document.id,
                                sender = document.getString("sender") ?: "Unknown",
                                recipientName = document.getString("recipientName")
                                    ?: "Unknown user",
                                message = document.getString("message") ?: "",
                                timestamp = document.getTimestamp("timestamp")
                            )
                        )
                    }

                    isLoading = false
                    errorMessage = null
                }

        onDispose { listener.remove() }
    }

    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Logs") },
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
            when {
                isLoading -> {
                    Text("Loading notifications...")
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                logs.isEmpty() -> {
                    Text("No notifications")
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Sender: ${log.sender}",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text("Recipient: ${log.recipientName}")

                                    Text("Message: ${log.message}")

                                    Text(
                                        text = log.timestamp?.let {
                                            dateFormatter.format(it.toDate())
                                        } ?: "Time unavailable",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
