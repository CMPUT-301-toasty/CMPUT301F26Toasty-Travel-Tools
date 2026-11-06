/*
LotteryNotifier.kt

Purpose: If the user doesn't have notifications on, nothing is displayed. It is
what displays the actual notification for winning/losing lotteries.

Issues:
 - No issues at the moment
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
import com.example.toastytraveltools.model.Lottery

object LotteryNotifier {
    private const val CHANNEL_ID = "lotteries"

    @SuppressLint("MissingPermission")
    fun send(context: Context, l: Lottery, win: Boolean, campsiteName: String = "") {
        //If user has won the lottery, set win argument to true
        // android 13+ needs the runtime permission, otherwise do nothing
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Lotteries", NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val place = campsiteName.ifBlank { l.lotteryName.ifBlank { "the campsite" } }

        if(win) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Congratulations!")
                .setContentText("You have just won the lottery for $place, book your reservation(s) on the app.")
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(l.id.hashCode(), notification)
        } else {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Lottery closed.")
                .setContentText("Unfortunately, you did not win the lottery for $place.")
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(l.id.hashCode(), notification)
        }
    }
}