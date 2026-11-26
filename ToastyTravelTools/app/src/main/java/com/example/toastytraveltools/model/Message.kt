package com.example.toastytraveltools.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId


/**
 * Represents a reservation made by an entrant for a campsite.
 *
 * Reservation information is stored in Firestore and is used
 * by reservation and lottery features.
 *
 * @property id Unique reservation document ID.
 * @property messageType Type of message, whether it is a direct message or a review.
 * @property sendTime When the message was created.
 * @property messageOwner Who has permission to edit/delete the message.
 * @property text The text body of the message.
 * @property reply Optional id of message that is referenced by this message as a reply. Used when message is assumed to only contain one reply
 * @property replies Optional id of message that is referenced by this message as a reply. Used when message can have more than one reply
 */

data class Message (
    @DocumentId var id: String = "",
    var messageType: String = "",
    var sendTime: Timestamp = Timestamp(0,0),
    var messageOwner: String = "",
    var text: String = "",
    var reply: String? = null,
    var replies: List<String>? = null
    )