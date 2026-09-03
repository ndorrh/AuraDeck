package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.TrackEntity
import com.example.dsp.AudioEngineDsp
import com.example.stream.StreamResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.*
import kotlin.random.Random

data class DeckUiState(
    val deckId: String, // "A" or "B"
    val track: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val isLoading: Boolean = false,
    val isBuffering: Boolean = false,
    val bufferPercent: Int = 0,
    val statusText: String? = null,
    val isYoutubeTrack: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isLooping: Boolean = false,
    val cuePositionMs: Long = 0L,
    val vuLevelL: Float = 0f,
    val vuLevelR: Float = 0f,
    val pitchBendPercent: Float = 0f
)

enum class DeckMode {
    SINGLE_DECK,
    DUAL_DECK
}

data class DualDeckUiState(
    val deckA: DeckUiState = DeckUiState(deckId = "A"),
    val deckB: DeckUiState = DeckUiState(deckId = "B"),
    val crossfaderPosition: Float = 0.5f, // 0.0 = 100% A, 1.0 = 100% B
    val masterVolume: Float = 1.0f,
    val activeAudioSessionId: Int = 0,
    val deckMode: DeckMode = DeckMode.SINGLE_DECK,
    val selectedSingleDeck: String = "A" // "A" or "B"
)

