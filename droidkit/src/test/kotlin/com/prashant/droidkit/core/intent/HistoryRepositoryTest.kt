package com.prashant.droidkit.core.intent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.prashant.droidkit.core.storage.PrefsReader
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class HistoryRepositoryTest {

    private lateinit var context: Context
    private lateinit var repo: HistoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear internal prefs before each test
        context.getSharedPreferences(PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE)
            .edit().clear().commit()
        repo = HistoryRepository(context)
    }

    @Test
    fun `getHistory returns empty when no entries`() {
        assertTrue(repo.getHistory().isEmpty())
    }

    @Test
    fun `addEntry adds uri to history`() {
        repo.addEntry("app://home")
        val history = repo.getHistory()
        assertEquals(1, history.size)
        assertEquals("app://home", history[0])
    }

    @Test
    fun `addEntry prepends most recent at top`() {
        repo.addEntry("app://first")
        repo.addEntry("app://second")
        val history = repo.getHistory()
        assertEquals("app://second", history[0])
        assertEquals("app://first", history[1])
    }

    @Test
    fun `addEntry deduplicates and moves to top`() {
        repo.addEntry("app://a")
        repo.addEntry("app://b")
        repo.addEntry("app://a") // re-add

        val history = repo.getHistory()
        assertEquals(2, history.size)
        assertEquals("app://a", history[0])
        assertEquals("app://b", history[1])
    }

    @Test
    fun `addEntry caps at 10 entries`() {
        for (i in 1..15) {
            repo.addEntry("app://link$i")
        }
        val history = repo.getHistory()
        assertEquals(10, history.size)
        assertEquals("app://link15", history[0])
    }

    @Test
    fun `removeEntry removes specific uri`() {
        repo.addEntry("app://keep")
        repo.addEntry("app://remove")

        repo.removeEntry("app://remove")

        val history = repo.getHistory()
        assertEquals(1, history.size)
        assertEquals("app://keep", history[0])
    }

    @Test
    fun `removeEntry no-ops for nonexistent uri`() {
        repo.addEntry("app://keep")
        repo.removeEntry("app://nonexistent")
        assertEquals(1, repo.getHistory().size)
    }

    @Test
    fun `clear removes all entries`() {
        repo.addEntry("app://a")
        repo.addEntry("app://b")
        repo.clear()
        assertTrue(repo.getHistory().isEmpty())
    }

    @Test
    fun `getHistory handles corrupted json gracefully`() {
        context.getSharedPreferences(PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE)
            .edit().putString("deeplink_history", "not_valid_json").commit()

        val history = repo.getHistory()
        assertTrue(history.isEmpty())
    }
}
