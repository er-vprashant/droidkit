package com.prashant.droidkit.core.storage

import android.content.Context

internal class PrefsReader(private val context: Context) {

    companion object {
        internal const val INTERNAL_PREFS = "droidkit_internal"
    }

    fun getAllFiles(): List<String> {
        return context.filesDir.parentFile
            ?.resolve("shared_prefs")
            ?.listFiles()
            ?.map { it.nameWithoutExtension }
            ?.filter { it != INTERNAL_PREFS }
            ?.sorted()
            ?: emptyList()
    }

    fun getAll(fileName: String): Map<String, Any?> {
        return context
            .getSharedPreferences(fileName, Context.MODE_PRIVATE)
            .all
            .toSortedMap()
    }

    fun setValue(fileName: String, key: String, value: Any?) {
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            .edit().apply {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                    null -> remove(key)
                }
                apply()
            }
    }

    fun deleteKey(fileName: String, key: String) {
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }
}
