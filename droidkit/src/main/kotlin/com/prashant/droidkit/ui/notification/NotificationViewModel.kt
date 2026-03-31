package com.prashant.droidkit.ui.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.prashant.droidkit.DroidKit
import com.prashant.droidkit.core.notification.NotifChannel
import com.prashant.droidkit.core.notification.NotificationFirer
import com.prashant.droidkit.core.notification.NotificationPayload
import com.prashant.droidkit.internal.DroidKitServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NotificationViewModel : ViewModel() {

    private val notificationFirer: NotificationFirer = DroidKitServiceLocator.notificationFirer

    data class UiState(
        val title: String = "",
        val body: String = "",
        val deepLink: String = "",
        val selectedChannel: NotifChannel = NotifChannel.DEFAULT,
        val hasPermission: Boolean = true,
        val permissionDeniedPermanently: Boolean = false,
        val error: String? = null
    )

    data class Preset(
        val name: String,
        val title: String,
        val body: String,
        val deepLink: String,
        val channel: NotifChannel
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val presets = listOf(
        Preset("Default", "Test notification", "Hello from DroidKit", "", NotifChannel.DEFAULT),
        Preset("Success", "Payment successful", "Your payment of ₹499 was received", "app://orders/latest", NotifChannel.DEFAULT),
        Preset("Promo", "Limited time offer", "Get 30% off — today only!", "app://promo/SALE30", NotifChannel.DEFAULT),
        Preset("OTP", "Your OTP is 847291", "Valid for 10 minutes. Do not share.", "", NotifChannel.HEADS_UP),
        Preset("Alert", "Action required", "Please verify your email address", "app://settings/email", NotifChannel.HEADS_UP)
    )

    init {
        checkPermission()
    }

    fun checkPermission() {
        val ctx = DroidKit.appContext ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            _state.value = _state.value.copy(hasPermission = granted)
        } else {
            _state.value = _state.value.copy(hasPermission = true)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(
            hasPermission = granted,
            permissionDeniedPermanently = !granted
        )
    }

    fun setTitle(title: String) {
        _state.value = _state.value.copy(title = title)
    }

    fun setBody(body: String) {
        _state.value = _state.value.copy(body = body)
    }

    fun setDeepLink(deepLink: String) {
        _state.value = _state.value.copy(deepLink = deepLink)
    }

    fun setChannel(channel: NotifChannel) {
        _state.value = _state.value.copy(selectedChannel = channel)
    }

    fun loadPreset(preset: Preset) {
        _state.value = _state.value.copy(
            title = preset.title,
            body = preset.body,
            deepLink = preset.deepLink,
            selectedChannel = preset.channel
        )
    }

    fun sendNotification() {
        val s = _state.value
        if (!s.hasPermission) {
            _state.value = s.copy(error = "Grant notification permission to send test notifications.")
            return
        }
        if (s.title.isBlank()) {
            _state.value = s.copy(error = "Title cannot be empty")
            return
        }

        try {
            notificationFirer.fire(
                NotificationPayload(
                    title = s.title,
                    body = s.body,
                    deepLink = s.deepLink.takeIf { it.isNotBlank() },
                    channel = s.selectedChannel
                )
            )
            _state.value = s.copy(error = null)
        } catch (e: Exception) {
            _state.value = s.copy(error = e.message ?: "Failed to send notification")
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}
