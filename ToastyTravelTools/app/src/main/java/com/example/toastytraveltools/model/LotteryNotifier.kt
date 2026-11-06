package com.example.toastytraveltools.model

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

object LotteryNotifier {
    private const val CHANNEL_ID = "lotteries"

    @SuppressLint("MissingPermission")
    fun send(context: Context, l: Lottery, win: Boolean) {
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

        if(win) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Congratulations!")
                .setContentText("You have just won the lottery: ${l.lotteryName}, book your reservation(s) on the app.")
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(l.id.hashCode(), notification)
        } else {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Lottery closed.")
                .setContentText("Unfortunately, you did not win the lottery: ${l.lotteryName}.")
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(l.id.hashCode(), notification)
        }
    }
}