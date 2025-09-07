package com.aminafi.smartfinance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerInput(
    onSendMessage: (String) -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var message by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Type your transaction...") },
            modifier = Modifier.weight(1f),
            enabled = !isProcessing,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        Button(
            onClick = {
                if (message.text.isNotBlank() && !isProcessing) {
                    scope.launch {
                        onSendMessage(message.text)
                        message = TextFieldValue("")
                    }
                }
            },
            enabled = message.text.isNotBlank() && !isProcessing,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(56.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send")
            }
        }
    }
}
