package com.prashant.droidkit.core.intent

import android.content.Context
import com.prashant.droidkit.core.storage.PrefsReader
import org.json.JSONArray

internal class HistoryRepository(private val context: Context) {

    private val prefs get() = context.getSharedPreferences(
        PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE
    )

    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addEntry(uri: String) {
        val history = getHistory().toMutableList()
        history.remove(uri)
        history.add(0, uri)
        val capped = history.take(MAX_HISTORY)
        prefs.edit().putString(KEY_HISTORY, JSONArray(capped).toString()).apply()
    }

    fun removeEntry(uri: String) {
        val history = getHistory().toMutableList()
        history.remove(uri)
        prefs.edit().putString(KEY_HISTORY, JSONArray(history).toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val MAX_HISTORY = 10
        private const val KEY_HISTORY = "deeplink_history"
    }
}
