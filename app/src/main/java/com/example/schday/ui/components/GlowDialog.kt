package com.example.schday.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GlowDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    properties: DialogProperties = DialogProperties(),
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    neutralButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title Area
                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                    ProvideTextStyle(value = MaterialTheme.typography.titleLarge) {
                        title()
                    }
                }

                // Content Area
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(bottom = 24.dp)
                ) {
                    content()
                }

                // Buttons Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (neutralButton != null) {
                        neutralButton()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}
