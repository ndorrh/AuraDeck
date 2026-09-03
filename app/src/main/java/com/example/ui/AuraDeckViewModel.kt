package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AuraDeckApp
import com.example.data.model.DspPresetEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.TrackEntity
import com.example.engine.DeckUiState
import com.example.engine.DualDeckUiState
import com.example.service.AuraPlaybackService
import com.example.stream.StreamResolver
import com.example.visualizer.VisualizerEngine
import com.example.visualizer.VisualizerFrame
import com.example.visualizer.VisualizerMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val icon: String) {
    DECKS("Dual Decks", "decks"),
    DSP_SUITE("DSP Studio", "equalizer"),
    VISUALIZER("Visualizer", "wave"),
    PLAYLISTS("Library", "library")
}

data class DspState(
    val isEnabled: Boolean = true,
    val bassBoostPercent: Float = 0.40f,
    val virtualizerPercent: Float = 0.70f,
    val dynamicsEnabled: Boolean = true,
    val eqBandLevels: List<Int> = List(10) { 0 },
    val activePresetName: String = "Audiophile Clarity"
)

data class StreamInputState(
    val urlInput: String = "",
    val isResolving: Boolean = false,
    val errorMessage: String? = null,
    val targetDeck: String = "A" // "A" or "B"
)

class AuraDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AuraDeckApp
    private val repository = app.repository
    val audioEngine = app.audioEngine
    val dspEngine = app.dspEngine
    val visualizerEngine = app.visualizerEngine

    private val _currentTab = MutableStateFlow(MainTab.DECKS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    val dualDeckState: StateFlow<DualDeckUiState> = audioEngine.uiState

    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dspPresets: StateFlow<List<DspPresetEntity>> = repository.dspPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dspState = MutableStateFlow(
        DspState(
            isEnabled = dspEngine.isEnabled,
            bassBoostPercent = dspEngine.bassBoostPercent,
            virtualizerPercent = dspEngine.virtualizerPercent,
            dynamicsEnabled = dspEngine.dynamicsEnabled,
            eqBandLevels = dspEngine.bandLevels.toList()
        )
    )
    val dspState: StateFlow<DspState> = _dspState.asStateFlow()

    val visualizerMode: StateFlow<VisualizerMode> = visualizerEngine.currentMode
    val visualizerFrame: StateFlow<VisualizerFrame> = visualizerEngine.frame

    private val _streamInputState = MutableStateFlow(StreamInputState())
    val streamInputState: StateFlow<StreamInputState> = _streamInputState.asStateFlow()

    init {
        // Link visualizer and foreground media service to active playback state
        viewModelScope.launch {
            dualDeckState.collect { state ->
                if (state.activeAudioSessionId != 0) {
                    visualizerEngine.linkAudioSession(state.activeAudioSessionId)
                }

                val isAnyPlaying = state.deckA.isPlaying || state.deckB.isPlaying
                val activeDeck = if (state.deckA.isPlaying) state.deckA
                                 else if (state.deckB.isPlaying) state.deckB
                                 else if (state.deckA.track != null) state.deckA
                                 else state.deckB

                val activeBpm = activeDeck.track?.bpm ?: 124
                val activeVu = maxOf(state.deckA.vuLevelL, state.deckA.vuLevelR, state.deckB.vuLevelL, state.deckB.vuLevelR)
                val activePos = activeDeck.currentPositionMs

                // Sync live playback telemetry with VisualizerEngine so visuals only run when playing and pulse on the beat
                visualizerEngine.updatePlaybackState(
                    isPlaying = isAnyPlaying,
                    bpm = activeBpm,
                    vuLevel = activeVu,
                    positionMs = activePos
                )

                // Update lock screen and notification media controls
                activeDeck.track?.let { track ->
                    AuraPlaybackService.update(
                        context = getApplication(),
                        title = track.title,
                        artist = track.artist,
                        isPlaying = isAnyPlaying,
                        positionMs = activePos,
                        durationMs = activeDeck.durationMs,
                        thumbnailUrl = track.thumbnailUrl
                    )
                }
            }
        }

        // Auto-load initial tracks into Deck A and Deck B when library loads
        viewModelScope.launch {
            allTracks.collect { tracks ->
                if (tracks.isNotEmpty()) {
                    if (dualDeckState.value.deckA.track == null) {
                        audioEngine.loadTrack("A", tracks[0], autoPlay = false)
                    }
                    if (tracks.size > 1 && dualDeckState.value.deckB.track == null) {
                        audioEngine.loadTrack("B", tracks[1], autoPlay = false)
                    }
                }
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    // Deck Playback Controls
    fun loadTrackToDeck(deckId: String, track: TrackEntity, autoPlay: Boolean = true) {
        audioEngine.loadTrack(deckId, track, autoPlay)
    }

    fun togglePlayPause(deckId: String) {
        audioEngine.togglePlayPause(deckId)
    }

    fun seekTo(deckId: String, positionMs: Long) {
        audioEngine.seekTo(deckId, positionMs)
    }

    fun setCrossfader(position: Float) {
        audioEngine.setCrossfader(position)
    }

    fun setMasterVolume(volume: Float) {
        audioEngine.setMasterVolume(volume)
    }

    fun setDeckVolume(deckId: String, volume: Float) {
        audioEngine.setDeckVolume(deckId, volume)
    }

    fun setDeckSpeed(deckId: String, speed: Float) {
        audioEngine.setDeckSpeed(deckId, speed)
    }

    fun toggleLoop(deckId: String) {
        audioEngine.toggleLoop(deckId)
    }

    fun setCue(deckId: String) {
        audioEngine.setCue(deckId)
    }

    fun jumpToCue(deckId: String) {
        audioEngine.jumpToCue(deckId)
    }

    fun syncBpm(targetDeckId: String) {
        audioEngine.syncBpm(targetDeckId)
    }

    // DSP Controls
    fun toggleDspMaster(enabled: Boolean) {
        dspEngine.setMasterDspEnabled(enabled)
        _dspState.update { it.copy(isEnabled = enabled) }
    }

    fun setBassBoost(percent: Float) {
        dspEngine.setBassBoostStrength(percent)
        _dspState.update { it.copy(bassBoostPercent = percent, activePresetName = "Custom") }
    }

    fun setVirtualizer(percent: Float) {
        dspEngine.setVirtualizerStrength(percent)
        _dspState.update { it.copy(virtualizerPercent = percent, activePresetName = "Custom") }
    }

    fun setDynamicsEnabled(enabled: Boolean) {
        dspEngine.setDynamicsEnabled(enabled)
        _dspState.update { it.copy(dynamicsEnabled = enabled) }
    }

    fun setEqBandLevel(bandIndex: Int, milliBels: Int) {
        dspEngine.setEqualizerBandGain(bandIndex, milliBels)
        val updated = _dspState.value.eqBandLevels.toMutableList()
        if (bandIndex in updated.indices) {
            updated[bandIndex] = milliBels
        }
        _dspState.update { it.copy(eqBandLevels = updated, activePresetName = "Custom") }
    }

    fun applyPreset(preset: DspPresetEntity) {
        val parsedLevels = preset.eqBandsLevels.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        val levels = if (parsedLevels.size == 10) parsedLevels else List(10) { 0 }

        dspEngine.applyPreset(
            levels = levels,
            bassPercent = preset.bassBoostPercent,
            virtualizerPercent = preset.virtualizerPercent,
            dynamics = preset.dynamicsCompressionEnabled
        )

        _dspState.update {
            it.copy(
                bassBoostPercent = preset.bassBoostPercent,
                virtualizerPercent = preset.virtualizerPercent,
                dynamicsEnabled = preset.dynamicsCompressionEnabled,
                eqBandLevels = levels,
                activePresetName = preset.name
            )
        }
    }

    // Visualizer Mode
    fun setVisualizerMode(mode: VisualizerMode) {
        visualizerEngine.setMode(mode)
    }

    private val _searchResults = MutableStateFlow<List<com.example.stream.ResolvedTrackInfo>>(emptyList())
    val searchResults: StateFlow<List<com.example.stream.ResolvedTrackInfo>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun searchYoutube(query: String) {
        if (query.isBlank()) return
        _isSearching.value = true
        viewModelScope.launch {
            val result = com.example.stream.StreamResolver.searchYoutube(query)
            result.onSuccess {
                _searchResults.value = it
            }
            _isSearching.value = false
        }
    }
    
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    // Stream & YouTube Ingestion
    fun setStreamUrlInput(input: String) {
        _streamInputState.update { it.copy(urlInput = input, errorMessage = null) }
    }

    fun setStreamTargetDeck(deckId: String) {
        _streamInputState.update { it.copy(targetDeck = deckId) }
    }

    fun resolveAndLoadStream(
        urlOrUri: String,
        targetDeck: String = _streamInputState.value.targetDeck,
        autoPlay: Boolean = true
    ) {
        if (urlOrUri.isBlank()) return
        _streamInputState.update { it.copy(isResolving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = StreamResolver.resolveStream(getApplication(), urlOrUri)
            result.onSuccess { info ->
                val track = TrackEntity(
                    uri = info.uri,
                    title = info.title,
                    artist = info.artist,
                    durationMs = info.durationMs,
                    thumbnailUrl = info.thumbnailUrl,
                    isRemoteStream = info.isRemoteStream,
                    bpm = info.bpm,
                    keySignature = info.keySignature,
                    bitDepth = "24-bit Hi-Res",
                    sampleRate = "96kHz"
                )
                val trackId = repository.insertTrack(track)
                val savedTrack = track.copy(trackId = trackId)

                audioEngine.loadTrack(targetDeck, savedTrack, autoPlay = autoPlay)
                _streamInputState.update {
                    it.copy(
                        urlInput = "",
                        isResolving = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _streamInputState.update {
                    it.copy(
                        isResolving = false,
                        errorMessage = "Stream resolution failed: ${error.localizedMessage ?: "Network or URL error"}"
                    )
                }
            }
        }
    }

    fun importLocalAudio(uri: Uri) {
        resolveAndLoadStream(uri.toString(), "A", autoPlay = true)
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.updateTrack(track.copy(isFavorite = !track.isFavorite))
        }
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            repository.deleteTrack(track)
            if (dualDeckState.value.deckA.track?.trackId == track.trackId) {
                audioEngine.unloadDeck("A")
            }
            if (dualDeckState.value.deckB.track?.trackId == track.trackId) {
                audioEngine.unloadDeck("B")
            }
        }
    }

    fun setDeckMode(mode: com.example.engine.DeckMode) {
        audioEngine.setDeckMode(mode)
    }

    fun selectSingleDeck(deckId: String) {
        audioEngine.selectSingleDeck(deckId)
    }

    fun exitApp(context: android.content.Context) {
        try {
            audioEngine.stopAllAndRelease()
            AuraPlaybackService.stop(context)
            val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.cancelAll()
        } catch (_: Exception) {}

        if (context is android.app.Activity) {
            context.finishAffinity()
        }
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
