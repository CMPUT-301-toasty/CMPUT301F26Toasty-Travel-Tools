/*
ReservationNotifier.kt

Purpose: Sends a notification to show that a reservation has been confirmed. Checks to see
if the entrant has opted in to notifications. sendCancelled shows that a reservation was cancelled.

Issues:
 - no outstanding issues currently
 */

package com.example.toastytraveltools.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.toastytraveltools.R
import com.example.toastytraveltools.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

object NotificationPreferences {
    fun ifEnabled(entrantId: String, action: () -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("entrants").document(entrantId).get()
            .addOnSuccessListener { doc ->
                if (doc.getBoolean("notificationsEnabled") ?: true) action()
            }
    }
}

// call ifEnabled function before sending notifications so that
// people who have opted out are never sent a notification

object ReservationNotifier {
    private const val CHANNEL_ID = "reservations"

    @SuppressLint("MissingPermission")
    fun send(context: Context, r: Reservation) {
        // android 13+ needs the runtime permission, otherwise do nothing
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Reservations", NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reservation confirmed: ${r.campsiteName}")
            .setContentText("${r.startDate} to ${r.endDate}")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(r.id.hashCode(), notification)
    }

    // tells the entrant their reservation was cancelled
    @SuppressLint("MissingPermission")
    fun sendCancelled(context: Context, r: Reservation) {
        // android 13+ needs the runtime permission, otherwise do nothing
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Reservations", NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reservation cancelled: ${r.campsiteName}")
            .setContentText(
                "Your stay from ${fmt.format(r.startDate.toDate())} to ${fmt.format(r.endDate.toDate())} has been cancelled."
            )
            .setAutoCancel(true)
            .build()

        // different id from the confirmed notification so they don't replace each other
        NotificationManagerCompat.from(context).notify("cancel_${r.id}".hashCode(), notification)
    }
}