package com.prashant.droidkit.ui.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prashant.droidkit.ui.theme.DroidKitColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditValueSheet(
    fileName: String,
    key: String,
    currentValue: Any?,
    onSave: (Any?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editedValue by remember { mutableStateOf(currentValue) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = key,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "File: $fileName · Type: ${typeLabel(currentValue)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Type-appropriate editor
            when (currentValue) {
                is Boolean -> {
                    var checked by remember { mutableStateOf(currentValue) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Value", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = checked,
                            onCheckedChange = {
                                checked = it
                                editedValue = it
                            }
                        )
                    }
                }
                is Int -> {
                    var text by remember { mutableStateOf(currentValue.toString()) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            editedValue = it.toIntOrNull() ?: currentValue
                        },
                        label = { Text("Value (Int)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                is Long -> {
                    var text by remember { mutableStateOf(currentValue.toString()) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            editedValue = it.toLongOrNull() ?: currentValue
                        },
                        label = { Text("Value (Long)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                is Float -> {
                    var text by remember { mutableStateOf(currentValue.toString()) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            editedValue = it.toFloatOrNull() ?: currentValue
                        },
                        label = { Text("Value (Float)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                else -> {
                    var text by remember { mutableStateOf(currentValue?.toString() ?: "") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            editedValue = it
                        },
                        label = { Text("Value (String)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { showSaveConfirm = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DroidKitColors.ErrorRed
                )
            ) {
                Text("Delete key")
            }
        }
    }

    // Save confirmation dialog
    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text("Update value") },
            text = { Text("Update value for \"$key\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveConfirm = false
                    onSave(editedValue)
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete key") },
            text = { Text("Delete \"$key\" from $fileName? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = DroidKitColors.ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun typeLabel(value: Any?): String = when (value) {
    is Boolean -> "Boolean"
    is Int -> "Int"
    is Long -> "Long"
    is Float -> "Float"
    is String -> "String"
    else -> "Unknown"
}
