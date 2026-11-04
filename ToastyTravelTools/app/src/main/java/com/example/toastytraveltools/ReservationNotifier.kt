package com.example.toastytraveltools

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
}