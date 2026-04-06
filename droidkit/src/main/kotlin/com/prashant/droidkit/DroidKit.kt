package com.prashant.droidkit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.prashant.droidkit.core.launcher.NotificationLauncher
import com.prashant.droidkit.core.network.DroidKitNetworkInterceptor
import com.prashant.droidkit.core.shake.ShakeDetector
import com.prashant.droidkit.internal.DroidKitServiceLocator
import com.prashant.droidkit.ui.DroidKitActivity
import okhttp3.Interceptor

object DroidKit {

    internal var config: DroidKitConfig? = null
        private set
    internal var appContext: Context? = null
        private set
    private var lifecycleRegistered = false

    internal fun initInternal(context: Context, cfg: DroidKitConfig) {
        appContext = context.applicationContext
        config = cfg
        DroidKitServiceLocator.init(context.applicationContext)
        ShakeDetector.start(context.applicationContext, cfg.launchOnShake) { launch(context.applicationContext) }

        if (cfg.showNotification) {
            NotificationLauncher.show(context.applicationContext)
            registerLifecycleIfNeeded(context.applicationContext)
        }
    }

    /**
     * Re-attempt showing the launcher notification.
     * Call after the user grants POST_NOTIFICATIONS permission.
     */
    fun refreshNotification() {
        val ctx = appContext ?: return
        if (config?.showNotification == true) {
            NotificationLauncher.show(ctx)
        }
    }

    /** Manual launch — call from any debug button in the host app */
    fun launch(context: Context) {
        context.startActivity(
            Intent(context, DroidKitActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    /** Builder for teams that need explicit control */
    fun builder() = DroidKitConfig.Builder()

    /**
     * Returns an OkHttp Interceptor for network inspection and mocking.
     * Add this to your OkHttpClient:
     * ```
     * val client = OkHttpClient.Builder()
     *     .addInterceptor(DroidKit.networkInterceptor())
     *     .build()
     * ```
     */
    fun networkInterceptor(): Interceptor {
        val ctx = appContext ?: throw IllegalStateException(
            "DroidKit not initialized. Ensure DroidKit is auto-initialized or call DroidKit.builder().build(context)."
        )
        return DroidKitNetworkInterceptor(ctx)
    }

    private fun registerLifecycleIfNeeded(context: Context) {
        if (lifecycleRegistered) return
        val app = context as? Application ?: return
        lifecycleRegistered = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is DroidKitActivity) return
                NotificationLauncher.show(activity.applicationContext)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        })
    }
}
