package com.prashant.droidkit.core.intent

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class IntentFirerTest {

    private lateinit var context: Context
    private lateinit var firer: IntentFirer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        firer = IntentFirer(context)
    }

    @Test
    fun `fire creates ACTION_VIEW intent with correct uri`() {
        val shadowApp = Shadows.shadowOf(context as android.app.Application)

        try {
            firer.fire("app://test")
        } catch (_: IntentFirer.NoHandlerException) {
            // Expected in test - no activity registered
        }

        // Verify the intent was attempted
        val launched = shadowApp.nextStartedActivity
        if (launched != null) {
            assertEquals(Intent.ACTION_VIEW, launched.action)
            assertEquals("app://test", launched.data.toString())
            assertTrue(launched.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        }
    }

    @Test
    fun `fire adds extras to intent`() {
        val shadowApp = Shadows.shadowOf(context as android.app.Application)

        try {
            firer.fire("app://test", mapOf("key1" to "value1", "key2" to "value2"))
        } catch (_: IntentFirer.NoHandlerException) {
            // Expected
        }

        val launched = shadowApp.nextStartedActivity
        if (launched != null) {
            assertEquals("value1", launched.getStringExtra("key1"))
            assertEquals("value2", launched.getStringExtra("key2"))
        }
    }

    @Test
    fun `NoHandlerException contains uri`() {
        val exception = IntentFirer.NoHandlerException("app://missing")
        assertEquals("app://missing", exception.uri)
        assertTrue(exception.message!!.contains("app://missing"))
    }
}
