package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val trackId: Long = 0,
    val uri: String,
    val isRemoteStream: Boolean = true,
    val title: String,
    val artist: String,
    val durationMs: Long = 0,
    val thumbnailUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val bpm: Int = 120,
    val keySignature: String = "C min",
    val bitDepth: String = "24-bit",
    val sampleRate: String = "96kHz"
)
