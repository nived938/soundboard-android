package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibleForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.ui.BoardThemePalette
import com.example.ui.SoundSortMode
import com.example.data.sync.CloudSyncInfo
import com.example.data.sync.SyncStatus
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

@Composable
fun TopSoundboardBar(
    activePlayingCount: Int,
    masterVolume: Float,
    cloudSyncInfo: CloudSyncInfo,
    searchQuery: String,
    sortMode: SoundSortMode,
    boardTheme: BoardThemePalette,
    onSearchQueryChange: (String) -> Unit,
    onSortModeChange: (SoundSortMode) -> Unit,
    onThemeChange: (BoardThemePalette) -> Unit,
    onStopAllClick: () -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onCloudSyncClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onOpenQuickRecord: () -> Unit,
    onOpenMixer: () -> Unit,
    onOpenSequencer: () -> Unit,
    onOpenTts: () -> Unit,
    onOpenPacks: () -> Unit,
    onOpenMetronome: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var isVolumeDialogExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var isThemeMenuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearchExpanded = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MinimalSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding & Clean Visualizer Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MinimalPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Soundboard Logo",
                        tint = MinimalPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Soundboard",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = "Minimal Studio • Cloud Sync",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Top Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stop All Emergency Button
                if (activePlayingCount > 0) {
                    Box(
                        modifier = Modifier
                            .testTag("stop_all_button")
                            .semantics {
                                contentDescription =
                                    "Stop all $activePlayingCount active playing sounds"
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(MinimalError)
                            .clickable { onStopAllClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "STOP ($activePlayingCount)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Volume Popup Button
                IconButton(
                    onClick = { isVolumeDialogExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("master_volume_button")
                        .clip(CircleShape)
                        .background(MinimalSurfaceVariant)
                ) {
                    Icon(
                        imageVector = when {
                            masterVolume <= 0f -> Icons.Default.VolumeMute
                            masterVolume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = "Master Volume ${(masterVolume * 100).toInt()}%",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 10s Voice Record Mic Action
                IconButton(
                    onClick = onOpenQuickRecord,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("quick_record_header_button")
                        .clip(CircleShape)
                        .background(MinimalPrimary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record 10s voice clip",
                        tint = MinimalPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Search Toggle Button
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) onSearchQueryChange("")
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("search_toggle_button")
                        .clip(CircleShape)
                        .background(if (isSearchExpanded) MinimalPrimary.copy(alpha = 0.12f) else MinimalSurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = if (isSearchExpanded) "Close search" else "Search sounds",
                        tint = if (isSearchExpanded) MinimalPrimary else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Cloud Sync Button
                IconButton(
                    onClick = onCloudSyncClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cloud_sync_button")
                        .clip(CircleShape)
                        .background(
                            when (cloudSyncInfo.status) {
                                SyncStatus.SYNCED -> MinimalSuccess.copy(alpha = 0.15f)
                                SyncStatus.SYNCING -> MinimalPrimary.copy(alpha = 0.15f)
                                else -> MinimalSurfaceVariant
                            }
                        )
                ) {
                    Icon(
                        imageVector = if (cloudSyncInfo.status == SyncStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync: ${cloudSyncInfo.statusMessage}",
                        tint = when (cloudSyncInfo.status) {
                            SyncStatus.SYNCED -> MinimalSuccess
                            SyncStatus.SYNCING -> MinimalPrimary
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Accessibility & Settings Button
                IconButton(
                    onClick = onAccessibilityClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("accessibility_settings_button")
                        .clip(CircleShape)
                        .background(MinimalSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Accessibility and Display Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Studio Quick Tools Strip (Features 1, 2, 5, 6, 8, 9, 10 & Sorting)
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick 10s Voice Recorder Chip
            item {
                StudioToolChip(
                    label = "🎙️ Record (10s)",
                    icon = Icons.Default.Mic,
                    onClick = onOpenQuickRecord
                )
            }

            // Live Mixer Chip
            item {
                StudioToolChip(
                    label = "Mixer",
                    icon = Icons.Default.GraphicEq,
                    onClick = onOpenMixer
                )
            }

            // Beat Sequencer Chip
            item {
                StudioToolChip(
                    label = "Beats",
                    icon = Icons.Default.MusicNote,
                    onClick = onOpenSequencer
                )
            }

            // Voice Synth Chip
            item {
                StudioToolChip(
                    label = "Voice Pad",
                    icon = Icons.Default.RecordVoiceOver,
                    onClick = onOpenTts
                )
            }

            // Sound Packs Chip
            item {
                StudioToolChip(
                    label = "Packs",
                    icon = Icons.Default.LibraryMusic,
                    onClick = onOpenPacks
                )
            }

            // Metronome Chip
            item {
                StudioToolChip(
                    label = "BPM",
                    icon = Icons.Default.Speed,
                    onClick = onOpenMetronome
                )
            }

            // Stats Chip
            item {
                StudioToolChip(
                    label = "Analytics",
                    icon = Icons.Default.BarChart,
                    onClick = onOpenStats
                )
            }

            // Sort Selector Chip
            item {
                Box {
                    StudioToolChip(
                        label = "Sort: ${sortMode.label}",
                        icon = Icons.Default.Sort,
                        onClick = { isSortMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                        modifier = Modifier.background(MinimalSurface)
                    ) {
                        SoundSortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${mode.emoji} ${mode.label}",
                                        fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortMode == mode) MinimalPrimary else TextPrimary
                                    )
                                },
                                onClick = {
                                    onSortModeChange(mode)
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input"),
                    placeholder = { Text("Search pads by name, emoji, or audio FX...", color = TextTertiary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MinimalPrimary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
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
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                // Quick Search Tags
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTags = listOf("Voice", "Drum", "Laser", "Synth", "Applause", "Airhorn", "Robot", "Echo")
                    items(quickTags) { tag ->
                        val isCurrent = searchQuery.equals(tag, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCurrent) MinimalPrimary else MinimalSurfaceVariant)
                                .clickable {
                                    if (isCurrent) onSearchQueryChange("") else onSearchQueryChange(tag)
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = if (isCurrent) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    // Master Volume Dialog
    if (isVolumeDialogExpanded) {
        Dialog(onDismissRequest = { isVolumeDialogExpanded = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MinimalSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Master Volume",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Slider(
                            value = masterVolume,
                            onValueChange = onMasterVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f).testTag("master_volume_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MinimalPrimary,
                                activeTrackColor = MinimalPrimary,
                                inactiveTrackColor = MinimalBorder
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MinimalPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${(masterVolume * 100).toInt()}%",
                        color = MinimalPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MinimalPrimary)
                            .clickable { isVolumeDialogExpanded = false }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MinimalSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MinimalPrimary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
