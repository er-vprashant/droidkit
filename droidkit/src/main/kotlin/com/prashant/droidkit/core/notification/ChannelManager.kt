package com.prashant.droidkit.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

internal class ChannelManager(private val context: Context) {

    fun ensureChannel(channel: NotifChannel) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channel.id) != null) return

        val importance = when (channel) {
            NotifChannel.SILENT -> NotificationManager.IMPORTANCE_LOW
            NotifChannel.HEADS_UP -> NotificationManager.IMPORTANCE_HIGH
            NotifChannel.DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
        }
        val nc = NotificationChannel(channel.id, channel.displayName, importance)
        nm.createNotificationChannel(nc)
    }
}
