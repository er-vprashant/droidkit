package com.prashant.droidkit.core.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ChannelManagerTest {

    private lateinit var context: Context
    private lateinit var manager: ChannelManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = ChannelManager(context)

        // Clear existing channels
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notificationChannels.forEach { nm.deleteNotificationChannel(it.id) }
    }

    @Test
    fun `ensureChannel creates default channel`() {
        manager.ensureChannel(NotifChannel.DEFAULT)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(NotifChannel.DEFAULT.id)
        assertNotNull(channel)
        assertEquals(NotifChannel.DEFAULT.displayName, channel!!.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
    }

    @Test
    fun `ensureChannel creates silent channel with low importance`() {
        manager.ensureChannel(NotifChannel.SILENT)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(NotifChannel.SILENT.id)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel!!.importance)
    }

    @Test
    fun `ensureChannel creates headsup channel with high importance`() {
        manager.ensureChannel(NotifChannel.HEADS_UP)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(NotifChannel.HEADS_UP.id)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel!!.importance)
    }

    @Test
    fun `ensureChannel is idempotent`() {
        manager.ensureChannel(NotifChannel.DEFAULT)
        manager.ensureChannel(NotifChannel.DEFAULT) // no crash

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = nm.notificationChannels.filter { it.id == NotifChannel.DEFAULT.id }
        assertEquals(1, channels.size)
    }

    @Test
    fun `NotifChannel enum has correct ids and names`() {
        assertEquals("droidkit_default", NotifChannel.DEFAULT.id)
        assertEquals("Default", NotifChannel.DEFAULT.displayName)
        assertEquals("droidkit_silent", NotifChannel.SILENT.id)
        assertEquals("Silent", NotifChannel.SILENT.displayName)
        assertEquals("droidkit_headsup", NotifChannel.HEADS_UP.id)
        assertEquals("Heads-up", NotifChannel.HEADS_UP.displayName)
    }
}