class DualDeckAudioEngine(
    private val context: Context,
    val dspEngine: AudioEngineDsp
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var playerA: MediaPlayer? = null
    private var playerB: MediaPlayer? = null

    private val _uiState = MutableStateFlow(DualDeckUiState())
    val uiState: StateFlow<DualDeckUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        initializePlayers()
        startPlaybackTicker()
    }

    private fun initializePlayers() {
        playerA = createConfiguredMediaPlayer("A")
        playerB = createConfiguredMediaPlayer("B")
    }

    private fun createConfiguredMediaPlayer(deckId: String, autoPlay: Boolean = false): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { mp ->
                val duration = mp.duration.toLong().coerceAtLeast(0L)
                updateDeck(deckId) {
                    it.copy(
                        isPrepared = true,
                        isLoading = false,
                        isBuffering = false,
                        isPlaying = autoPlay,
                        durationMs = duration,
                        statusText = null
                    )
                }
                if (autoPlay) {
                    mp.start()
                }
                recalculateVolumes()
                val sessionId = mp.audioSessionId
                _uiState.update { it.copy(activeAudioSessionId = sessionId) }
                dspEngine.bindSession(sessionId)
            }
            setOnBufferingUpdateListener { _, percent ->
                updateDeck(deckId) { it.copy(bufferPercent = percent) }
            }
            setOnCompletionListener {
                val looping = if (deckId == "A") _uiState.value.deckA.isLooping else _uiState.value.deckB.isLooping
                if (!looping) {
                    updateDeck(deckId) { it.copy(isPlaying = false, currentPositionMs = 0L) }
                }
            }
            setOnErrorListener { _, what, extra ->
                Log.e("DualDeckEngine", "MediaPlayer error on Deck $deckId: what=$what extra=$extra")
                updateDeck(deckId) { it.copy(isLoading = false, isBuffering = false, isPlaying = false, statusText = "Player error") }
                true
            }
        }
    }

    fun loadTrack(deckId: String, track: TrackEntity, autoPlay: Boolean = true) {
        val isYoutube = track.uri.startsWith("youtube:") || track.uri.startsWith("yt:") || StreamResolver.isYoutubeUrl(track.uri)

        if (deckId == "A") {
            try { playerA?.release() } catch (_: Exception) {}
            playerA = createConfiguredMediaPlayer("A", autoPlay)
        } else {
            try { playerB?.release() } catch (_: Exception) {}
            playerB = createConfiguredMediaPlayer("B", autoPlay)
        }
        val player = if (deckId == "A") playerA else playerB

        updateDeck(deckId) {
            it.copy(
                track = track,
                isLoading = true,
                isBuffering = true,
                bufferPercent = 0,
                statusText = if (isYoutube) "Extracting YouTube audio..." else "Loading audio...",
                isPrepared = false,
                isYoutubeTrack = isYoutube,
                currentPositionMs = 0L
            )
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val p = player ?: return@launch
                var finalUrl = track.uri

                if (isYoutube) {
                    val videoId = if (track.uri.startsWith("youtube:")) {
                        track.uri.removePrefix("youtube:")
                    } else if (track.uri.startsWith("yt:")) {
                        track.uri.removePrefix("yt:")
                    } else {
                        StreamResolver.extractYoutubeVideoId(track.uri) ?: track.uri
                    }
                    val ytUrl = "https://www.youtube.com/watch?v=$videoId"
                    
                    val request = com.yausername.youtubedl_android.YoutubeDLRequest(ytUrl)
                    request.addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
                    val streamInfo = com.yausername.youtubedl_android.YoutubeDL.getInstance().getInfo(request)
                    finalUrl = streamInfo.url ?: throw Exception("Failed to extract audio URL")
                    
                    withContext(Dispatchers.Main) {
                        updateDeck(deckId) { it.copy(statusText = "Buffering audio...") }
                    }
                }

                if (finalUrl.startsWith("content://") || finalUrl.startsWith("file://")) {
                    p.setDataSource(context, Uri.parse(finalUrl))
                } else {
                    p.setDataSource(finalUrl)
                }
                p.prepareAsync()
            } catch (e: Exception) {
                Log.e("DualDeckEngine", "Error loading track to Deck $deckId: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    updateDeck(deckId) { it.copy(isLoading = false, isBuffering = false, statusText = "Failed to load audio") }
                }
            }
        }
    }

    fun togglePlayPause(deckId: String) {
        val currentDeckState = if (deckId == "A") _uiState.value.deckA else _uiState.value.deckB


        val player = (if (deckId == "A") playerA else playerB) ?: return

        if (!currentDeckState.isPrepared) {
            currentDeckState.track?.let { loadTrack(deckId, it, autoPlay = true) }
            return
        }

        if (player.isPlaying) {
            player.pause()
            updateDeck(deckId) { it.copy(isPlaying = false) }
        } else {
            player.start()
            updateDeck(deckId) { it.copy(isPlaying = true) }
            val sessionId = player.audioSessionId
            _uiState.update { it.copy(activeAudioSessionId = sessionId) }
            dspEngine.bindSession(sessionId)
        }
        recalculateVolumes()
    }

    fun seekTo(deckId: String, positionMs: Long) {
        val currentDeckState = if (deckId == "A") _uiState.value.deckA else _uiState.value.deckB


        val player = (if (deckId == "A") playerA else playerB) ?: return
        try {
            player.seekTo(positionMs.toInt())
            updateDeck(deckId) { it.copy(currentPositionMs = positionMs) }
        } catch (e: Exception) {
            Log.w("DualDeckEngine", "Seek failed: ${e.message}")
        }
    }

    fun setCrossfader(position: Float) {
        val clamped = position.coerceIn(0f, 1f)
        _uiState.update { it.copy(crossfaderPosition = clamped) }
        recalculateVolumes()
    }

    fun setMasterVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(masterVolume = clamped) }
        recalculateVolumes()
    }

    fun setDeckVolume(deckId: String, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        updateDeck(deckId) { it.copy(volume = clamped) }
        recalculateVolumes()
    }

    fun setDeckSpeed(deckId: String, speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        val currentDeckState = if (deckId == "A") _uiState.value.deckA else _uiState.value.deckB
        updateDeck(deckId) { it.copy(playbackSpeed = clamped) }


        val player = if (deckId == "A") playerA else playerB
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && player != null) {
            try {
                if (player.isPlaying || _uiState.value.let { if (deckId == "A") it.deckA.isPrepared else it.deckB.isPrepared }) {
                    val params = PlaybackParams().apply { this.speed = clamped }
                    player.playbackParams = params
                }
            } catch (e: Exception) {
                Log.w("DualDeckEngine", "Playback speed adjustment failed: ${e.message}")
            }
        }
    }

    fun toggleLoop(deckId: String) {
        val isLooping = if (deckId == "A") !_uiState.value.deckA.isLooping else !_uiState.value.deckB.isLooping
        val currentDeckState = if (deckId == "A") _uiState.value.deckA else _uiState.value.deckB

        if (!currentDeckState.isYoutubeTrack) {
            val player = if (deckId == "A") playerA else playerB
            player?.isLooping = isLooping
        }
        updateDeck(deckId) { it.copy(isLooping = isLooping) }
    }

    fun setCue(deckId: String) {
        val currentPos = if (deckId == "A") _uiState.value.deckA.currentPositionMs else _uiState.value.deckB.currentPositionMs
        updateDeck(deckId) { it.copy(cuePositionMs = currentPos) }
    }

    fun jumpToCue(deckId: String) {
        val cuePos = if (deckId == "A") _uiState.value.deckA.cuePositionMs else _uiState.value.deckB.cuePositionMs
        seekTo(deckId, cuePos)
    }

    fun syncBpm(targetDeckId: String) {
        val sourceDeck = if (targetDeckId == "A") _uiState.value.deckB else _uiState.value.deckA
        val targetDeck = if (targetDeckId == "A") _uiState.value.deckA else _uiState.value.deckB
        val sourceBpm = sourceDeck.track?.bpm ?: 120
        val targetBpm = targetDeck.track?.bpm ?: 120

        if (targetBpm > 0) {
            val ratio = sourceBpm.toFloat() / targetBpm.toFloat()
            setDeckSpeed(targetDeckId, ratio.coerceIn(0.8f, 1.25f))
        }
    }

    fun unloadDeck(deckId: String) {
        if (deckId == "A") {
            try { playerA?.stop() } catch (_: Exception) {}
            ytPlayerA?.stop()
            updateDeck("A") { DeckUiState(deckId = "A") }
        } else {
            try { playerB?.stop() } catch (_: Exception) {}
            ytPlayerB?.stop()
            updateDeck("B") { DeckUiState(deckId = "B") }
        }
    }

    fun setDeckMode(mode: DeckMode) {
        _uiState.update { it.copy(deckMode = mode) }
        recalculateVolumes()
    }

    fun selectSingleDeck(deckId: String) {
        _uiState.update { it.copy(selectedSingleDeck = deckId) }
        recalculateVolumes()
    }

    fun getOrCreateBridgeView(ctx: Context, deckId: String): android.view.View {
        val bridge = if (deckId == "A") ytPlayerA else ytPlayerB
        return bridge?.getOrCreateWebView(ctx) ?: android.view.View(ctx)
    }

    /**
     * Volume curve calculations:
     * In SINGLE_DECK mode, the selected deck gets full master output (1.0x master) without crossfade attenuation.
     * In DUAL_DECK mode, Constant-Power Crossfade curves apply:
     * GainA = cos(x * PI / 2)
     * GainB = sin(x * PI / 2)
     */
    private fun recalculateVolumes() {
        val mode = _uiState.value.deckMode
        val crossPos = _uiState.value.crossfaderPosition
        val master = _uiState.value.masterVolume

        val (gainA, gainB) = if (mode == DeckMode.SINGLE_DECK) {
            val selected = _uiState.value.selectedSingleDeck
            if (selected == "A") {
                Pair(_uiState.value.deckA.volume * master, 0f)
            } else {
                Pair(0f, _uiState.value.deckB.volume * master)
            }
        } else {
            Pair(
                cos(crossPos * (PI / 2.0)).toFloat() * _uiState.value.deckA.volume * master,
                sin(crossPos * (PI / 2.0)).toFloat() * _uiState.value.deckB.volume * master
            )
        }

        try {
            playerA?.setVolume(gainA, gainA)
            playerB?.setVolume(gainB, gainB)
        } catch (e: Exception) {
            Log.w("DualDeckEngine", "Error setting volume: ${e.message}")
        }


    }

    fun stopAllAndRelease() {
        try {
            if (playerA?.isPlaying == true) playerA?.stop()
            if (playerB?.isPlaying == true) playerB?.stop()

        } catch (_: Exception) {}
        release()
    }

    private fun startPlaybackTicker() {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch {
            while (isActive) {
                delay(60) // ~16 updates per second for smooth scrubbers and VU meters
                updatePlaybackPositionsAndVu()
            }
        }
    }

    private fun updatePlaybackPositionsAndVu() {
        val state = _uiState.value

        // Deck A
        if (state.deckA.isPlaying) {
            val baseLevel = (state.deckA.volume * cos(state.crossfaderPosition * (PI / 2.0)).toFloat() * state.masterVolume).coerceIn(0f, 1f)
            val jitterL = (Random.nextFloat() * 0.18f) - 0.09f
            val jitterR = (Random.nextFloat() * 0.18f) - 0.09f
            val vuL = (baseLevel * 0.85f + jitterL).coerceIn(0.05f, 0.98f)
            val vuR = (baseLevel * 0.85f + jitterR).coerceIn(0.05f, 0.98f)

            if (playerA != null) {
                try {
                    val pos = playerA!!.currentPosition.toLong()
                    val dur = playerA!!.duration.toLong().coerceAtLeast(1L)
                    updateDeck("A") { it.copy(currentPositionMs = pos, durationMs = dur, vuLevelL = vuL, vuLevelR = vuR) }
                } catch (_: Exception) {}
            }
        } else {
            if (state.deckA.vuLevelL > 0f) {
                updateDeck("A") { it.copy(vuLevelL = (it.vuLevelL * 0.8f).coerceAtLeast(0f), vuLevelR = (it.vuLevelR * 0.8f).coerceAtLeast(0f)) }
            }
        }

        // Deck B
        if (state.deckB.isPlaying) {
            val baseLevel = (state.deckB.volume * sin(state.crossfaderPosition * (PI / 2.0)).toFloat() * state.masterVolume).coerceIn(0f, 1f)
            val jitterL = (Random.nextFloat() * 0.18f) - 0.09f
            val jitterR = (Random.nextFloat() * 0.18f) - 0.09f
            val vuL = (baseLevel * 0.85f + jitterL).coerceIn(0.05f, 0.98f)
            val vuR = (baseLevel * 0.85f + jitterR).coerceIn(0.05f, 0.98f)

            if (playerB != null) {
                try {
                    val pos = playerB!!.currentPosition.toLong()
                    val dur = playerB!!.duration.toLong().coerceAtLeast(1L)
                    updateDeck("B") { it.copy(currentPositionMs = pos, durationMs = dur, vuLevelL = vuL, vuLevelR = vuR) }
                } catch (_: Exception) {}
            }
        } else {
            if (state.deckB.vuLevelL > 0f) {
                updateDeck("B") { it.copy(vuLevelL = (it.vuLevelL * 0.8f).coerceAtLeast(0f), vuLevelR = (it.vuLevelR * 0.8f).coerceAtLeast(0f)) }
            }
        }
    }

    private inline fun updateDeck(deckId: String, crossinline transform: (DeckUiState) -> DeckUiState) {
        _uiState.update { current ->
            if (deckId == "A") {
                current.copy(deckA = transform(current.deckA))
            } else {
                current.copy(deckB = transform(current.deckB))
            }
        }
    }

    fun release() {
        tickerJob?.cancel()
        coroutineScope.cancel()
        try {
            playerA?.stop()
            playerA?.release()
            playerB?.stop()
            playerB?.release()
            ytPlayerA?.release()
            ytPlayerB?.release()
        } catch (_: Exception) {}
        playerA = null
        playerB = null
        ytPlayerA = null
        ytPlayerB = null
        dspEngine.releaseEffects()
    }
}
