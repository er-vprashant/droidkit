package com.prashant.droidkit.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object NetworkCallRepository {
    private const val MAX_CALLS = 200
    
    private val _calls = MutableStateFlow<List<NetworkCall>>(emptyList())
    val calls: StateFlow<List<NetworkCall>> = _calls.asStateFlow()

    @Synchronized
    fun add(call: NetworkCall) {
        val current = _calls.value.toMutableList()
        current.add(0, call)
        
        if (current.size > MAX_CALLS) {
            current.removeAt(current.lastIndex)
        }
        
        _calls.value = current
    }

    @Synchronized
    fun clear() {
        _calls.value = emptyList()
    }

    fun getById(id: String): NetworkCall? {
        return _calls.value.find { it.id == id }
    }

    fun filter(
        method: String? = null,
        statusCode: Int? = null,
        host: String? = null
    ): List<NetworkCall> {
        return _calls.value.filter { call ->
            (method == null || call.method == method) &&
            (statusCode == null || call.responseStatus == statusCode) &&
            (host == null || call.host == host)
        }
    }
}
