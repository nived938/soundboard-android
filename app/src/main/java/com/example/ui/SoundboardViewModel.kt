package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEffectsProcessor
import com.example.audio.AudioRecorderHelper
import com.example.audio.AudioShareHelper
import com.example.audio.AudioWaveformHelper
import com.example.audio.MetronomeManager
import com.example.audio.PlaybackStatus
import com.example.audio.RecordingState
import com.example.audio.SequencePlaybackState
import com.example.audio.SequencePlayerManager
import com.example.audio.SoundPlaybackManager
import com.example.audio.TextToSpeechHelper
import com.example.data.model.CategoryEntity
import com.example.data.model.SequenceEntity
import com.example.data.model.SoundClipEntity
import com.example.data.preset.SoundPackDefinition
import com.example.data.repository.SoundboardRepository
import com.example.data.sync.CloudSyncInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class CategoryFilter {
    object All : CategoryFilter()
    object Favorites : CategoryFilter()
    data class Custom(val categoryId: Long) : CategoryFilter()
}

enum class SoundSortMode(val label: String, val emoji: String) {
    FAVORITES_FIRST("Favorites", "⭐"),
    MOST_PLAYED("Most Played", "🔥"),
    ALPHABETICAL("A to Z", "🔤"),
    NEWEST("Newest", "🆕"),
    DURATION("Duration", "⏱️")
}

enum class BoardThemePalette(val title: String, val primaryHex: String, val bgHex: String, val isDark: Boolean) {
    MINIMAL_VIOLET("Clean Minimalist", "#6750A4", "#FDF8F6", false),
    OBSIDIAN_DARK("Studio Obsidian", "#A855F7", "#121218", true),
    CYBERPUNK_NEON("Cyberpunk Neon", "#00F0FF", "#0D1117", true),
    SUNSET_AMBER("Sunset Terracotta", "#E65100", "#FFF8F0", false),
    FOREST_SAGE("Forest Sage", "#2E7D32", "#F4F8F4", false)
}

data class AccessibilitySettings(
    val highContrast: Boolean = false,
    val gridColumns: Int = 2, // 2 = Large/Accessible, 3 = Standard, 4 = Compact
    val hapticFeedback: Boolean = true,
    val showDurationLabels: Boolean = true
)

class SoundboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SoundboardRepository(application)
    val playbackManager = SoundPlaybackManager(application)
    val recorderHelper = AudioRecorderHelper(application)
    val ttsHelper = TextToSpeechHelper(application)
    val metronomeManager = MetronomeManager(application)
    val sequenceManager = SequencePlayerManager(playbackManager)

    // Raw Room Flows
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSounds: StateFlow<List<SoundClipEntity>> = repository.allSounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSequences: StateFlow<List<SequenceEntity>> = repository.allSequences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudSyncInfo: StateFlow<CloudSyncInfo> = repository.cloudSyncInfo

    // Filters & Sorting
    private val _selectedFilter = MutableStateFlow<CategoryFilter>(CategoryFilter.All)
    val selectedFilter: StateFlow<CategoryFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(SoundSortMode.FAVORITES_FIRST)
    val sortMode: StateFlow<SoundSortMode> = _sortMode.asStateFlow()

    private val _boardTheme = MutableStateFlow(BoardThemePalette.MINIMAL_VIOLET)
    val boardTheme: StateFlow<BoardThemePalette> = _boardTheme.asStateFlow()

    // Filtered & Sorted sounds flow for UI
    val displayedSounds: StateFlow<List<SoundClipEntity>> = combine(
        allSounds,
        _selectedFilter,
        _searchQuery,
        _sortMode
    ) { sounds, filter, query, sort ->
        var list = when (filter) {
            is CategoryFilter.All -> sounds
            is CategoryFilter.Favorites -> sounds.filter { it.isFavorite }
            is CategoryFilter.Custom -> sounds.filter { it.categoryId == filter.categoryId }
        }
        val cleanQuery = query.trim()
        if (cleanQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(cleanQuery, ignoreCase = true) ||
                it.emoji.contains(cleanQuery, ignoreCase = true) ||
                it.activeFx.contains(cleanQuery, ignoreCase = true) ||
                (it.presetKey != null && it.presetKey.contains(cleanQuery, ignoreCase = true))
            }
        }
        when (sort) {
            SoundSortMode.FAVORITES_FIRST -> list.sortedWith(compareByDescending<SoundClipEntity> { it.isFavorite }.thenBy { it.sortOrder })
            SoundSortMode.MOST_PLAYED -> list.sortedByDescending { it.playCount }
            SoundSortMode.ALPHABETICAL -> list.sortedBy { it.title.lowercase() }
            SoundSortMode.NEWEST -> list.sortedByDescending { it.createdAt }
            SoundSortMode.DURATION -> list.sortedBy { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audio Playback State
    val playbackStates: StateFlow<Map<Long, PlaybackStatus>> = playbackManager.playbackStates
    val activePlayingCount: StateFlow<Int> = playbackManager.activeCount
    val channelVolumes: StateFlow<Map<Long, Float>> = playbackManager.channelVolumes
    val mutedState: StateFlow<Set<Long>> = playbackManager.mutedState

    // Sequence / Combo Beat State
    val sequencePlayback: StateFlow<SequencePlaybackState> = sequenceManager.playbackState
    val isRecordingSequence: StateFlow<Boolean> = sequenceManager.isRecording
    val recordedSteps = sequenceManager.recordedSteps

    // Recorder State
    val recordingState: StateFlow<RecordingState> = recorderHelper.recordingState

    // Master Controls & Accessibility
    private val _masterVolume = MutableStateFlow(1.0f)
    val masterVolume: StateFlow<Float> = _masterVolume.asStateFlow()

    private val _accessibilitySettings = MutableStateFlow(AccessibilitySettings())
    val accessibilitySettings: StateFlow<AccessibilitySettings> = _accessibilitySettings.asStateFlow()

    // Feature Dialog States
    private val _editingSound = MutableStateFlow<SoundClipEntity?>(null)
    val editingSound: StateFlow<SoundClipEntity?> = _editingSound.asStateFlow()

    private val _isAddSoundDialogOpen = MutableStateFlow(false)
    val isAddSoundDialogOpen: StateFlow<Boolean> = _isAddSoundDialogOpen.asStateFlow()

    private val _isCategoryManageOpen = MutableStateFlow(false)
    val isCategoryManageOpen: StateFlow<Boolean> = _isCategoryManageOpen.asStateFlow()

    private val _isCloudSyncOpen = MutableStateFlow(false)
    val isCloudSyncOpen: StateFlow<Boolean> = _isCloudSyncOpen.asStateFlow()

    private val _isAccessibilityOpen = MutableStateFlow(false)
    val isAccessibilityOpen: StateFlow<Boolean> = _isAccessibilityOpen.asStateFlow()

    // 10 Features Dialogs
    private val _isQuickRecordOpen = MutableStateFlow(false)
    val isQuickRecordOpen: StateFlow<Boolean> = _isQuickRecordOpen.asStateFlow()

    private val _isLiveMixerOpen = MutableStateFlow(false)
    val isLiveMixerOpen: StateFlow<Boolean> = _isLiveMixerOpen.asStateFlow()

    private val _isSequenceBeatOpen = MutableStateFlow(false)
    val isSequenceBeatOpen: StateFlow<Boolean> = _isSequenceBeatOpen.asStateFlow()

    private val _isTtsDialogOpen = MutableStateFlow(false)
    val isTtsDialogOpen: StateFlow<Boolean> = _isTtsDialogOpen.asStateFlow()

    private val _isSoundPacksOpen = MutableStateFlow(false)
    val isSoundPacksOpen: StateFlow<Boolean> = _isSoundPacksOpen.asStateFlow()

    private val _isStatsOpen = MutableStateFlow(false)
    val isStatsOpen: StateFlow<Boolean> = _isStatsOpen.asStateFlow()

    private val _isMetronomeOpen = MutableStateFlow(false)
    val isMetronomeOpen: StateFlow<Boolean> = _isMetronomeOpen.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultPresetsPopulated()
        }
    }

    // --- Sound Interactions ---

    fun onSoundPadClicked(sound: SoundClipEntity) {
        playbackManager.playSound(sound)
        if (isRecordingSequence.value) {
            sequenceManager.recordPadHit(sound)
        }
        viewModelScope.launch {
            repository.incrementPlayCount(sound.id)
        }
    }

    fun onSoundPadLoopToggled(sound: SoundClipEntity) {
        playbackManager.toggleLoop(sound)
        viewModelScope.launch {
            repository.incrementPlayCount(sound.id)
        }
    }

    fun stopAllSounds() {
        playbackManager.stopAll()
        sequenceManager.stopPlayback()
        _userMessage.value = "Stopped all sounds"
    }

    fun fadeAllOut() {
        playbackManager.fadeAllOut()
        _userMessage.value = "Fading out all channels..."
    }

    fun setMasterVolume(vol: Float) {
        _masterVolume.value = vol.coerceIn(0f, 1f)
        playbackManager.masterVolume = vol
    }

    fun setSortMode(mode: SoundSortMode) {
        _sortMode.value = mode
    }

    fun setBoardTheme(theme: BoardThemePalette) {
        _boardTheme.value = theme
    }

    fun toggleFavorite(sound: SoundClipEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(sound.id, sound.isFavorite)
        }
    }

    fun toggleSoundLoopSetting(sound: SoundClipEntity) {
        viewModelScope.launch {
            repository.toggleLoop(sound.id, sound.isLooping)
            _userMessage.value = if (!sound.isLooping) "Seamless loop enabled" else "Loop disabled"
        }
    }

    fun saveSound(sound: SoundClipEntity) {
        viewModelScope.launch {
            if (sound.id == 0L) {
                repository.insertSound(sound)
                _userMessage.value = "Sound \"${sound.title}\" created!"
            } else {
                repository.updateSound(sound)
                _userMessage.value = "Sound \"${sound.title}\" updated!"
            }
            closeSoundDialog()
        }
    }

    fun deleteSound(sound: SoundClipEntity) {
        viewModelScope.launch {
            playbackManager.stopSound(sound.id)
            repository.deleteSound(sound)
            _userMessage.value = "Deleted \"${sound.title}\""
            if (_editingSound.value?.id == sound.id) {
                closeSoundDialog()
            }
        }
    }

    fun duplicateSound(sound: SoundClipEntity) {
        viewModelScope.launch {
            val copy = sound.copy(
                id = 0,
                title = "${sound.title} (Copy)",
                createdAt = System.currentTimeMillis()
            )
            repository.insertSound(copy)
            _userMessage.value = "Duplicated \"${sound.title}\""
        }
    }

    fun shareSound(sound: SoundClipEntity) {
        AudioShareHelper.shareAudioClip(getApplication(), sound)
    }

    // --- Audio FX & Waveform Trimming ---

    fun applyTrimToClip(sourcePath: String, startMs: Long, endMs: Long, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val targetFile = File(app.filesDir, "trimmed_${System.currentTimeMillis()}.wav")
            val success = withContext(Dispatchers.IO) {
                AudioWaveformHelper.trimWavFile(File(sourcePath), targetFile, startMs, endMs)
            }
            if (success) {
                onDone(targetFile.absolutePath)
                _userMessage.value = "Trim applied successfully!"
            } else {
                _userMessage.value = "Failed to trim audio clip"
            }
        }
    }

    fun applyFxToClip(sourcePath: String, fx: AudioEffectsProcessor.SoundFx, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val targetFile = File(app.filesDir, "fx_${fx.name.lowercase()}_${System.currentTimeMillis()}.wav")
            val success = withContext(Dispatchers.IO) {
                AudioEffectsProcessor.applyFxToWavFile(File(sourcePath), targetFile, fx)
            }
            if (success) {
                onDone(targetFile.absolutePath)
                _userMessage.value = "${fx.title} effect applied!"
            } else {
                _userMessage.value = "Failed to apply effect"
            }
        }
    }

    // --- Sequence & Beat Performer ---

    fun startSequenceRecording() {
        sequenceManager.startRecording()
        _userMessage.value = "Recording pad sequence... Tap pads now!"
    }

    fun stopSequenceRecordingAndSave(title: String, isLooping: Boolean, bpm: Int) {
        val steps = sequenceManager.stopRecording()
        if (steps.isEmpty()) {
            _userMessage.value = "No pads were tapped during recording."
            return
        }
        val serialized = SequencePlayerManager.serializeSteps(steps)
        val maxOffset = steps.maxOfOrNull { it.offsetMs } ?: 2000L
        val totalDur = (maxOffset + 800L).coerceAtLeast(1000L)

        viewModelScope.launch {
            val entity = SequenceEntity(
                title = title.ifBlank { "Combo Beat #${System.currentTimeMillis() % 1000}" },
                eventsJson = serialized,
                totalDurationMs = totalDur,
                isLooping = isLooping,
                bpm = bpm
            )
            repository.insertSequence(entity)
            _userMessage.value = "Beat Combo \"${entity.title}\" saved!"
        }
    }

    fun playSequence(sequence: SequenceEntity) {
        val soundsMap = allSounds.value.associateBy { it.id }
        sequenceManager.playSequence(sequence, soundsMap)
    }

    fun stopSequencePlayback() {
        sequenceManager.stopPlayback()
    }

    fun deleteSequence(sequence: SequenceEntity) {
        viewModelScope.launch {
            if (sequencePlayback.value.activeSequenceId == sequence.id) {
                sequenceManager.stopPlayback()
            }
            repository.deleteSequence(sequence)
            _userMessage.value = "Deleted sequence \"${sequence.title}\""
        }
    }

    // --- Curated Sound Packs ---

    fun installSoundPack(pack: SoundPackDefinition) {
        viewModelScope.launch {
            repository.installSoundPack(pack)
            _userMessage.value = "Installed \"${pack.title}\" Sound Pack!"
            setSoundPacksOpen(false)
        }
    }

    // --- Category Interactions ---

    fun selectFilter(filter: CategoryFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.id == 0L) {
                repository.insertCategory(category)
                _userMessage.value = "Category \"${category.name}\" created"
            } else {
                repository.updateCategory(category)
                _userMessage.value = "Category \"${category.name}\" updated"
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            if (_selectedFilter.value is CategoryFilter.Custom &&
                (_selectedFilter.value as CategoryFilter.Custom).categoryId == category.id
            ) {
                _selectedFilter.value = CategoryFilter.All
            }
            _userMessage.value = "Category deleted"
        }
    }

    // --- Cloud Sync ---

    fun syncToCloud() {
        viewModelScope.launch {
            val cats = categories.value
            val snds = allSounds.value
            val res = repository.syncSoundboard(cats, snds)
            if (res.isSuccess) {
                _userMessage.value = "Cloud Sync successful!"
            } else {
                _userMessage.value = "Sync failed: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun restoreFromCloud(code: String, overwrite: Boolean) {
        viewModelScope.launch {
            val res = repository.restoreSoundboard(code, overwrite)
            if (res.isSuccess) {
                _userMessage.value = "Restored ${res.getOrNull()} sounds from cloud!"
            } else {
                _userMessage.value = "Restore failed: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun setSyncCode(code: String) {
        repository.setCloudSyncCode(code)
    }

    // --- Accessibility & UI Settings ---

    fun updateAccessibilitySettings(settings: AccessibilitySettings) {
        _accessibilitySettings.value = settings
        playbackManager.hapticsEnabled = settings.hapticFeedback
    }

    // --- Dialog Controls ---

    fun openAddSoundDialog(sound: SoundClipEntity? = null) {
        _editingSound.value = sound
        _isAddSoundDialogOpen.value = true
    }

    fun closeSoundDialog() {
        _editingSound.value = null
        _isAddSoundDialogOpen.value = false
        recorderHelper.reset()
    }

    fun setCategoryManageOpen(open: Boolean) {
        _isCategoryManageOpen.value = open
    }

    fun setCloudSyncOpen(open: Boolean) {
        _isCloudSyncOpen.value = open
    }

    fun setAccessibilityOpen(open: Boolean) {
        _isAccessibilityOpen.value = open
    }

    fun setQuickRecordOpen(open: Boolean) {
        _isQuickRecordOpen.value = open
        if (!open) {
            recorderHelper.reset()
        }
    }

    fun setLiveMixerOpen(open: Boolean) {
        _isLiveMixerOpen.value = open
    }

    fun setSequenceBeatOpen(open: Boolean) {
        _isSequenceBeatOpen.value = open
    }

    fun setTtsDialogOpen(open: Boolean) {
        _isTtsDialogOpen.value = open
    }

    fun setSoundPacksOpen(open: Boolean) {
        _isSoundPacksOpen.value = open
    }

    fun setStatsOpen(open: Boolean) {
        _isStatsOpen.value = open
    }

    fun setMetronomeOpen(open: Boolean) {
        _isMetronomeOpen.value = open
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
        metronomeManager.release()
        ttsHelper.release()
        sequenceManager.stopPlayback()
    }
}

