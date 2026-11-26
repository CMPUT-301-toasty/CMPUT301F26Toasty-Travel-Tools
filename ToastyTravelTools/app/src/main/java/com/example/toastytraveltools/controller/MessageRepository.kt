package com.example.toastytraveltools.controller

/**
 *
 * Sends messages to Firestore collection
 * Grouped by reservationId
 */

import com.example.toastytraveltools.model.Message
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MessageRepository {
    private val db = Firebase.firestore
    private val messagesRef = db.collection("messages")

    /**
     * Timestamp for each message
     */
    fun sendMessage(
        reservationId: String,
        senderId: String,
        receiverId: String,
        text: String,
        onSent: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        messagesRef.document()
            .set(
                mapOf(
                    "reservationId" to reservationId,
                    "senderId" to senderId,
                    "receiverId" to receiverId,
                    "text" to text,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { onSent() }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Orders conversation message order by oldest message first
     */
    fun listenToConversation(
        reservationId: String,
        onUpdate: (List<Message>) -> Unit
    ): ListenerRegistration {
        return messagesRef
            .whereEqualTo("reservationId", reservationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.toObjects(Message::class.java)
                    // sort here instead of orderBy() so no Firestore index is needed;
                    // messages still sending (no timestamp yet) go last
                    .sortedWith(compareBy(nullsLast()) { it.timestamp })
                onUpdate(messages)
            }
    }

    /**
     * Finds owner of a campsite by its name
     */
    fun findOwnerId(campsiteName: String, onResult: (String?) -> Unit) {
        db.collection("campsites")
            .whereEqualTo("name", campsiteName)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.firstOrNull()?.getString("ownerId"))
            }
            .addOnFailureListener { onResult(null) }
    }
    private val _messages = mutableStateListOf<Message>()

    val campsites: List<Message>
        get() = _messages

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun addMessage(
        message: Message,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ): String {
        val documentRef = messagesRef.document()
        val newMessage = message.copy(id = documentRef.id)

        documentRef.set(newMessage)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }

        return documentRef.id
    }

    fun updateMessage(
        message: Message,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        messagesRef.document(message.id).set(message)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

}