package com.prashant.droidkit.ui.deeplink

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prashant.droidkit.DroidKit
import com.prashant.droidkit.core.intent.HistoryRepository
import com.prashant.droidkit.core.intent.IntentFirer
import com.prashant.droidkit.core.storage.PrefsReader
import com.prashant.droidkit.internal.DroidKitServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal class DeepLinkViewModel : ViewModel() {

    private val intentFirer: IntentFirer = DroidKitServiceLocator.intentFirer
    private val historyRepo: HistoryRepository = DroidKitServiceLocator.historyRepository

    data class ExtraEntry(val key: String = "", val value: String = "")

    data class Preset(val name: String, val uri: String, val extras: List<ExtraEntry> = emptyList())

    data class UiState(
        val uri: String = "",
        val extras: List<ExtraEntry> = listOf(ExtraEntry()),
        val history: List<String> = emptyList(),
        val presets: List<Preset> = emptyList(),
        val error: String? = null,
        val uriError: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadHistory()
        loadPresets()
    }

    fun setUri(uri: String) {
        _state.value = _state.value.copy(uri = uri, uriError = null)
    }

    fun updateExtra(index: Int, entry: ExtraEntry) {
        val list = _state.value.extras.toMutableList()
        if (index in list.indices) {
            list[index] = entry
            _state.value = _state.value.copy(extras = list)
        }
    }

    fun addExtra() {
        _state.value = _state.value.copy(
            extras = _state.value.extras + ExtraEntry()
        )
    }

    fun removeExtra(index: Int) {
        val list = _state.value.extras.toMutableList()
        if (index in list.indices && list.size > 1) {
            list.removeAt(index)
            _state.value = _state.value.copy(extras = list)
        }
    }

    fun fireIntent() {
        val uri = _state.value.uri.trim()
        if (uri.isBlank()) {
            _state.value = _state.value.copy(uriError = "URI cannot be empty")
            return
        }

        // Validate URI
        try {
            val parsed = Uri.parse(uri)
            if (parsed.scheme.isNullOrBlank()) {
                _state.value = _state.value.copy(uriError = "Invalid URI: missing scheme")
                return
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(uriError = "Malformed URI")
            return
        }

        val extras = _state.value.extras
            .filter { it.key.isNotBlank() }
            .associate { it.key to it.value }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                intentFirer.fire(uri, extras)
                historyRepo.addEntry(uri)
                loadHistory()
                _state.value = _state.value.copy(error = null)
            } catch (e: IntentFirer.NoHandlerException) {
                _state.value = _state.value.copy(error = "No app handles this URI")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Failed to fire intent")
            }
        }
    }

    fun loadFromHistory(uri: String) {
        _state.value = _state.value.copy(uri = uri, uriError = null)
    }

    fun deleteFromHistory(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepo.removeEntry(uri)
            loadHistory()
        }
    }

    fun loadPreset(preset: Preset) {
        _state.value = _state.value.copy(
            uri = preset.uri,
            extras = if (preset.extras.isEmpty()) listOf(ExtraEntry()) else preset.extras,
            uriError = null
        )
    }

    fun savePreset(name: String) {
        val preset = Preset(
            name = name,
            uri = _state.value.uri,
            extras = _state.value.extras.filter { it.key.isNotBlank() }
        )
        val ctx = DroidKit.appContext ?: return
        val prefs = ctx.getSharedPreferences(PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE)
        val existing = _state.value.presets.toMutableList()
        existing.removeAll { it.name == name }
        existing.add(0, preset)

        val jsonArray = JSONArray()
        existing.forEach { p ->
            val obj = JSONObject().apply {
                put("name", p.name)
                put("uri", p.uri)
                val extrasArr = JSONArray()
                p.extras.forEach { e ->
                    extrasArr.put(JSONObject().apply {
                        put("key", e.key)
                        put("value", e.value)
                    })
                }
                put("extras", extrasArr)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("deeplink_presets", jsonArray.toString()).apply()
        _state.value = _state.value.copy(presets = existing)
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun loadHistory() {
        val history = historyRepo.getHistory()
        _state.value = _state.value.copy(history = history)
    }

    private fun loadPresets() {
        val ctx = DroidKit.appContext ?: return
        val prefs = ctx.getSharedPreferences(PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString("deeplink_presets", "[]") ?: "[]"
        try {
            val arr = JSONArray(json)
            val presets = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val extrasArr = obj.optJSONArray("extras") ?: JSONArray()
                Preset(
                    name = obj.getString("name"),
                    uri = obj.getString("uri"),
                    extras = (0 until extrasArr.length()).map { j ->
                        val e = extrasArr.getJSONObject(j)
                        ExtraEntry(e.getString("key"), e.getString("value"))
                    }
                )
            }
            _state.value = _state.value.copy(presets = presets)
        } catch (e: Exception) {
            // ignore malformed presets
        }
    }
}
