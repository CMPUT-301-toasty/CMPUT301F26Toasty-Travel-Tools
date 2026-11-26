package com.example.toastytraveltools.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A singular chat between entrant and campsite owner
 *
 * @property id document ID generated from Firestore
 * @property reservationId actual reservation we are talking about
 * @property senderId device ID sender
 * @property receiverId device ID receiver
 * @property text the message content
 * @property timestamp when the server received it
 */
data class Message(
    @DocumentId val id: String = "",
    val reservationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)