package com.prashant.droidkit.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.prashant.droidkit.core.network.NetworkCall
import com.prashant.droidkit.core.network.NetworkCallRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkDetailScreen(
    navController: NavController,
    callId: String
) {
    val call = NetworkCallRepository.getById(callId)
    var selectedTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    if (call == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Network Detail") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text("Call not found", modifier = Modifier.padding(16.dp))
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${call.method} ${call.responseStatus ?: "ERR"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val curl = generateCurl(call)
                        clipboardManager.setText(AnnotatedString(curl))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy as cURL")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mock this call") },
                            onClick = {
                                navController.navigate("network/mocks/new?from=$callId")
                                showMenu = false
                            }
                        )
                    }
                },
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Request") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Response") }
                )
            }

            when (selectedTab) {
                0 -> OverviewTab(call)
                1 -> RequestTab(call)
                2 -> ResponseTab(call)
            }
        }
    }
}

@Composable
private fun OverviewTab(call: NetworkCall) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DetailField("URL", call.url)
        Spacer(modifier = Modifier.height(12.dp))
        DetailField("Method", call.method)
        Spacer(modifier = Modifier.height(12.dp))
        DetailField("Status", call.responseStatus?.toString() ?: "Error")
        Spacer(modifier = Modifier.height(12.dp))
        DetailField("Message", call.responseMessage ?: call.error ?: "-")
        Spacer(modifier = Modifier.height(12.dp))
        DetailField("Duration", call.duration?.let { "${it}ms" } ?: "-")
        Spacer(modifier = Modifier.height(12.dp))
        DetailField(
            "Time",
            SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                .format(Date(call.timestamp))
        )
        if (call.isMocked) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailField("Type", "MOCKED RESPONSE")
        }
    }
}

@Composable
private fun RequestTab(call: NetworkCall) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Headers",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (call.requestHeaders.isEmpty()) {
            Text("None", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            call.requestHeaders.forEach { (key, value) ->
                HeaderItem(key, value)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Body",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (call.requestBody.isNullOrEmpty()) {
            Text("Empty", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CodeBlock(call.requestBody)
        }
    }
}

@Composable
private fun ResponseTab(call: NetworkCall) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Headers",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (call.responseHeaders.isNullOrEmpty()) {
            Text("None", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            call.responseHeaders.forEach { (key, value) ->
                HeaderItem(key, value)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Body",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (call.responseBody.isNullOrEmpty()) {
            Text(
                call.error ?: "Empty",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CodeBlock(call.responseBody)
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun HeaderItem(key: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$key: ",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Text(
        text = code,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
    )
}

private fun generateCurl(call: NetworkCall): String {
    val sb = StringBuilder("curl -X ${call.method}")
    call.requestHeaders.forEach { (key, value) ->
        sb.append(" \\\n  -H \"$key: $value\"")
    }
    if (call.requestBody != null) {
        sb.append(" \\\n  -d '${call.requestBody}'")
    }
    sb.append(" \\\n  \"${call.url}\"")
    return sb.toString()
}

@Composable
private fun Row(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.Row(modifier = modifier, content = content)
}
