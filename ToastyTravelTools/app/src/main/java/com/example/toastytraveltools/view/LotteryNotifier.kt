
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
import com.example.toastytraveltools.model.Lottery
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object LotteryNotifier {
    private const val CHANNEL_ID = "lotteries"

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun send(
        context: Context,
        l: Lottery,
        win: Boolean,
        campsiteName: String = "",
        recipientId: String,
        recipientName: String
    ) {
        if (!canPost(context)) return

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lotteries",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val place = campsiteName.ifBlank {
            l.lotteryName.ifBlank { "the campsite" }
        }

        val title: String
        val message: String

        if (win) {
            title = "Congratulations!"
            message = "You have just won the lottery for $place, book your reservation(s) on the app."
        } else {
            title = "Lottery closed."
            message = "Unfortunately, you did not win the lottery for $place."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        // Only create a log after the Android notification is successfully posted.
        try {
            NotificationManagerCompat.from(context)
                .notify(l.id.hashCode(), notification)

            FirebaseFirestore.getInstance()
                .collection("notification_logs")
                .add(
                    mapOf(
                        "sender" to "System",
                        "recipientId" to recipientId,
                        "recipientName" to recipientName,
                        "message" to message,
                        "title" to title,
                        "type" to if (win) "Lottery Win" else "Lottery Loss",
                        "lotteryId" to l.id,
                        "campsiteName" to place,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
        } catch (e: SecurityException) {
            // Permission was revoked before the notification could be posted.
        }
    }
}
