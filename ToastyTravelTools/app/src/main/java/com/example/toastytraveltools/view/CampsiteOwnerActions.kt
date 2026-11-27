/*
CampsiteOwnerActions.kt

Purpose: The Instagram-style part of Campsite Details. On a campsite you own you get the
owner tools (edit, delete, create a lottery, view bookings) instead of the guest actions.
Kept stateless so it can be tested without Firestore.
 */

package com.example.toastytraveltools.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.view.components.ConfirmDestructiveDialog

/**
 * Owner tools for one campsite. Delete asks for confirmation first; [onDeleteConfirmed]
 * runs only after the owner confirms.
 */
@Composable
fun OwnerActionsCard(
    campsiteName: String,
    onEdit: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onCreateLottery: () -> Unit,
    onViewBookings: () -> Unit,
    deleting: Boolean = false,
    deleteError: String? = null
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        // paper surface so the hairline borders of the outlined buttons stay visible
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "YOU OWN THIS CAMPSITE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCreateLottery,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) { Text("Create lottery") }
                OutlinedButton(
                    onClick = onViewBookings,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) { Text("View bookings") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) { Text("Edit") }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !deleting,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) { Text("Delete") }
            }
            deleteError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDestructiveDialog(
            title = "Delete campsite?",
            message = "This permanently deletes $campsiteName, its photos, its lotteries and its " +
                    "reservations. Guests with bookings will be notified. This cannot be undone.",
            confirmLabel = "Delete campsite",
            working = deleting,
            onConfirm = {
                confirmDelete = false
                onDeleteConfirmed()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/**
 * Picks what the Details page shows under the campsite info: owner tools if you own it,
 * otherwise [guestContent] (join/leave lottery). Owners never see the guest actions.
 */
@Composable
fun CampsiteActionsSection(
    isOwner: Boolean,
    campsiteName: String,
    onEdit: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onCreateLottery: () -> Unit,
    onViewBookings: () -> Unit,
    deleting: Boolean = false,
    deleteError: String? = null,
    guestContent: @Composable () -> Unit
) {
    if (isOwner) {
        OwnerActionsCard(
            campsiteName = campsiteName,
            onEdit = onEdit,
            onDeleteConfirmed = onDeleteConfirmed,
            onCreateLottery = onCreateLottery,
            onViewBookings = onViewBookings,
            deleting = deleting,
            deleteError = deleteError
        )
    } else {
        guestContent()
    }
}
