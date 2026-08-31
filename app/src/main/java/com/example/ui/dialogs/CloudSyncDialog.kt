package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.CloudSyncInfo
import com.example.data.sync.SyncStatus
import com.example.ui.AccessibilitySettings
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalBorderActive
import com.example.ui.theme.MinimalError
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalPrimaryContainer
import com.example.ui.theme.MinimalSuccess
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceElevated
import com.example.ui.theme.MinimalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncDialog(
    cloudSyncInfo: CloudSyncInfo,
    accessibilitySettings: AccessibilitySettings,
    onSyncToCloud: () -> Unit,
    onRestoreFromCloud: (String, Boolean) -> Unit,
    onUpdateSyncCode: (String) -> Unit,
    onUpdateAccessibility: (AccessibilitySettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var inputSyncCode by remember { mutableStateOf(cloudSyncInfo.cloudSyncCode) }
    var overwriteExisting by remember { mutableStateOf(false) }

    val clipboard = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MinimalSurface,
        scrimColor = Color(0x66000000),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MinimalBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cloud Sync & Accessibility",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cross-device sync • Backup • Accessibility options",
                        color = MinimalPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_cloud_sync_dialog")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloud Sync Section Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MinimalPrimary, modifier = Modifier.size(20.dp))
                            Text("Soundboard Cloud Sync", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (cloudSyncInfo.status) {
                                        SyncStatus.SYNCED -> MinimalSuccess.copy(alpha = 0.12f)
                                        SyncStatus.SYNCING -> MinimalPrimary.copy(alpha = 0.12f)
                                        SyncStatus.ERROR -> MinimalError.copy(alpha = 0.12f)
                                        else -> MinimalSurfaceVariant
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (cloudSyncInfo.status) {
                                    SyncStatus.SYNCED -> "SYNCED"
                                    SyncStatus.SYNCING -> "SYNCING..."
                                    SyncStatus.ERROR -> "ERROR"
                                    else -> "READY"
                                },
                                color = when (cloudSyncInfo.status) {
                                    SyncStatus.SYNCED -> MinimalSuccess
                                    SyncStatus.SYNCING -> MinimalPrimary
                                    SyncStatus.ERROR -> MinimalError
                                    else -> TextSecondary
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Your unique Sync Passcode connects your sound clips, custom recordings, and categories securely across all your devices.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Code Input Field with Copy button
                    OutlinedTextField(
                        value = inputSyncCode,
                        onValueChange = {
                            inputSyncCode = it
                            onUpdateSyncCode(it)
                        },
                        label = { Text("Cloud Sync Code", color = TextSecondary) },
                        trailingIcon = {
                            IconButton(onClick = {
                                val clip = ClipData.newPlainText("Soundboard Sync Code", inputSyncCode)
                                clipboard?.setPrimaryClip(clip)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = MinimalPrimary, modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MinimalPrimary,
                            unfocusedBorderColor = MinimalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = MinimalSurfaceVariant,
                            unfocusedContainerColor = MinimalSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_sync_code_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons (Upload to Cloud, Restore from Cloud)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncToCloud,
                            colors = ButtonDefaults.buttonColors(containerColor = MinimalPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_to_cloud_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Upload Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { onRestoreFromCloud(inputSyncCode, overwriteExisting) },
                            colors = ButtonDefaults.buttonColors(containerColor = MinimalSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_from_cloud_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Text("Restore Sync", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Message
                    Text(
                        text = cloudSyncInfo.statusMessage,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accessibility & Display Section
            Text("Accessibility & Visual Preferences", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // High Contrast Mode Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("High-Contrast Tactile Pads", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Sharp bold outlines & high-visibility borders", color = TextSecondary, fontSize = 10.sp)
                    }

                    Switch(
                        checked = accessibilitySettings.highContrast,
                        onCheckedChange = {
                            onUpdateAccessibility(accessibilitySettings.copy(highContrast = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MinimalPrimary,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = MinimalBorder
                        ),
                        modifier = Modifier.testTag("high_contrast_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Haptic Feedback Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MinimalPrimary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Tactile Haptic Feedback", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Vibrate device on sound trigger & loop toggle", color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    Switch(
                        checked = accessibilitySettings.hapticFeedback,
                        onCheckedChange = {
                            onUpdateAccessibility(accessibilitySettings.copy(hapticFeedback = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MinimalPrimary,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = MinimalBorder
                        ),
                        modifier = Modifier.testTag("haptic_feedback_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pad Layout Grid Size Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text("Sound Pad Layout Density", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(2, "2 Columns", "Large Tactile"),
                            Triple(3, "3 Columns", "Standard"),
                            Triple(4, "4 Columns", "Compact")
                        ).forEach { (cols, label, sub) ->
                            val isSelected = accessibilitySettings.gridColumns == cols
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MinimalPrimary.copy(alpha = 0.12f) else MinimalSurfaceVariant)
                                    .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MinimalPrimary else MinimalBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onUpdateAccessibility(accessibilitySettings.copy(gridColumns = cols))
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        label,
                                        color = if (isSelected) MinimalPrimary else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        sub,
                                        color = if (isSelected) MinimalPrimary else TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
