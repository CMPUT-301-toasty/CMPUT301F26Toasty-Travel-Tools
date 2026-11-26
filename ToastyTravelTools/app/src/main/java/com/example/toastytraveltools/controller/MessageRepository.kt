package com.example.toastytraveltools.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.toastytraveltools.model.Message
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class MessageRepository {
    private val db = Firebase.firestore
    private val messagesRef = db.collection("messages")

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