package com.prashant.droidkit.internal

import android.content.Context
import com.prashant.droidkit.core.intent.HistoryRepository
import com.prashant.droidkit.core.intent.IntentFirer
import com.prashant.droidkit.core.notification.ChannelManager
import com.prashant.droidkit.core.notification.NotificationFirer
import com.prashant.droidkit.core.storage.DbInspector
import com.prashant.droidkit.core.storage.PrefsReader

internal object DroidKitServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val prefsReader: PrefsReader by lazy { PrefsReader(appContext) }
    val dbInspector: DbInspector by lazy { DbInspector(appContext) }
    val intentFirer: IntentFirer by lazy { IntentFirer(appContext) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(appContext) }
    val notificationFirer: NotificationFirer by lazy { NotificationFirer(appContext) }
    val channelManager: ChannelManager by lazy { ChannelManager(appContext) }
}
