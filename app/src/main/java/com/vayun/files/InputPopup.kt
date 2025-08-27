package com.vayun.files

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun showInputPopup(title: String, label: String, default: String? = null, onResult: (String) -> Unit): () -> Unit {
    var isShown by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(default ?: "") }

    if (isShown) {
        AlertDialog(
            onDismissRequest = { isShown = false },
            title = { Text(title) },
            text = {
                Column {
                    TextField(
                        label = {Text(label)},
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Enter text") }
                    )
                }

            },
            confirmButton = {
                TextButton(onClick = {
                    onResult(text)
                    isShown = false
                    text = ""
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isShown = false
                    text = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    return { isShown = true }
}
