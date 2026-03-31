package com.prashant.droidkit.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prashant.droidkit.core.storage.DbInspector
import com.prashant.droidkit.core.storage.PrefsReader
import com.prashant.droidkit.internal.DroidKitServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class StorageViewModel : ViewModel() {

    private val prefsReader: PrefsReader = DroidKitServiceLocator.prefsReader
    private val dbInspector: DbInspector = DroidKitServiceLocator.dbInspector

    // SharedPreferences state
    data class PrefsState(
        val files: List<String> = emptyList(),
        val entries: Map<String, Map<String, Any?>> = emptyMap(),
        val isLoading: Boolean = true,
        val searchQuery: String = ""
    )

    private val _prefsState = MutableStateFlow(PrefsState())
    val prefsState: StateFlow<PrefsState> = _prefsState.asStateFlow()

    // Database state
    data class DbState(
        val databases: List<String> = emptyList(),
        val tables: Map<String, List<DbInspector.TableInfo>> = emptyMap(),
        val isLoading: Boolean = true,
        val selectedDb: String? = null,
        val selectedTable: String? = null,
        val tableColumns: List<String> = emptyList(),
        val tableRows: List<Map<String, String?>> = emptyList(),
        val tableError: String? = null
    )

    private val _dbState = MutableStateFlow(DbState())
    val dbState: StateFlow<DbState> = _dbState.asStateFlow()

    // Tab state
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadPrefs()
        loadDatabases()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _prefsState.value = _prefsState.value.copy(searchQuery = query)
    }

    fun loadPrefs() {
        viewModelScope.launch(Dispatchers.IO) {
            _prefsState.value = _prefsState.value.copy(isLoading = true)
            try {
                val files = prefsReader.getAllFiles()
                val entries = files.associateWith { prefsReader.getAll(it) }
                _prefsState.value = PrefsState(
                    files = files,
                    entries = entries,
                    isLoading = false,
                    searchQuery = _prefsState.value.searchQuery
                )
            } catch (e: Exception) {
                _prefsState.value = _prefsState.value.copy(isLoading = false)
            }
        }
    }

    fun updatePrefValue(fileName: String, key: String, value: Any?) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsReader.setValue(fileName, key, value)
            loadPrefs()
        }
    }

    fun deletePrefKey(fileName: String, key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsReader.deleteKey(fileName, key)
            loadPrefs()
        }
    }

    fun loadDatabases() {
        viewModelScope.launch(Dispatchers.IO) {
            _dbState.value = _dbState.value.copy(isLoading = true)
            try {
                val dbs = dbInspector.getAllDatabases()
                val tables = dbs.associateWith { dbInspector.getTables(it) }
                _dbState.value = DbState(
                    databases = dbs,
                    tables = tables,
                    isLoading = false
                )
            } catch (e: Exception) {
                _dbState.value = _dbState.value.copy(isLoading = false)
            }
        }
    }

    fun selectTable(dbName: String, tableName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _dbState.value = _dbState.value.copy(
                selectedDb = dbName,
                selectedTable = tableName,
                tableError = null
            )
            try {
                val columns = dbInspector.getColumnNames(dbName, tableName)
                val rows = dbInspector.query(dbName, tableName)
                _dbState.value = _dbState.value.copy(
                    tableColumns = columns,
                    tableRows = rows
                )
            } catch (e: Exception) {
                _dbState.value = _dbState.value.copy(
                    tableError = "Cannot open database: ${e.message}"
                )
            }
        }
    }

    fun closeTable() {
        _dbState.value = _dbState.value.copy(
            selectedDb = null,
            selectedTable = null,
            tableColumns = emptyList(),
            tableRows = emptyList(),
            tableError = null
        )
    }
}
