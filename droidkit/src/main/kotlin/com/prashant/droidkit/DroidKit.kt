package com.prashant.droidkit

import android.content.Context
import android.content.Intent
import com.prashant.droidkit.core.launcher.NotificationLauncher
import com.prashant.droidkit.core.shake.ShakeDetector
import com.prashant.droidkit.internal.DroidKitServiceLocator

object DroidKit {

    internal var config: DroidKitConfig? = null
        private set
    internal var appContext: Context? = null
        private set

    internal fun initInternal(context: Context, cfg: DroidKitConfig) {
        appContext = context.applicationContext
        config = cfg
        DroidKitServiceLocator.init(context.applicationContext)
        ShakeDetector.start(context.applicationContext, cfg.launchOnShake) { launch(context.applicationContext) }
        if (cfg.showNotification) NotificationLauncher.show(context.applicationContext)
    }

    /** Manual launch — call from any debug button in the host app */
    fun launch(context: Context) {
        try {
            val activityClass = Class.forName("com.prashant.droidkit.ui.DroidKitActivity")
            context.startActivity(
                Intent(context, activityClass).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        } catch (e: ClassNotFoundException) {
            // DroidKitActivity only exists in debug builds
        }
    }

    /** Builder for teams that need explicit control */
    fun builder() = DroidKitConfig.Builder()
}
