package com.prashant.droidkit.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prashant.droidkit.R

internal class NotificationFirer(private val context: Context) {

    fun fire(payload: NotificationPayload) {
        ChannelManager(context).ensureChannel(payload.channel)

        val pendingIntent = payload.deepLink?.takeIf { it.isNotBlank() }?.let {
            PendingIntent.getActivity(
                context, 0,
                Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, payload.channel.id)
            .setSmallIcon(R.drawable.ic_droidkit_notif)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                when (payload.channel) {
                    NotifChannel.SILENT -> NotificationCompat.PRIORITY_LOW
                    NotifChannel.HEADS_UP -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    companion object {
        private const val NOTIF_ID = 7391
    }
}

data class NotificationPayload(
    val title: String,
    val body: String,
    val deepLink: String? = null,
    val channel: NotifChannel = NotifChannel.DEFAULT
)

enum class NotifChannel(val id: String, val displayName: String) {
    DEFAULT("droidkit_default", "Default"),
    SILENT("droidkit_silent", "Silent"),
    HEADS_UP("droidkit_headsup", "Heads-up")
}
