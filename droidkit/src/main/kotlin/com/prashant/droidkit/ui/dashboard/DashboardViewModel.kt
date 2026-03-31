package com.prashant.droidkit.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import com.prashant.droidkit.DroidKit
import com.prashant.droidkit.core.storage.PrefsReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class DashboardViewModel : ViewModel() {

    enum class LaunchMethod { SHAKE, NOTIFICATION, CODE }

    private val _launchMethod = MutableStateFlow(LaunchMethod.SHAKE)
    val launchMethod: StateFlow<LaunchMethod> = _launchMethod

    init {
        loadLaunchMethod()
    }

    private fun loadLaunchMethod() {
        val ctx = DroidKit.appContext ?: return
        val prefs = ctx.getSharedPreferences(PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString("launch_method", "SHAKE") ?: "SHAKE"
        _launchMethod.value = try {
            LaunchMethod.valueOf(saved)
        } catch (e: IllegalArgumentException) {
            LaunchMethod.SHAKE
        }
    }

    fun setLaunchMethod(method: LaunchMethod) {
        _launchMethod.value = method
        val ctx = DroidKit.appContext ?: return
        ctx.getSharedPreferences(PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("launch_method", method.name)
            .apply()
    }
}
