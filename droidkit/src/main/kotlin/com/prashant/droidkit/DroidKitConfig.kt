package com.prashant.droidkit

import android.content.Context

data class DroidKitConfig(
    val autoInit: Boolean = true,
    val launchOnShake: Boolean = true,
    val showNotification: Boolean = true,
    val enabledModules: Set<Module> = Module.entries.toSet()
) {

    class Builder {
        private var launchOnShake = true
        private var showNotification = true
        private val disabled = mutableSetOf<Module>()

        fun launchOnShake(v: Boolean) = apply { launchOnShake = v }
        fun showNotification(v: Boolean) = apply { showNotification = v }
        fun disable(m: Module) = apply { disabled += m }

        fun init(context: Context) {
            DroidKit.initInternal(
                context,
                DroidKitConfig(
                    launchOnShake = launchOnShake,
                    showNotification = showNotification,
                    enabledModules = Module.entries.toSet() - disabled
                )
            )
        }
    }

    companion object {
        internal fun readFromManifest(context: Context): DroidKitConfig {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val meta = appInfo.metaData
            val autoInit = meta?.getBoolean("droidkit_auto_init", true) ?: true
            return DroidKitConfig(autoInit = autoInit)
        }
    }
}

enum class Module { STORAGE, DEEP_LINK, NOTIFICATIONS }
