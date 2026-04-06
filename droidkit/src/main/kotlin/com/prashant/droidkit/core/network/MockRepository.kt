package com.prashant.droidkit.core.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class MockRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("droidkit_mocks", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _mocks = MutableStateFlow<List<MockResponse>>(emptyList())
    val mocks: StateFlow<List<MockResponse>> = _mocks.asStateFlow()

    init {
        loadMocks()
    }

    private fun loadMocks() {
        val json = prefs.getString("mocks", null) ?: return
        try {
            val type = object : TypeToken<List<MockResponse>>() {}.type
            val loaded = gson.fromJson<List<MockResponse>>(json, type)
            _mocks.value = loaded
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun saveMocks() {
        val json = gson.toJson(_mocks.value)
        prefs.edit().putString("mocks", json).apply()
    }

    fun findMatch(url: String, method: String): MockResponse? {
        return _mocks.value.firstOrNull { it.matches(url, method) }
    }

    fun add(mock: MockResponse) {
        val current = _mocks.value.toMutableList()
        current.add(mock)
        _mocks.value = current
        saveMocks()
    }

    fun update(mock: MockResponse) {
        val current = _mocks.value.toMutableList()
        val index = current.indexOfFirst { it.id == mock.id }
        if (index >= 0) {
            current[index] = mock
            _mocks.value = current
            saveMocks()
        }
    }

    fun delete(id: String) {
        val current = _mocks.value.toMutableList()
        current.removeAll { it.id == id }
        _mocks.value = current
        saveMocks()
    }

    fun getById(id: String): MockResponse? {
        return _mocks.value.find { it.id == id }
    }

    fun clear() {
        _mocks.value = emptyList()
        saveMocks()
    }
}
