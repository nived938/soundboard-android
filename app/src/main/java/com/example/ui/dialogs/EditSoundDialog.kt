package com.example.ui.dialogs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioImportHelper
import com.example.audio.AudioRecorderHelper
import com.example.audio.SoundPlaybackManager
import com.example.audio.WavAudioGenerator
import com.example.data.model.CategoryEntity
import com.example.data.model.SoundClipEntity
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalBorderActive
import com.example.ui.theme.MinimalError
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalPrimaryContainer
import com.example.ui.theme.MinimalSuccess
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceElevated
import com.example.ui.theme.MinimalSurfaceVariant
import com.example.ui.theme.PadColorSwatches
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.io.File

private val POPULAR_EMOJIS = listOf(
    "🔊", "📢", "💥", "🥁", "🎸", "🎧", "🎮", "🪙", "🏆", "💀",
    "👏", "🚨", "🔔", "🪀", "🎺", "🔥", "⚡", "✨", "🎵", "👾",
    "🤖", "🐱", "🐶", "🚀", "💣", "🎉", "👑", "🍕", "⚽", "🚗"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSoundDialog(
    soundToEdit: SoundClipEntity?,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    recorderHelper: AudioRecorderHelper,
    playbackManager: SoundPlaybackManager,
    onSaveSound: (SoundClipEntity) -> Unit,
    onDeleteSound: ((SoundClipEntity) -> Unit)?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val recordingState by recorderHelper.recordingState.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf(soundToEdit?.title ?: "New Sound") }
    var emoji by remember { mutableStateOf(soundToEdit?.emoji ?: "🔊") }
    var colorHex by remember { mutableStateOf(soundToEdit?.colorHex ?: "#6750A4") }
    var categoryId by remember {
        mutableStateOf(
            soundToEdit?.categoryId
                ?: selectedCategoryId
                ?: categories.firstOrNull()?.id
                ?: 1L
        )
    }
    var audioFilePath by remember { mutableStateOf(soundToEdit?.audioFilePath ?: "") }
    var presetKey by remember { mutableStateOf(soundToEdit?.presetKey) }
    var durationMs by remember { mutableLongStateOf(soundToEdit?.durationMs ?: 1500L) }
    var isLooping by remember { mutableStateOf(soundToEdit?.isLooping ?: false) }
    var volume by remember { mutableFloatStateOf(soundToEdit?.volume ?: 1.0f) }
    var pitch by remember { mutableFloatStateOf(soundToEdit?.pitch ?: 1.0f) }
    var playbackSpeed by remember { mutableFloatStateOf(soundToEdit?.playbackSpeed ?: 1.0f) }
    var trimStartMs by remember { mutableLongStateOf(soundToEdit?.trimStartMs ?: 0L) }
    var trimEndMs by remember { mutableLongStateOf(soundToEdit?.trimEndMs ?: (soundToEdit?.durationMs ?: 1500L)) }
    var activeFx by remember { mutableStateOf(soundToEdit?.activeFx ?: "NONE") }

    // Update trimEndMs whenever durationMs changes
    LaunchedEffect(durationMs) {
        if (trimEndMs > durationMs || trimEndMs == 0L) {
            trimEndMs = durationMs
        }
    }

    // Source tab: 0 = Studio Presets, 1 = Record Mic, 2 = Import File
    var selectedSourceTab by remember {
        mutableStateOf(
            when {
                soundToEdit?.presetKey != null -> 0
                soundToEdit != null -> 1
                else -> 0
            }
        )
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var previewSoundEntity by remember { mutableStateOf<SoundClipEntity?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // Mic permission launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            recorderHelper.startRecording { file, dur ->
                audioFilePath = file.absolutePath
                presetKey = null
                durationMs = dur.coerceAtMost(10000L)
            }
        }
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = AudioImportHelper.importAudioFromUri(context, uri)
            if (result != null) {
                audioFilePath = result.file.absolutePath
                presetKey = null
                durationMs = result.durationMs.coerceAtMost(10000L)
                if (title == "New Sound" || title.isBlank()) {
                    title = result.originalFileName.take(20)
                }
            }
        }
    }

    // Ensure preset audio files exist for studio presets tab
    val presets = remember { WavAudioGenerator.getPresets() }

    LaunchedEffect(recordingState.isFinished, recordingState.recordedFilePath) {
        if (recordingState.isFinished && recordingState.recordedFilePath != null) {
            audioFilePath = recordingState.recordedFilePath!!
            presetKey = null
            durationMs = recordingState.recordedMs.coerceAtMost(10000L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackManager.stopSound(999999L)
            recorderHelper.reset()
        }
    }

    val selectedColor = remember(colorHex) { parseColorSafe(colorHex) }

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
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (soundToEdit == null) "Create Sound Pad" else "Customize Sound Pad",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Max 10s audio • Seamless looping option",
                        color = MinimalPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sound_dialog_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Source Selector Tabs
            TabRow(
                selectedTabIndex = selectedSourceTab,
                containerColor = MinimalSurfaceVariant,
                contentColor = MinimalPrimary,
                indicator = { tabPositions ->
                    if (selectedSourceTab < tabPositions.size) {
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSourceTab]),
                            color = MinimalPrimary,
                            height = 3.dp
                        )
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MinimalBorder, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSourceTab == 0,
                    onClick = { selectedSourceTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedSourceTab == 0) MinimalPrimary else TextSecondary)
                            Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedSourceTab == 0) MinimalPrimary else TextSecondary)
                        }
                    }
                )
                Tab(
                    selected = selectedSourceTab == 1,
                    onClick = { selectedSourceTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedSourceTab == 1) MinimalPrimary else TextSecondary)
                            Text("Record", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedSourceTab == 1) MinimalPrimary else TextSecondary)
                        }
                    }
                )
                Tab(
                    selected = selectedSourceTab == 2,
                    onClick = { selectedSourceTab = 2 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedSourceTab == 2) MinimalPrimary else TextSecondary)
                            Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedSourceTab == 2) MinimalPrimary else TextSecondary)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Tab Content
            when (selectedSourceTab) {
                0 -> {
                    // Studio Presets Grid / Selector
                    Text("Select a Studio Sound Preset", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(presets) { preset ->
                            val isPresetSelected = presetKey == preset.key
                            val presetColor = parseColorSafe(preset.colorHex)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPresetSelected) presetColor.copy(alpha = 0.12f) else MinimalSurfaceElevated)
                                    .border(if (isPresetSelected) 1.5.dp else 1.dp, if (isPresetSelected) presetColor else MinimalBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        presetKey = preset.key
                                        audioFilePath = WavAudioGenerator.ensurePresetAudioFiles(context)[preset.key] ?: ""
                                        durationMs = preset.defaultDurationMs.coerceAtMost(10000L)
                                        isLooping = preset.isDefaultLoop
                                        colorHex = preset.colorHex
                                        emoji = preset.emoji
                                        if (title == "New Sound") title = preset.title
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(preset.emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        preset.title,
                                        color = if (isPresetSelected) TextPrimary else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        "${preset.defaultDurationMs / 1000f}s",
                                        color = presetColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Microphone Recorder (Max 10s strict limit)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Microphone Clip (Max 10s)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = String.format("%.1fs / 10.0s", recordingState.recordedMs / 1000f),
                                    color = if (recordingState.isRecording) MinimalError else MinimalPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Live recording amplitude progress bar
                            LinearProgressIndicator(
                                progress = { (recordingState.recordedMs / 10000f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (recordingState.isRecording) MinimalError else MinimalPrimary,
                                trackColor = MinimalBorder
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Record / Stop Button
                            Button(
                                onClick = {
                                    if (recordingState.isRecording) {
                                        recorderHelper.stopRecording { file, dur ->
                                            audioFilePath = file.absolutePath
                                            presetKey = null
                                            durationMs = dur.coerceAtMost(10000L)
                                        }
                                    } else {
                                        if (hasMicPermission) {
                                            recorderHelper.startRecording { file, dur ->
                                                audioFilePath = file.absolutePath
                                                presetKey = null
                                                durationMs = dur.coerceAtMost(10000L)
                                            }
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (recordingState.isRecording) MinimalError else MinimalPrimary
                                ),
                                modifier = Modifier.testTag("record_mic_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (recordingState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        if (recordingState.isRecording) "Stop Recording" else "Start 10s Record",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (recordingState.isFinished) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "✓ Recorded ${String.format("%.1fs", durationMs / 1000f)} audio clip!",
                                    color = MinimalSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Audio Importer (Clamped <= 10s)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Import Audio from Device (Max 10s limit)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Audio files longer than 10 seconds are trimmed to first 10s",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MinimalPrimary),
                                modifier = Modifier.testTag("import_audio_file_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Choose Audio File", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (audioFilePath.isNotEmpty() && presetKey == null && !recordingState.isFinished) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "✓ Audio loaded: ${String.format("%.1fs", durationMs / 1000f)}",
                                    color = MinimalSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pad Properties Section
            Text("Pad Appearance & Details", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Sound Title", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sound_title_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedColor,
                    unfocusedBorderColor = MinimalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = MinimalSurfaceVariant,
                    unfocusedContainerColor = MinimalSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Dropdown Picker
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentCategory = categories.find { it.id == categoryId }
                OutlinedTextField(
                    value = "${currentCategory?.icon ?: "📁"} ${currentCategory?.name ?: "Category"}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category", color = TextSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("sound_category_picker"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor,
                        unfocusedBorderColor = MinimalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = MinimalSurfaceVariant,
                        unfocusedContainerColor = MinimalSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.background(MinimalSurface)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.icon} ${cat.name}", color = TextPrimary) },
                            onClick = {
                                categoryId = cat.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Emoji / Icon Picker
            Text("Select Pad Emoji", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(POPULAR_EMOJIS) { em ->
                    val isSelected = emoji == em
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else MinimalSurfaceVariant)
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) selectedColor else MinimalBorder, CircleShape)
                            .clickable { emoji = em },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(em, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color Swatches
            Text("Select Pad Color Theme", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(PadColorSwatches) { hex ->
                    val col = parseColorSafe(hex)
                    val isSelected = colorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) TextPrimary else Color.Transparent, CircleShape)
                            .clickable { colorHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Options (Seamless Looping & Audio Parameters)
            Text("Playback Configuration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Seamless Loop Switch
            Card(
                colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isLooping) selectedColor.copy(alpha = 0.5f) else MinimalBorder),
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
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = if (isLooping) selectedColor else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Seamless Loop Playback", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Loops endlessly without audio gaps", color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    Switch(
                        checked = isLooping,
                        onCheckedChange = { isLooping = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = selectedColor,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = MinimalBorder
                        ),
                        modifier = Modifier.testTag("loop_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pad Volume", color = TextSecondary, fontSize = 12.sp)
                Text("${(volume * 100).toInt()}%", color = selectedColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Slider(
                value = volume,
                onValueChange = { volume = it },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(thumbColor = selectedColor, activeTrackColor = selectedColor, inactiveTrackColor = MinimalBorder),
                modifier = Modifier.testTag("pad_volume_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pitch Slider (0.5x to 2.0x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pitch & Speed Shift", color = TextSecondary, fontSize = 12.sp)
                Text(String.format("%.2fx", pitch), color = selectedColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Slider(
                value = pitch,
                onValueChange = {
                    pitch = it
                    playbackSpeed = it
                },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = selectedColor, activeTrackColor = selectedColor, inactiveTrackColor = MinimalBorder),
                modifier = Modifier.testTag("pad_pitch_slider")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Audio Effects Rack (Feature 3)
            Text("Audio Effects Rack (DSP Engine)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            val fxOptions = listOf(
                "NONE" to "Normal (Clean)",
                "ECHO" to "Tape Echo 🎚️",
                "REVERB" to "Studio Reverb 🏛️",
                "BITCRUSH" to "8-Bit Bitcrush 👾",
                "ROBOT" to "Robot RingMod 🤖",
                "WARM" to "Lo-Fi Warmth ☕"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(fxOptions) { (key, label) ->
                    val isSelected = activeFx == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) selectedColor.copy(alpha = 0.15f) else MinimalSurfaceElevated)
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) selectedColor else MinimalBorder, RoundedCornerShape(10.dp))
                            .clickable { activeFx = key }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) selectedColor else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Waveform Audio Trimmer (Feature 4)
            Card(
                colors = CardDefaults.cardColors(containerColor = MinimalSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Interactive Audio Trimmer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "${trimStartMs}ms - ${trimEndMs}ms (${((trimEndMs - trimStartMs).coerceAtLeast(0)) / 1000f}s)",
                            color = selectedColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stylized Waveform Graphic Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MinimalSurfaceVariant)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val barHeights = listOf(0.4f, 0.7f, 0.3f, 0.9f, 0.6f, 0.8f, 0.5f, 1.0f, 0.7f, 0.4f, 0.8f, 0.6f, 0.9f, 0.5f, 0.3f)
                        barHeights.forEachIndexed { i, h ->
                            val progressFrac = i.toFloat() / barHeights.size
                            val isWithinTrim = progressFrac >= (trimStartMs.toFloat() / durationMs.coerceAtLeast(1L)) &&
                                    progressFrac <= (trimEndMs.toFloat() / durationMs.coerceAtLeast(1L))
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height((h * 22).dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(if (isWithinTrim) selectedColor else MinimalBorder)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Start Trim Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start Offset", color = TextSecondary, fontSize = 11.sp)
                        Text("${trimStartMs}ms", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value = trimStartMs.toFloat(),
                        onValueChange = {
                            trimStartMs = it.toLong().coerceAtMost(trimEndMs - 100L).coerceAtLeast(0L)
                        },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(100f),
                        colors = SliderDefaults.colors(thumbColor = selectedColor, activeTrackColor = selectedColor, inactiveTrackColor = MinimalBorder)
                    )

                    // End Trim Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("End Offset", color = TextSecondary, fontSize = 11.sp)
                        Text("${trimEndMs}ms", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value = trimEndMs.toFloat(),
                        onValueChange = {
                            trimEndMs = it.toLong().coerceAtLeast(trimStartMs + 100L).coerceAtMost(durationMs)
                        },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(100f),
                        colors = SliderDefaults.colors(thumbColor = selectedColor, activeTrackColor = selectedColor, inactiveTrackColor = MinimalBorder)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview Test Button
            Button(
                onClick = {
                    val previewSound = SoundClipEntity(
                        id = 999999L,
                        title = title,
                        emoji = emoji,
                        colorHex = colorHex,
                        categoryId = categoryId,
                        audioFilePath = audioFilePath,
                        presetKey = presetKey,
                        durationMs = durationMs.coerceAtMost(10000L),
                        isLooping = isLooping,
                        volume = volume,
                        pitch = pitch,
                        playbackSpeed = playbackSpeed,
                        trimStartMs = trimStartMs,
                        trimEndMs = trimEndMs,
                        activeFx = activeFx
                    )
                    playbackManager.playSound(previewSound)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MinimalSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, selectedColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_sound_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = selectedColor)
                    Text("Test Preview Playback with FX & Trim", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save and Delete Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (soundToEdit != null && onDeleteSound != null) {
                    Button(
                        onClick = { onDeleteSound(soundToEdit) },
                        colors = ButtonDefaults.buttonColors(containerColor = MinimalError.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalError),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_sound_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MinimalError, modifier = Modifier.size(16.dp))
                            Text("Delete", color = MinimalError, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        val finalSound = SoundClipEntity(
                            id = soundToEdit?.id ?: 0L,
                            title = title.trim().ifEmpty { "Sound" },
                            emoji = emoji,
                            colorHex = colorHex,
                            categoryId = categoryId,
                            audioFilePath = audioFilePath,
                            presetKey = presetKey,
                            durationMs = durationMs.coerceAtMost(10000L),
                            isLooping = isLooping,
                            volume = volume,
                            pitch = pitch,
                            playbackSpeed = playbackSpeed,
                            isFavorite = soundToEdit?.isFavorite ?: false,
                            playCount = soundToEdit?.playCount ?: 0,
                            trimStartMs = trimStartMs,
                            trimEndMs = trimEndMs,
                            activeFx = activeFx
                        )
                        onSaveSound(finalSound)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(if (soundToEdit != null) 2f else 1f)
                        .testTag("save_sound_button")
                ) {
                    Text(
                        if (soundToEdit == null) "Create Sound Pad" else "Save Changes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
