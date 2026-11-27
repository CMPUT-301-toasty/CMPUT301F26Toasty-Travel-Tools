package com.example.toastytraveltools.view.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.google.firebase.firestore.FirebaseFirestore

const val ADMIN_ROLE = "admin"

/**
 * Watches entrants/{deviceId}.role and reports whether this device is an admin.
 *
 * Returns null while the role is still loading, then true or false. A missing profile
 * or a failed read counts as not admin.
 */
@Composable
fun rememberIsAdmin(deviceId: String): Boolean? {
    var isAdmin by remember(deviceId) { mutableStateOf<Boolean?>(null) }
    DisposableEffect(deviceId) {
        if (deviceId.isBlank()) {
            isAdmin = false
            return@DisposableEffect onDispose { }
        }
        val registration = FirebaseFirestore.getInstance()
            .collection("entrants")
            .document(deviceId)
            .addSnapshotListener { doc, error ->
                isAdmin = if (error != null || doc == null) false
                else doc.getString("role") == ADMIN_ROLE
            }
        onDispose { registration.remove() }
    }
    return isAdmin
}

/**
 * Renders [content] only for admins.
 *
 * - null (role loading): a spinner, never the admin content
 * - false: calls [onDenied] once (the caller redirects to Explore)
 * - true: the admin content
 *
 * This only hides admin screens in the app. It is not access control: Firestore must
 * enforce the role in its security rules for that.
 */
@Composable
fun AdminGate(
    isAdmin: Boolean?,
    onDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    when (isAdmin) {
        true -> content()
        false -> LaunchedEffect(Unit) { onDenied() }
        null -> Box(
            modifier = Modifier.fillMaxSize().testTag("adminGateLoading"),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
