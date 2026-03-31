package com.prashant.droidkit.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prashant.droidkit.ui.theme.DroidKitColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StorageInspectorScreen(
    viewModel: StorageViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val prefsState by viewModel.prefsState.collectAsState()
    val dbState by viewModel.dbState.collectAsState()

    var editingEntry by remember { mutableStateOf<Triple<String, String, Any?>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Inspector", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pill toggle tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    label = { Text("SharedPreferences") },
                    leadingIcon = {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DroidKitColors.StorageBlue.copy(alpha = 0.2f),
                        selectedLabelColor = DroidKitColors.StorageBlue,
                        selectedLeadingIconColor = DroidKitColors.StorageBlue
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    label = { Text("Room DB") },
                    leadingIcon = {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DroidKitColors.StorageBlue.copy(alpha = 0.2f),
                        selectedLabelColor = DroidKitColors.StorageBlue,
                        selectedLeadingIconColor = DroidKitColors.StorageBlue
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            when (selectedTab) {
                0 -> SharedPrefsTab(
                    state = prefsState,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onEntryClick = { file, key, value -> editingEntry = Triple(file, key, value) }
                )
                1 -> RoomDbTab(
                    state = dbState,
                    onTableClick = { db, table -> viewModel.selectTable(db, table) },
                    onBackFromTable = { viewModel.closeTable() }
                )
            }
        }
    }

    // Edit sheet
    editingEntry?.let { (file, key, value) ->
        EditValueSheet(
            fileName = file,
            key = key,
            currentValue = value,
            onSave = { newValue ->
                viewModel.updatePrefValue(file, key, newValue)
                editingEntry = null
            },
            onDelete = {
                viewModel.deletePrefKey(file, key)
                editingEntry = null
            },
            onDismiss = { editingEntry = null }
        )
    }
}

@Composable
private fun SharedPrefsTab(
    state: StorageViewModel.PrefsState,
    onSearchQueryChange: (String) -> Unit,
    onEntryClick: (file: String, key: String, value: Any?) -> Unit
) {
    Column {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Search keys...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.files.isEmpty()) {
            EmptyState(
                title = "No SharedPreferences files found",
                subtitle = "This app hasn't written any SharedPreferences yet, or files are stored in a non-standard location."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                state.entries.forEach { (file, entries) ->
                    val filtered = if (state.searchQuery.isBlank()) entries
                    else entries.filter { it.key.contains(state.searchQuery, ignoreCase = true) }

                    if (filtered.isNotEmpty()) {
                        item {
                            Text(
                                text = file,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(filtered.entries.toList()) { (key, value) ->
                            PrefEntryRow(
                                key = key,
                                value = value,
                                onClick = { onEntryClick(file, key, value) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefEntryRow(key: String, value: Any?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(DroidKitColors.StorageBlue)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = key,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value?.toString()?.take(40) ?: "null",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TypeBadge(value)
    }
}

@Composable
private fun TypeBadge(value: Any?) {
    val (label, color) = when (value) {
        is Boolean -> if (value) "true" to Color(0xFF4CAF50) else "false" to Color(0xFFF44336)
        is String -> "String" to Color.Gray
        is Int -> "Int" to DroidKitColors.NotifAmber
        is Float -> "Float" to DroidKitColors.StorageBlue
        is Long -> "Long" to Color(0xFF9C27B0)
        else -> "?" to Color.Gray
    }
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun RoomDbTab(
    state: StorageViewModel.DbState,
    onTableClick: (db: String, table: String) -> Unit,
    onBackFromTable: () -> Unit
) {
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Table viewer mode
    if (state.selectedTable != null && state.selectedDb != null) {
        TableViewer(
            dbName = state.selectedDb,
            tableName = state.selectedTable,
            columns = state.tableColumns,
            rows = state.tableRows,
            error = state.tableError,
            onBack = onBackFromTable
        )
        return
    }

    if (state.databases.isEmpty()) {
        EmptyState(
            title = "No databases found",
            subtitle = "This app doesn't use SQLite/Room databases, or databases haven't been created yet."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
    ) {
        state.databases.forEach { dbName ->
            item {
                Text(
                    text = dbName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            val tables = state.tables[dbName] ?: emptyList()
            if (tables.isEmpty()) {
                item {
                    Text(
                        "No tables",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                items(tables) { table ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onTableClick(dbName, table.name) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                table.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${table.rowCount} rows · ${table.columnCount} cols",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableViewer(
    dbName: String,
    tableName: String,
    columns: List<String>,
    rows: List<Map<String, String?>>,
    error: String?,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Column {
                Text(tableName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(dbName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (error != null) {
            EmptyState(title = "Error", subtitle = error)
            return
        }

        if (rows.isEmpty()) {
            EmptyState(title = "Empty table", subtitle = "No rows in $tableName")
            return
        }

        // Scrollable table
        val scrollState = rememberScrollState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // Header row
            item {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    columns.forEach { col ->
                        Text(
                            text = col,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(120.dp),
                            color = DroidKitColors.StorageBlue
                        )
                    }
                }
            }
            // Data rows
            items(rows) { row ->
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    columns.forEach { col ->
                        Text(
                            text = row[col] ?: "NULL",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(120.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (row[col] == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            lineHeight = 18.sp
        )
    }
}
