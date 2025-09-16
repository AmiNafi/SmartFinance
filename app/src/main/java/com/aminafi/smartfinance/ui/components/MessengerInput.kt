package com.aminafi.smartfinance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAddManual: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add button (outside the text field)
            IconButton(
                onClick = onAddManual,
                modifier = Modifier.size(40.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Message input field with send button inside
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Add transaction or ask AI...") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("➤", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    }
}
