package com.example.dsp

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log

class AudioEngineDsp(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null
    private var bassBoost: BassBoost? = null
    private var dynamics: DynamicsProcessing? = null

    private var activeSessionId: Int = 0

    // DSP State Cache
    var isEnabled: Boolean = true
        private set

    var bassBoostPercent: Float = 0.40f
        private set

    var virtualizerPercent: Float = 0.70f
        private set

    var dynamicsEnabled: Boolean = true
        private set

    // Cached band levels in milliBels (-1200 to +1200 mB typical, i.e. -12dB to +12dB)
    val bandLevels = IntArray(10) { 0 }

    // Standard 10 audiophile frequency labels (in Hz/kHz)
    val standardFrequencyLabels = listOf(
        "31Hz", "62Hz", "125Hz", "250Hz", "500Hz",
        "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    )

    fun bindSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == activeSessionId) return
        releaseEffects()
        this.activeSessionId = audioSessionId

        try {
            // 1. Equalizer Initialization
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = this@AudioEngineDsp.isEnabled
                applyCurrentBandLevels()
            }
            Log.d("AudioEngineDsp", "Equalizer attached. Bands: ${equalizer?.numberOfBands}")
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Equalizer unavailable: ${e.message}")
        }

        try {
            // 2. Headset 3D Surround Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = this@AudioEngineDsp.isEnabled
                if (strengthSupported) {
                    setStrength((virtualizerPercent * 1000).toInt().toShort())
                }
            }
            Log.d("AudioEngineDsp", "Virtualizer attached")
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Virtualizer unavailable: ${e.message}")
        }

        try {
            // 3. Harmonic Bass Booster / Exciter
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = this@AudioEngineDsp.isEnabled
                if (strengthSupported) {
                    setStrength((bassBoostPercent * 1000).toInt().toShort())
                }
            }
            Log.d("AudioEngineDsp", "BassBoost attached")
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "BassBoost unavailable: ${e.message}")
        }

        try {
            // 4. Dynamics Processing (Limiter & Compressor) on Android 9+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,      // 2 channels stereo
                    true,   // eqIn
                    10,     // eqInBands
                    true,   // mbc
                    4,      // mbcBands
                    true,   // eqOut
                    10,     // eqOutBands
                    true    // limiter
                ).build()

                dynamics = DynamicsProcessing(0, audioSessionId, config).apply {
                    enabled = this@AudioEngineDsp.isEnabled && this@AudioEngineDsp.dynamicsEnabled
                }
                Log.d("AudioEngineDsp", "DynamicsProcessing attached")
            }
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "DynamicsProcessing unavailable: ${e.message}")
        }
    }

    fun setMasterDspEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        try {
            equalizer?.enabled = enabled
            virtualizer?.enabled = enabled
            bassBoost?.enabled = enabled
            dynamics?.enabled = enabled && dynamicsEnabled
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Error toggling DSP: ${e.message}")
        }
    }

    fun setEqualizerBandGain(bandIndex: Int, milliBels: Int) {
        if (bandIndex in bandLevels.indices) {
            bandLevels[bandIndex] = milliBels
        }
        try {
            equalizer?.let { eq ->
                val totalBands = eq.numberOfBands.toInt()
                if (bandIndex < totalBands) {
                    val range = eq.bandLevelRange
                    val clamped = milliBels.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                    eq.setBandLevel(bandIndex.toShort(), clamped)
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Error setting EQ band: ${e.message}")
        }
    }

    private fun applyCurrentBandLevels() {
        equalizer?.let { eq ->
            val totalBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            for (i in 0 until minOf(totalBands, bandLevels.size)) {
                val clamped = bandLevels[i].coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                eq.setBandLevel(i.toShort(), clamped)
            }
        }
    }

    fun setVirtualizerStrength(strengthFraction: Float) {
        this.virtualizerPercent = strengthFraction.coerceIn(0f, 1f)
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength((virtualizerPercent * 1000).toInt().toShort())
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Error setting virtualizer: ${e.message}")
        }
    }

    fun setBassBoostStrength(strengthFraction: Float) {
        this.bassBoostPercent = strengthFraction.coerceIn(0f, 1f)
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength((bassBoostPercent * 1000).toInt().toShort())
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Error setting bass boost: ${e.message}")
        }
    }

    fun setDynamicsEnabled(enabled: Boolean) {
        this.dynamicsEnabled = enabled
        try {
            dynamics?.enabled = this.isEnabled && enabled
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Error setting dynamics: ${e.message}")
        }
    }

    fun applyPreset(levels: List<Int>, bassPercent: Float, virtualizerPercent: Float, dynamics: Boolean) {
        setBassBoostStrength(bassPercent)
        setVirtualizerStrength(virtualizerPercent)
        setDynamicsEnabled(dynamics)
        levels.forEachIndexed { index, level ->
            setEqualizerBandGain(index, level)
        }
    }

    fun releaseEffects() {
        try {
            equalizer?.release()
            virtualizer?.release()
            bassBoost?.release()
            dynamics?.release()
        } catch (e: Exception) {
            Log.w("AudioEngineDsp", "Error releasing effects: ${e.message}")
        } finally {
            equalizer = null
            virtualizer = null
            bassBoost = null
            dynamics = null
            activeSessionId = 0
        }
    }
}
