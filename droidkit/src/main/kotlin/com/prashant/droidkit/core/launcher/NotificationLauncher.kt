package com.prashant.droidkit.core.launcher

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prashant.droidkit.R

internal object NotificationLauncher {

    private const val CHANNEL_ID = "droidkit_launcher"
    private const val NOTIF_ID = 7390

    fun show(context: Context) {
        val nm = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "DroidKit Launcher",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val intent = try {
            val activityClass = Class.forName("com.prashant.droidkit.ui.DroidKitActivity")
            Intent(context, activityClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: ClassNotFoundException) {
            return
        }

        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_droidkit_notif)
            .setContentTitle("DroidKit")
            .setContentText("Tap to open debug toolkit")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        nm.notify(NOTIF_ID, notification)
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }
}
