package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dsp_presets")
data class DspPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bassBoostPercent: Float,      // 0.0f to 1.0f
    val virtualizerPercent: Float,    // 0.0f to 1.0f (3D Surround HRTF)
    val eqBandsLevels: String,        // Comma-separated millibels for 10 bands e.g. "300,200,0,-100,..."
    val dynamicsCompressionEnabled: Boolean = true,
    val isCustom: Boolean = false
)
