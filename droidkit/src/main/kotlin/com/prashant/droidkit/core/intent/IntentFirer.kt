package com.prashant.droidkit.core.intent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

internal class IntentFirer(private val context: Context) {

    fun fire(uri: String, extras: Map<String, String> = emptyMap()) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            extras.forEach { (k, v) -> putExtra(k, v) }
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw NoHandlerException(uri)
        }
    }

    class NoHandlerException(val uri: String) : Exception("No activity handles: $uri")
}
