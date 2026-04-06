package com.prashant.droidkit.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prashant.droidkit.core.network.NetworkCall
import com.prashant.droidkit.core.network.NetworkCallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class NetworkViewModel : ViewModel() {
    private val _selectedMethod = MutableStateFlow<String?>(null)
    val selectedMethod: StateFlow<String?> = _selectedMethod.asStateFlow()

    val calls: StateFlow<List<NetworkCall>> = combine(
        NetworkCallRepository.calls,
        _selectedMethod
    ) { allCalls, method ->
        if (method == null) {
            allCalls
        } else {
            allCalls.filter { it.method == method }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectMethod(method: String?) {
        _selectedMethod.value = method
    }

    fun clearAll() {
        NetworkCallRepository.clear()
    }
}
