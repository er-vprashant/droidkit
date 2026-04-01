package com.prashant.droidkit.core.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DbInspectorTest {

    private lateinit var context: Context
    private lateinit var inspector: DbInspector
    private lateinit var helper: TestDbHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inspector = DbInspector(context)
        helper = TestDbHelper(context)
        helper.writableDatabase // trigger creation
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(TestDbHelper.DB_NAME)
    }

    @Test
    fun `getAllDatabases lists created database`() {
        val dbs = inspector.getAllDatabases()
        assertTrue(dbs.contains(TestDbHelper.DB_NAME))
    }

    @Test
    fun `getAllDatabases filters journal files`() {
        val dbs = inspector.getAllDatabases()
        assertFalse(dbs.any { it.endsWith("-journal") || it.endsWith("-wal") || it.endsWith("-shm") })
    }

    @Test
    fun `getAllDatabases returns sorted list`() {
        val dbs = inspector.getAllDatabases()
        assertEquals(dbs, dbs.sorted())
    }

    @Test
    fun `getTables returns user tables only`() {
        val tables = inspector.getTables(TestDbHelper.DB_NAME)
        val names = tables.map { it.name }
        assertTrue(names.contains("users"))
        assertTrue(names.contains("orders"))
        assertFalse(names.any { it.startsWith("sqlite_") || it.startsWith("android_") })
    }

    @Test
    fun `getTables returns correct row and column counts`() {
        seedData()
        val tables = inspector.getTables(TestDbHelper.DB_NAME)
        val users = tables.first { it.name == "users" }
        assertEquals(3, users.columnCount) // id, name, email
        assertEquals(2, users.rowCount)
    }

    @Test
    fun `getTables returns empty for nonexistent db`() {
        val tables = inspector.getTables("nonexistent.db")
        assertTrue(tables.isEmpty())
    }

    @Test
    fun `getColumnNames returns column names for table`() {
        val cols = inspector.getColumnNames(TestDbHelper.DB_NAME, "users")
        assertEquals(listOf("id", "name", "email"), cols)
    }

    @Test
    fun `getColumnNames returns empty for nonexistent db`() {
        val cols = inspector.getColumnNames("nonexistent.db", "users")
        assertTrue(cols.isEmpty())
    }

    @Test
    fun `query returns rows as maps`() {
        seedData()
        val rows = inspector.query(TestDbHelper.DB_NAME, "users")
        assertEquals(2, rows.size)
        assertEquals("Alice", rows[0]["name"])
        assertEquals("Bob", rows[1]["name"])
    }

    @Test
    fun `query respects limit and offset`() {
        seedData()
        val rows = inspector.query(TestDbHelper.DB_NAME, "users", limit = 1, offset = 1)
        assertEquals(1, rows.size)
        assertEquals("Bob", rows[0]["name"])
    }

    @Test
    fun `query returns empty for nonexistent db`() {
        val rows = inspector.query("nonexistent.db", "users")
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `query handles null values`() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO users (name, email) VALUES ('Charlie', NULL)")
        db.close()

        val rows = inspector.query(TestDbHelper.DB_NAME, "users")
        val charlie = rows.first { it["name"] == "Charlie" }
        assertNull(charlie["email"])
    }

    private fun seedData() {
        val db = helper.writableDatabase
        db.insert("users", null, ContentValues().apply {
            put("name", "Alice")
            put("email", "alice@test.com")
        })
        db.insert("users", null, ContentValues().apply {
            put("name", "Bob")
            put("email", "bob@test.com")
        })
        db.close()
    }

    class TestDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {
        companion object {
            const val DB_NAME = "test_app.db"
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT)")
            db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, total REAL)")
        }

        override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {}
    }
}
