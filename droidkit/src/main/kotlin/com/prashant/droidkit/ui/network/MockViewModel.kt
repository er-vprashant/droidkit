package com.prashant.droidkit.ui.network

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.prashant.droidkit.core.network.MockRepository
import com.prashant.droidkit.core.network.MockResponse
import kotlinx.coroutines.flow.StateFlow

internal class MockViewModel(application: Application) : AndroidViewModel(application) {
    private val mockRepository = MockRepository(application.applicationContext)
    
    val mocks: StateFlow<List<MockResponse>> = mockRepository.mocks

    fun toggleMock(id: String) {
        val mock = mockRepository.getById(id) ?: return
        mockRepository.update(mock.copy(enabled = !mock.enabled))
    }

    fun deleteMock(id: String) {
        mockRepository.delete(id)
    }
}
