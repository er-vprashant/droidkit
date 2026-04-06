package com.prashant.droidkit.ui.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prashant.droidkit.core.network.MockResponse
import com.prashant.droidkit.core.network.NetworkCallRepository
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MockEditorScreen(
    navController: NavController,
    mockId: String?,
    fromCallId: String?,
    viewModel: MockEditorViewModel = viewModel()
) {
    var urlPattern by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("GET") }
    var statusCode by remember { mutableStateOf("200") }
    var responseBody by remember { mutableStateOf("") }
    var delayMs by remember { mutableStateOf("0") }
    var methodExpanded by remember { mutableStateOf(false) }

    val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")

    LaunchedEffect(mockId, fromCallId) {
        if (mockId != null) {
            viewModel.loadMock(mockId)?.let { mock ->
                urlPattern = mock.urlPattern
                method = mock.method
                statusCode = mock.statusCode.toString()
                responseBody = mock.responseBody
                delayMs = mock.delayMs.toString()
            }
        } else if (fromCallId != null) {
            NetworkCallRepository.getById(fromCallId)?.let { call ->
                urlPattern = call.url
                method = call.method
                statusCode = call.responseStatus?.toString() ?: "200"
                responseBody = call.responseBody ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mockId != null) "Edit Mock" else "New Mock") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val mock = MockResponse(
                                id = mockId ?: UUID.randomUUID().toString(),
                                urlPattern = urlPattern,
                                method = method,
                                matchType = if (urlPattern.contains("*")) 
                                    MockResponse.MatchType.WILDCARD 
                                else 
                                    MockResponse.MatchType.EXACT,
                                enabled = true,
                                statusCode = statusCode.toIntOrNull() ?: 200,
                                responseBody = responseBody,
                                headers = emptyMap(),
                                delayMs = delayMs.toLongOrNull() ?: 0
                            )
                            viewModel.saveMock(mock, isNew = mockId == null)
                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = urlPattern,
                onValueChange = { urlPattern = it },
                label = { Text("URL Pattern") },
                placeholder = { Text("https://api.example.com/users/*") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Use * for wildcards") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = it }
            ) {
                OutlinedTextField(
                    value = method,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Method") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = methodExpanded,
                    onDismissRequest = { methodExpanded = false }
                ) {
                    methods.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                method = m
                                methodExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = statusCode,
                onValueChange = { statusCode = it },
                label = { Text("Status Code") },
                placeholder = { Text("200") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = responseBody,
                onValueChange = { responseBody = it },
                label = { Text("Response Body") },
                placeholder = { Text("{\"message\": \"success\"}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = delayMs,
                onValueChange = { delayMs = it },
                label = { Text("Delay (ms)") },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Simulate slow network") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
