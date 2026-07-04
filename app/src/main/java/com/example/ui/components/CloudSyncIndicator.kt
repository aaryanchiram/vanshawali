package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CloudSyncIndicator(
    isSyncing: Boolean,
    lastSyncedDate: String,
    onTriggerSync: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        if (isSyncing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("सिंक हो रहा है...", fontSize = 12.sp)
        } else {
            IconButton(onClick = onTriggerSync) {
                Icon(
                    imageVector = if (lastSyncedDate == "अभी-अभी (Just Now)") Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                    contentDescription = "Cloud Cloud Sync",
                    tint = if (lastSyncedDate == "अभी-अभी (Just Now)") Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
