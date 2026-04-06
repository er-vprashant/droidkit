package com.prashant.droidkit.ui.network

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.prashant.droidkit.core.network.MockRepository
import com.prashant.droidkit.core.network.MockResponse

internal class MockEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val mockRepository = MockRepository(application.applicationContext)

    fun loadMock(id: String): MockResponse? {
        return mockRepository.getById(id)
    }

    fun saveMock(mock: MockResponse, isNew: Boolean) {
        if (isNew) {
            mockRepository.add(mock)
        } else {
            mockRepository.update(mock)
        }
    }
}
