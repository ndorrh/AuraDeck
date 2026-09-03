package com.example.visualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*
import kotlin.random.Random

enum class VisualizerMode(val displayName: String, val description: String) {
    NEON_SPECTRUM("Neon Spectrum", "1024-pt FFT bars with glowing peak gravity"),
    CIRCULAR_WAVE("Radial Pulse", "360-degree bass reactive circular oscilloscope"),
    LISSAJOUS_PHASE("Stereo Goniometer", "Lissajous stereo phase correlation vector"),
    LIQUID_PARTICLES("Liquid Particles", "Dynamic transient particle field")
}

data class VisualizerFrame(
    val fftMagnitudes: FloatArray = FloatArray(64) { 0f },
    val peakMagnitudes: FloatArray = FloatArray(64) { 0f },
    val waveformPoints: FloatArray = FloatArray(128) { 0f },
    val bassEnergy: Float = 0f,
    val midEnergy: Float = 0f,
    val highEnergy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

class VisualizerEngine {

    private var visualizer: Visualizer? = null
    private var activeSessionId: Int = 0

    private val _currentMode = MutableStateFlow(VisualizerMode.NEON_SPECTRUM)
    val currentMode: StateFlow<VisualizerMode> = _currentMode.asStateFlow()

    private val _frame = MutableStateFlow(VisualizerFrame())
    val frame: StateFlow<VisualizerFrame> = _frame.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var animationJob: Job? = null

    // Real-time playback telemetry
    private var isPlaying: Boolean = false
    private var currentBpm: Int = 124
    private var currentVuLevel: Float = 0f
    private var currentPositionMs: Long = 0L

    private var phase: Float = 0f
    private val peaks = FloatArray(64) { 0f }

    init {
        startRenderLoop()
    }

    fun setMode(mode: VisualizerMode) {
        _currentMode.value = mode
    }

    fun updatePlaybackState(
        isPlaying: Boolean,
        bpm: Int = 124,
        vuLevel: Float = 0f,
        positionMs: Long = 0L
    ) {
        this.isPlaying = isPlaying
        this.currentBpm = if (bpm in 40..240) bpm else 124
        this.currentVuLevel = vuLevel.coerceIn(0f, 1f)
        this.currentPositionMs = positionMs
    }

    fun linkAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == activeSessionId) return
        releaseVisualizer()
        this.activeSessionId = audioSessionId

        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1] // Max 1024
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, rate: Int) {
                            if (isPlaying) {
                                waveform?.let { processWaveform(it) }
                            }
                        }

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                            if (isPlaying) {
                                fft?.let { processFft(it) }
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                enabled = true
            }
            Log.d("VisualizerEngine", "Hardware visualizer linked to session $audioSessionId")
        } catch (e: Exception) {
            Log.w("VisualizerEngine", "Hardware visualizer unavailable: ${e.message}. Using beat-synchronized DSP engine.")
        }
    }

    private fun processWaveform(bytes: ByteArray) {
        val points = FloatArray(128)
        val step = maxOf(1, bytes.size / 128)
        for (i in 0 until 128) {
            val byteIndex = (i * step).coerceAtMost(bytes.size - 1)
            points[i] = ((bytes[byteIndex].toInt() and 0xFF) - 128) / 128f
        }
        val current = _frame.value
        _frame.value = current.copy(waveformPoints = points)
    }

    private fun processFft(bytes: ByteArray) {
        val bands = 64
        val magnitudes = FloatArray(bands)
        val step = maxOf(2, (bytes.size / 2) / bands)

        var bassSum = 0f
        var midSum = 0f
        var highSum = 0f

        for (i in 0 until bands) {
            val idx = i * step * 2
            if (idx + 1 < bytes.size) {
                val r = bytes[idx].toFloat()
                val im = bytes[idx + 1].toFloat()
                val mag = sqrt(r * r + im * im) / 128f
                magnitudes[i] = mag.coerceIn(0.01f, 1.0f)

                if (i < 10) bassSum += mag
                else if (i < 35) midSum += mag
                else highSum += mag

                if (magnitudes[i] > peaks[i]) {
                    peaks[i] = magnitudes[i]
                } else {
                    peaks[i] = (peaks[i] - 0.02f).coerceAtLeast(magnitudes[i])
                }
            }
        }

        val bEnergy = (bassSum / 10f).coerceIn(0f, 1.0f)
        val mEnergy = (midSum / 25f).coerceIn(0f, 1.0f)
        val hEnergy = (highSum / 29f).coerceIn(0f, 1.0f)

        _frame.value = _frame.value.copy(
            fftMagnitudes = magnitudes,
            peakMagnitudes = peaks.clone(),
            bassEnergy = bEnergy,
            midEnergy = mEnergy,
            highEnergy = hEnergy,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun startRenderLoop() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            while (isActive) {
                delay(16) // ~60 FPS

                if (!isPlaying) {
                    // Instantly and smoothly decay to flat silence when paused
                    decayToSilence()
                } else {
                    phase += 0.08f
                    if (phase > 2 * PI.toFloat()) phase = 0f

                    // If hardware visualizer is not actively feeding or null, generate beat-synchronized acoustic visual
                    if (visualizer == null || !visualizer!!.enabled) {
                        generateBeatSynchronizedFrame(phase)
                    }
                }
            }
        }
    }

    private fun decayToSilence() {
        val current = _frame.value
        val newFft = FloatArray(64)
        val newPeaks = FloatArray(64)
        val newWave = FloatArray(128)

        var hasRemainingEnergy = false
        for (i in 0 until 64) {
            newFft[i] = (current.fftMagnitudes[i] * 0.70f).let { if (it < 0.005f) 0f else it }
            newPeaks[i] = (peaks[i] * 0.75f).let { if (it < 0.005f) 0f else it }
            peaks[i] = newPeaks[i]
            if (newPeaks[i] > 0f) hasRemainingEnergy = true
        }
        for (i in 0 until 128) {
            newWave[i] = (current.waveformPoints[i] * 0.65f).let { if (abs(it) < 0.005f) 0f else it }
            if (abs(newWave[i]) > 0f) hasRemainingEnergy = true
        }

        val bEnergy = (current.bassEnergy * 0.70f).let { if (it < 0.005f) 0f else it }
        val mEnergy = (current.midEnergy * 0.70f).let { if (it < 0.005f) 0f else it }
        val hEnergy = (current.highEnergy * 0.70f).let { if (it < 0.005f) 0f else it }

        _frame.value = VisualizerFrame(
            fftMagnitudes = newFft,
            peakMagnitudes = newPeaks,
            waveformPoints = newWave,
            bassEnergy = bEnergy,
            midEnergy = mEnergy,
            highEnergy = hEnergy,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun generateBeatSynchronizedFrame(p: Float) {
        val bands = 64
        val mags = FloatArray(bands)
        val waves = FloatArray(128)

        // Calculate exact beat phase from the track's BPM and current playback position
        val beatIntervalMs = (60000.0 / currentBpm).coerceIn(250.0, 1000.0)
        val beatPhase = ((currentPositionMs % beatIntervalMs.toLong()) / beatIntervalMs).toFloat()
        // Sharp transient on downbeat with exponential acoustic decay
        val beatTransient = exp(-beatPhase * 4.2f)
        val amplitude = currentVuLevel.coerceAtLeast(0.35f)

        for (i in 0 until bands) {
            val freq = (i + 1) * 0.18f
            // Low frequencies pulse strongly with the beat transient
            val beatBoost = if (i < 8) beatTransient * 0.65f else beatTransient * 0.20f
            val base = (sin(p * 1.6f + freq) * 0.35f + cos(p * 0.9f - freq * 0.6f) * 0.25f + 0.30f).toFloat()
            val noise = Random.nextFloat() * 0.08f
            val v = ((base + beatBoost + noise) * amplitude).coerceIn(0.02f, 0.98f)
            mags[i] = v

            if (v > peaks[i]) {
                peaks[i] = v
            } else {
                peaks[i] = (peaks[i] - 0.025f).coerceAtLeast(v)
            }
        }

        for (i in 0 until 128) {
            val wavePhase = (i.toFloat() / 128f) * 4 * PI.toFloat() + p * 2f
            val raw = (sin(wavePhase) * 0.55f + sin(wavePhase * 2.2f) * 0.3f + beatTransient * 0.2f * sin(wavePhase * 5f)).toFloat()
            waves[i] = (raw * amplitude).coerceIn(-1.0f, 1.0f)
        }

        val bEnergy = ((mags[0] + mags[1] + mags[2] + mags[3]) / 4f).coerceIn(0f, 1f)
        val mEnergy = ((mags[14] + mags[15] + mags[16]) / 3f).coerceIn(0f, 1f)
        val hEnergy = ((mags[38] + mags[39] + mags[40]) / 3f).coerceIn(0f, 1f)

        _frame.value = VisualizerFrame(
            fftMagnitudes = mags,
            peakMagnitudes = peaks.clone(),
            waveformPoints = waves,
            bassEnergy = bEnergy,
            midEnergy = mEnergy,
            highEnergy = hEnergy,
            timestamp = System.currentTimeMillis()
        )
    }

    fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        activeSessionId = 0
    }

    fun release() {
        animationJob?.cancel()
        coroutineScope.cancel()
        releaseVisualizer()
    }
}
