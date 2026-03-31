package com.prashant.droidkit.core.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase

internal class DbInspector(private val context: Context) {

    data class TableInfo(
        val name: String,
        val rowCount: Int,
        val columnCount: Int
    )

    fun getAllDatabases(): List<String> {
        return context.databaseList()
            .filter { name ->
                !name.endsWith("-journal") &&
                !name.endsWith("-wal") &&
                !name.endsWith("-shm")
            }
            .sorted()
    }

    fun getTables(dbName: String): List<TableInfo> {
        val db = openDatabase(dbName) ?: return emptyList()
        return db.use { database ->
            val tables = mutableListOf<TableInfo>()
            val cursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT LIKE 'android_%' " +
                "AND name NOT LIKE 'room_%'",
                null
            )
            while (cursor.moveToNext()) {
                val tableName = cursor.getString(0)
                val rowCount = getRowCount(database, tableName)
                val colCount = getColumnCount(database, tableName)
                tables.add(TableInfo(tableName, rowCount, colCount))
            }
            cursor.close()
            tables.sortedBy { it.name }
        }
    }

    fun getColumnNames(dbName: String, tableName: String): List<String> {
        val db = openDatabase(dbName) ?: return emptyList()
        return db.use { database ->
            val cursor = database.rawQuery("PRAGMA table_info($tableName)", null)
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            cursor.close()
            columns
        }
    }

    fun query(dbName: String, tableName: String, limit: Int = 50, offset: Int = 0): List<Map<String, String?>> {
        val db = openDatabase(dbName) ?: return emptyList()
        return db.use { database ->
            val cursor = database.rawQuery(
                "SELECT * FROM $tableName LIMIT $limit OFFSET $offset",
                null
            )
            val columns = cursor.columnNames.toList()
            val rows = mutableListOf<Map<String, String?>>()
            while (cursor.moveToNext()) {
                val row = columns.associateWith { col ->
                    val idx = cursor.getColumnIndexOrThrow(col)
                    if (cursor.isNull(idx)) null else cursor.getString(idx)
                }
                rows.add(row)
            }
            cursor.close()
            rows
        }
    }

    private fun getRowCount(db: SQLiteDatabase, tableName: String): Int {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    private fun getColumnCount(db: SQLiteDatabase, tableName: String): Int {
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        val count = cursor.count
        cursor.close()
        return count
    }

    private fun openDatabase(name: String): SQLiteDatabase? {
        return try {
            val path = context.getDatabasePath(name)
            if (!path.exists()) return null
            SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            null
        }
    }
}
