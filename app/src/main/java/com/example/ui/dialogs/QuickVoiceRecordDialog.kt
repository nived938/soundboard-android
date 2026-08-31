package com.example.ui.dialogs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioRecorderHelper
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

private val VOICE_EMOJIS = listOf(
    "🎙️", "🗣️", "🎤", "🔊", "💬", "🤖", "⚡", "🔥", "✨", "🎵",
    "💥", "👽", "📢", "🚨", "🎉", "👑", "🥁", "🎮", "🏆", "💀"
)

private val FX_PRESETS = listOf(
    "NONE" to "Clean Mic",
    "REVERB" to "Studio Reverb",
    "ROBOT" to "Robot Vocoder",
    "ECHO" to "Echo Delay",
    "PITCH_HIGH" to "High Pitch (+4st)",
    "PITCH_LOW" to "Deep Pitch (-4st)",
    "BITCRUSH" to "8-Bit Lo-Fi"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickVoiceRecordDialog(
    recorderHelper: AudioRecorderHelper,
    categories: List<CategoryEntity>,
    onSoundCreated: (SoundClipEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recordingState by recorderHelper.recordingState.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            recorderHelper.startRecording()
        }
    }

    // Recorded metadata form
    var clipTitle by remember { mutableStateOf("Voice Clip ${System.currentTimeMillis() % 1000}") }
    var selectedEmoji by remember { mutableStateOf("🎙️") }
    var selectedColorHex by remember { mutableStateOf("#6750A4") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var selectedFx by remember { mutableStateOf("NONE") }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Preview audio player
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var recordedDurationMs by remember { mutableLongStateOf(0L) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            previewPlayer = null
            recorderHelper.reset()
        }
    }

    // Pulse animation for active recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = {
            previewPlayer?.stop()
            recorderHelper.reset()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MinimalSurface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MinimalPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MinimalPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "10s Voice Clip Recorder",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Record up to 10 seconds of mic audio",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        previewPlayer?.stop()
                        recorderHelper.reset()
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Recording / Preview Stage Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (recordingState.isRecording) MinimalPrimaryContainer.copy(alpha = 0.25f) else MinimalSurfaceElevated
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (recordingState.isRecording) MinimalPrimary else MinimalBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!hasPermission) {
                        // Permission Request State
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MinimalError,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Microphone Permission Required",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant microphone access to record custom audio clips directly onto your soundboard.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = MinimalPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Grant Access & Start Recording")
                        }
                    } else if (recordingState.isRecording) {
                        // Active Recording State (Countdown & Live Waveform)
                        val elapsedSec = recordingState.recordedMs / 1000f
                        val progress = (recordingState.recordedMs / 10000f).coerceIn(0f, 1f)
                        val remainingSec = (10f - elapsedSec).coerceAtLeast(0f)

                        Text(
                            text = "RECORDING MIC...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MinimalError,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = String.format("%.1fs remaining", remainingSec),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar up to 10s
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MinimalError,
                            trackColor = MinimalBorder
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Waveform Visualizer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val amps = recordingState.amplitudes.takeLast(24)
                            if (amps.isEmpty()) {
                                repeat(16) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MinimalBorderActive)
                                    )
                                }
                            } else {
                                amps.forEach { amp ->
                                    val barHeight = (amp * 40f + 4f).coerceIn(4f, 44f).dp
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MinimalPrimary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stop Recording Button
                        Button(
                            onClick = {
                                recorderHelper.stopRecording { file, durationMs ->
                                    recordedFilePath = file.absolutePath
                                    recordedDurationMs = durationMs
                                }
                            },
                            modifier = Modifier
                                .scale(pulseScale)
                                .testTag("stop_recording_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MinimalError),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop & Keep (${String.format("%.1fs", elapsedSec)})", fontWeight = FontWeight.Bold)
                        }
                    } else if (recordedFilePath != null || recordingState.isFinished) {
                        // Finished Recording State (Preview & Retake)
                        val filePath = recordedFilePath ?: recordingState.recordedFilePath
                        val duration = if (recordedDurationMs > 0) recordedDurationMs else recordingState.recordedMs

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Audio Clip Captured!",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MinimalSuccess
                                )
                                Text(
                                    text = "Duration: ${String.format("%.1fs", duration / 1000f)} • Ready to assign",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Preview Play Button
                                IconButton(
                                    onClick = {
                                        if (isPreviewPlaying) {
                                            previewPlayer?.stop()
                                            previewPlayer?.release()
                                            previewPlayer = null
                                            isPreviewPlaying = false
                                        } else if (filePath != null) {
                                            try {
                                                previewPlayer?.release()
                                                previewPlayer = MediaPlayer().apply {
                                                    setDataSource(filePath)
                                                    prepare()
                                                    setOnCompletionListener {
                                                        isPreviewPlaying = false
                                                    }
                                                    start()
                                                }
                                                isPreviewPlaying = true
                                            } catch (e: Exception) {
                                                isPreviewPlaying = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MinimalPrimary.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewPlaying) "Stop" else "Preview",
                                        tint = MinimalPrimary
                                    )
                                }

                                // Re-record Button
                                IconButton(
                                    onClick = {
                                        previewPlayer?.stop()
                                        previewPlayer?.release()
                                        previewPlayer = null
                                        isPreviewPlaying = false
                                        recordedFilePath = null
                                        recorderHelper.reset()
                                        recorderHelper.startRecording { file, dur ->
                                            recordedFilePath = file.absolutePath
                                            recordedDurationMs = dur
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MinimalSurfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Re-record",
                                        tint = TextPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        // Idle / Initial Ready State
                        Text(
                            text = "Tap to Record (Max 10s)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Speak clearly into your device microphone",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (hasPermission) {
                                    recorderHelper.startRecording { file, dur ->
                                        recordedFilePath = file.absolutePath
                                        recordedDurationMs = dur
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("start_10s_recording_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MinimalPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start 10s Voice Session", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pad Customization Controls (Shown if clip is recorded or being edited)
            OutlinedTextField(
                value = clipTitle,
                onValueChange = { clipTitle = it },
                label = { Text("Pad Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MinimalPrimary,
                    unfocusedBorderColor = MinimalBorder
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selector Dropdown
            var categoryDropdownExpanded by remember { mutableStateOf(false) }
            val selectedCategory = categories.find { it.id == selectedCategoryId } ?: categories.firstOrNull()

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "${selectedCategory?.icon ?: "📁"} ${selectedCategory?.name ?: "General"}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalPrimary,
                        unfocusedBorderColor = MinimalBorder
                    )
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
                                selectedCategoryId = cat.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sound Effect (DSP Preset)
            Text(
                text = "Apply Audio Effect",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(FX_PRESETS) { (key, label) ->
                    val isSelected = selectedFx == key
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MinimalPrimary else MinimalSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MinimalPrimary else MinimalBorder
                        ),
                        modifier = Modifier.clickable { selectedFx = key }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Emoji Picker
            Text(
                text = "Pad Icon Emoji",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VOICE_EMOJIS) { emoji ->
                    val isSelected = selectedEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MinimalPrimary.copy(alpha = 0.2f) else MinimalSurfaceVariant)
                            .border(
                                1.5.dp,
                                if (isSelected) MinimalPrimary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color Swatches
            Text(
                text = "Pad Color Accent",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PadColorSwatches) { hex ->
                    val color = parseColorSafe(hex)
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) TextPrimary else MinimalBorder,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            val finalPath = recordedFilePath ?: recordingState.recordedFilePath
            val finalDuration = if (recordedDurationMs > 0) recordedDurationMs else recordingState.recordedMs

            Button(
                onClick = {
                    if (finalPath != null && File(finalPath).exists()) {
                        val soundClip = SoundClipEntity(
                            title = clipTitle.ifBlank { "Voice Clip" },
                            categoryId = selectedCategoryId,
                            audioFilePath = finalPath,
                            durationMs = finalDuration.coerceIn(100L, 10000L),
                            volume = volume,
                            pitch = 1.0f,
                            playbackSpeed = playbackSpeed,
                            isLooping = false,
                            colorHex = selectedColorHex,
                            emoji = selectedEmoji,
                            activeFx = selectedFx
                        )
                        onSoundCreated(soundClip)
                    }
                },
                enabled = finalPath != null && File(finalPath).exists() && !recordingState.isRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_voice_clip_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MinimalPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (finalPath != null) "Save Custom Voice Pad" else "Record Mic Audio to Save",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
