package com.prashant.droidkit.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PrefsReaderTest {

    private lateinit var context: Context
    private lateinit var reader: PrefsReader

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        reader = PrefsReader(context)
        // Clear any leftover prefs
        context.filesDir.parentFile?.resolve("shared_prefs")?.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `getAllFiles returns empty when no prefs exist`() {
        val files = reader.getAllFiles()
        assertTrue(files.isEmpty())
    }

    @Test
    fun `getAllFiles returns prefs file names`() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().putString("k", "v").commit()
        context.getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().putString("k", "v").commit()

        val files = reader.getAllFiles()
        assertTrue(files.contains("app_settings"))
        assertTrue(files.contains("user_data"))
    }

    @Test
    fun `getAllFiles filters out droidkit_internal`() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().putString("k", "v").commit()
        context.getSharedPreferences(PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE).edit().putString("k", "v").commit()

        val files = reader.getAllFiles()
        assertTrue(files.contains("app_settings"))
        assertFalse(files.contains(PrefsReader.INTERNAL_PREFS))
    }

    @Test
    fun `getAllFiles returns sorted list`() {
        context.getSharedPreferences("z_prefs", Context.MODE_PRIVATE).edit().putString("k", "v").commit()
        context.getSharedPreferences("a_prefs", Context.MODE_PRIVATE).edit().putString("k", "v").commit()
        context.getSharedPreferences("m_prefs", Context.MODE_PRIVATE).edit().putString("k", "v").commit()

        val files = reader.getAllFiles()
        assertEquals(files, files.sorted())
    }

    @Test
    fun `getAll returns all entries for a file`() {
        context.getSharedPreferences("test", Context.MODE_PRIVATE).edit()
            .putString("name", "droidkit")
            .putInt("count", 5)
            .putBoolean("active", true)
            .commit()

        val entries = reader.getAll("test")
        assertEquals("droidkit", entries["name"])
        assertEquals(5, entries["count"])
        assertEquals(true, entries["active"])
    }

    @Test
    fun `getAll returns sorted map`() {
        context.getSharedPreferences("test", Context.MODE_PRIVATE).edit()
            .putString("z_key", "v")
            .putString("a_key", "v")
            .putString("m_key", "v")
            .commit()

        val keys = reader.getAll("test").keys.toList()
        assertEquals(keys, keys.sorted())
    }

    @Test
    fun `getAll returns empty map for nonexistent file`() {
        val entries = reader.getAll("nonexistent")
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `setValue writes boolean`() {
        reader.setValue("test", "flag", true)
        assertEquals(true, context.getSharedPreferences("test", Context.MODE_PRIVATE).getBoolean("flag", false))
    }

    @Test
    fun `setValue writes int`() {
        reader.setValue("test", "count", 42)
        assertEquals(42, context.getSharedPreferences("test", Context.MODE_PRIVATE).getInt("count", 0))
    }

    @Test
    fun `setValue writes long`() {
        reader.setValue("test", "ts", 1234567890L)
        assertEquals(1234567890L, context.getSharedPreferences("test", Context.MODE_PRIVATE).getLong("ts", 0))
    }

    @Test
    fun `setValue writes float`() {
        reader.setValue("test", "ratio", 0.5f)
        assertEquals(0.5f, context.getSharedPreferences("test", Context.MODE_PRIVATE).getFloat("ratio", 0f))
    }

    @Test
    fun `setValue writes string`() {
        reader.setValue("test", "name", "hello")
        assertEquals("hello", context.getSharedPreferences("test", Context.MODE_PRIVATE).getString("name", null))
    }

    @Test
    fun `setValue with null removes key`() {
        context.getSharedPreferences("test", Context.MODE_PRIVATE).edit().putString("name", "hello").commit()
        reader.setValue("test", "name", null)
        assertFalse(context.getSharedPreferences("test", Context.MODE_PRIVATE).contains("name"))
    }

    @Test
    fun `deleteKey removes key from prefs`() {
        context.getSharedPreferences("test", Context.MODE_PRIVATE).edit()
            .putString("keep", "a")
            .putString("remove", "b")
            .commit()

        reader.deleteKey("test", "remove")

        val prefs = context.getSharedPreferences("test", Context.MODE_PRIVATE)
        assertTrue(prefs.contains("keep"))
        assertFalse(prefs.contains("remove"))
    }
}
