package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SoundClipEntity
import com.example.ui.components.CategoryBar
import com.example.ui.components.SoundPadButton
import com.example.ui.components.TopSoundboardBar
import com.example.ui.dialogs.CategoryManageDialog
import com.example.ui.dialogs.CloudSyncDialog
import com.example.ui.dialogs.EditSoundDialog
import com.example.ui.dialogs.LiveMixerDialog
import com.example.ui.dialogs.MetronomeDialog
import com.example.ui.dialogs.QuickVoiceRecordDialog
import com.example.ui.dialogs.SequenceBeatDialog
import com.example.ui.dialogs.SoundPacksDialog
import com.example.ui.dialogs.StatsDashboardDialog
import com.example.ui.dialogs.TtsGeneratorDialog
import com.example.ui.theme.MinimalCanvas
import com.example.ui.theme.MinimalError
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceElevated
import com.example.ui.theme.MinimalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SoundboardScreen(
    viewModel: SoundboardViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allSounds by viewModel.allSounds.collectAsStateWithLifecycle()
    val allSequences by viewModel.allSequences.collectAsStateWithLifecycle()
    val displayedSounds by viewModel.displayedSounds.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()

    val playbackStates by viewModel.playbackStates.collectAsStateWithLifecycle()
    val activePlayingCount by viewModel.activePlayingCount.collectAsStateWithLifecycle()
    val channelVolumes by viewModel.channelVolumes.collectAsStateWithLifecycle()
    val mutedState by viewModel.mutedState.collectAsStateWithLifecycle()
    val masterVolume by viewModel.masterVolume.collectAsStateWithLifecycle()
    val cloudSyncInfo by viewModel.cloudSyncInfo.collectAsStateWithLifecycle()
    val accessibilitySettings by viewModel.accessibilitySettings.collectAsStateWithLifecycle()

    val isAddSoundOpen by viewModel.isAddSoundDialogOpen.collectAsStateWithLifecycle()
    val isQuickRecordOpen by viewModel.isQuickRecordOpen.collectAsStateWithLifecycle()
    val editingSound by viewModel.editingSound.collectAsStateWithLifecycle()
    val isCategoryManageOpen by viewModel.isCategoryManageOpen.collectAsStateWithLifecycle()
    val isCloudSyncOpen by viewModel.isCloudSyncOpen.collectAsStateWithLifecycle()
    val isAccessibilityOpen by viewModel.isAccessibilityOpen.collectAsStateWithLifecycle()

    // 10 Best Features State
    val isLiveMixerOpen by viewModel.isLiveMixerOpen.collectAsStateWithLifecycle()
    val isSequenceBeatOpen by viewModel.isSequenceBeatOpen.collectAsStateWithLifecycle()
    val isTtsDialogOpen by viewModel.isTtsDialogOpen.collectAsStateWithLifecycle()
    val isSoundPacksOpen by viewModel.isSoundPacksOpen.collectAsStateWithLifecycle()
    val isMetronomeOpen by viewModel.isMetronomeOpen.collectAsStateWithLifecycle()
    val isStatsOpen by viewModel.isStatsOpen.collectAsStateWithLifecycle()

    val sequencePlayback by viewModel.sequencePlayback.collectAsStateWithLifecycle()
    val isRecordingSequence by viewModel.isRecordingSequence.collectAsStateWithLifecycle()
    val recordedSteps by viewModel.recordedSteps.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearUserMessage()
        }
    }

    val selectedCategoryId: Long? = when (val filter = selectedFilter) {
        is CategoryFilter.Custom -> filter.categoryId
        else -> null
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MinimalSurface)) {
                TopSoundboardBar(
                    activePlayingCount = activePlayingCount,
                    masterVolume = masterVolume,
                    cloudSyncInfo = cloudSyncInfo,
                    searchQuery = searchQuery,
                    sortMode = sortMode,
                    boardTheme = boardTheme,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onSortModeChange = { viewModel.setSortMode(it) },
                    onThemeChange = { viewModel.setBoardTheme(it) },
                    onStopAllClick = { viewModel.stopAllSounds() },
                    onMasterVolumeChange = { viewModel.setMasterVolume(it) },
                    onCloudSyncClick = { viewModel.setCloudSyncOpen(true) },
                    onAccessibilityClick = { viewModel.setAccessibilityOpen(true) },
                    onOpenQuickRecord = { viewModel.setQuickRecordOpen(true) },
                    onOpenMixer = { viewModel.setLiveMixerOpen(true) },
                    onOpenSequencer = { viewModel.setSequenceBeatOpen(true) },
                    onOpenTts = { viewModel.setTtsDialogOpen(true) },
                    onOpenPacks = { viewModel.setSoundPacksOpen(true) },
                    onOpenMetronome = { viewModel.setMetronomeOpen(true) },
                    onOpenStats = { viewModel.setStatsOpen(true) }
                )
                CategoryBar(
                    categories = categories,
                    allSounds = allSounds,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.selectFilter(it) },
                    onManageCategoriesClick = { viewModel.setCategoryManageOpen(true) }
                )
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 10-Second Voice Recording Action Button
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setQuickRecordOpen(true) },
                    containerColor = MinimalError,
                    contentColor = Color.White,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Record (10s)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier
                        .testTag("quick_record_fab")
                        .semantics { contentDescription = "Initiate 10-second voice recording session" }
                )

                // Add New Sound Pad Button
                FloatingActionButton(
                    onClick = { viewModel.openAddSoundDialog() },
                    containerColor = MinimalPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .testTag("add_sound_fab")
                        .semantics { contentDescription = "Add new custom sound pad" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        containerColor = MinimalCanvas
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (displayedSounds.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MinimalSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = MinimalPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No sounds matching \"$searchQuery\"" else "No sounds in this category yet",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the + button below to create a custom sound pad, generate a voice clip, or install curated sound packs!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Pad Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(accessibilitySettings.gridColumns),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 10.dp,
                        bottom = 80.dp // Padding for FAB
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("sound_pad_grid")
                ) {
                    items(displayedSounds, key = { it.id }) { sound ->
                        val status = playbackStates[sound.id]
                        SoundPadButton(
                            sound = sound,
                            playbackStatus = status,
                            highContrast = accessibilitySettings.highContrast,
                            onPadClick = { viewModel.onSoundPadClicked(sound) },
                            onLoopToggle = { viewModel.toggleSoundLoopSetting(sound) },
                            onFavoriteToggle = { viewModel.toggleFavorite(sound) },
                            onEditClick = { viewModel.openAddSoundDialog(sound) },
                            onDuplicateClick = { viewModel.duplicateSound(sound) },
                            onShareClick = { viewModel.shareSound(sound) },
                            onDeleteClick = { viewModel.deleteSound(sound) },
                            modifier = Modifier.height(
                                when (accessibilitySettings.gridColumns) {
                                    2 -> 135.dp
                                    3 -> 125.dp
                                    else -> 115.dp
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    if (isQuickRecordOpen) {
        QuickVoiceRecordDialog(
            recorderHelper = viewModel.recorderHelper,
            categories = categories,
            onSoundCreated = { sound ->
                viewModel.saveSound(sound)
                viewModel.setQuickRecordOpen(false)
            },
            onDismiss = { viewModel.setQuickRecordOpen(false) }
        )
    }

    // Modal Dialogs
    if (isAddSoundOpen) {
        EditSoundDialog(
            soundToEdit = editingSound,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            recorderHelper = viewModel.recorderHelper,
            playbackManager = viewModel.playbackManager,
            onSaveSound = { viewModel.saveSound(it) },
            onDeleteSound = if (editingSound != null) { { viewModel.deleteSound(it) } } else null,
            onDismiss = { viewModel.closeSoundDialog() }
        )
    }

    if (isCategoryManageOpen) {
        CategoryManageDialog(
            categories = categories,
            onSaveCategory = { viewModel.saveCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { viewModel.setCategoryManageOpen(false) }
        )
    }

    if (isCloudSyncOpen || isAccessibilityOpen) {
        CloudSyncDialog(
            cloudSyncInfo = cloudSyncInfo,
            accessibilitySettings = accessibilitySettings,
            onSyncToCloud = { viewModel.syncToCloud() },
            onRestoreFromCloud = { code, overwrite -> viewModel.restoreFromCloud(code, overwrite) },
            onUpdateSyncCode = { viewModel.setSyncCode(it) },
            onUpdateAccessibility = { viewModel.updateAccessibilitySettings(it) },
            onDismiss = {
                viewModel.setCloudSyncOpen(false)
                viewModel.setAccessibilityOpen(false)
            }
        )
    }

    // Best 10 Features Dialogs
    if (isLiveMixerOpen) {
        LiveMixerDialog(
            activeSounds = allSounds,
            playbackStates = playbackStates,
            channelVolumes = channelVolumes,
            mutedChannels = mutedState,
            masterVolume = masterVolume,
            onMasterVolumeChange = { vol -> viewModel.setMasterVolume(vol) },
            onChannelVolumeChange = { id, vol -> viewModel.playbackManager.setChannelVolume(id, vol) },
            onToggleMute = { id -> viewModel.playbackManager.toggleMute(id) },
            onSoloChannel = { id -> viewModel.playbackManager.soloChannel(id) },
            onStopChannel = { id -> viewModel.playbackManager.stopSound(id) },
            onFadeAllOut = { viewModel.fadeAllOut() },
            onStopAll = { viewModel.stopAllSounds() },
            onDismiss = { viewModel.setLiveMixerOpen(false) }
        )
    }

    if (isSequenceBeatOpen) {
        SequenceBeatDialog(
            sequences = allSequences,
            playbackState = sequencePlayback,
            isRecording = isRecordingSequence,
            recordedSteps = recordedSteps,
            onStartRecording = { viewModel.startSequenceRecording() },
            onStopRecordingAndSave = { title, looping, bpm ->
                viewModel.stopSequenceRecordingAndSave(title, looping, bpm)
            },
            onPlaySequence = { seq -> viewModel.playSequence(seq) },
            onStopPlayback = { viewModel.stopSequencePlayback() },
            onDeleteSequence = { seq -> viewModel.deleteSequence(seq) },
            onDismiss = { viewModel.setSequenceBeatOpen(false) }
        )
    }

    if (isTtsDialogOpen) {
        TtsGeneratorDialog(
            ttsHelper = viewModel.ttsHelper,
            categories = categories,
            onSoundCreated = { sound ->
                viewModel.saveSound(sound)
                viewModel.setTtsDialogOpen(false)
            },
            onDismiss = { viewModel.setTtsDialogOpen(false) }
        )
    }

    if (isSoundPacksOpen) {
        SoundPacksDialog(
            onInstallPack = { pack -> viewModel.installSoundPack(pack) },
            onDismiss = { viewModel.setSoundPacksOpen(false) }
        )
    }

    if (isMetronomeOpen) {
        val isMetroPlaying by viewModel.metronomeManager.isPlaying.collectAsStateWithLifecycle()
        val metroBpm by viewModel.metronomeManager.bpm.collectAsStateWithLifecycle()
        val metroBeat by viewModel.metronomeManager.currentBeat.collectAsStateWithLifecycle()
        val metroBeatsPerBar by viewModel.metronomeManager.beatsPerBar.collectAsStateWithLifecycle()

        MetronomeDialog(
            metronomeManager = viewModel.metronomeManager,
            isPlaying = isMetroPlaying,
            bpm = metroBpm,
            currentBeat = metroBeat,
            beatsPerBar = metroBeatsPerBar,
            onTogglePlay = { viewModel.metronomeManager.togglePlay() },
            onBpmChange = { viewModel.metronomeManager.setBpm(it) },
            onTapTempo = { viewModel.metronomeManager.registerTapTempo() },
            onDismiss = { viewModel.setMetronomeOpen(false) }
        )
    }

    if (isStatsOpen) {
        StatsDashboardDialog(
            sounds = allSounds,
            categories = categories,
            onDismiss = { viewModel.setStatsOpen(false) }
        )
    }
}

