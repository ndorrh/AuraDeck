package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuraDeckDao
import com.example.data.model.DspPresetEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        DspPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AuraDeckDatabase : RoomDatabase() {
    abstract fun auraDeckDao(): AuraDeckDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDeckDatabase? = null

        fun getInstance(context: Context): AuraDeckDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDeckDatabase::class.java,
                    "auradeck_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.auraDeckDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: AuraDeckDao) {
                // Initial Default Presets
                val presets = listOf(
                    DspPresetEntity(
                        name = "Audiophile Clarity",
                        bassBoostPercent = 0.35f,
                        virtualizerPercent = 0.70f,
                        eqBandsLevels = "150,100,50,0,50,100,200,300,350,400",
                        dynamicsCompressionEnabled = true
                    ),
                    DspPresetEntity(
                        name = "Deep Bass Exciter",
                        bassBoostPercent = 0.90f,
                        virtualizerPercent = 0.50f,
                        eqBandsLevels = "600,500,350,150,0,-50,-50,50,100,150",
                        dynamicsCompressionEnabled = true
                    ),
                    DspPresetEntity(
                        name = "3D Holographic Headset",
                        bassBoostPercent = 0.40f,
                        virtualizerPercent = 0.95f,
                        eqBandsLevels = "200,100,50,0,-50,100,250,400,450,500",
                        dynamicsCompressionEnabled = true
                    ),
                    DspPresetEntity(
                        name = "Club & Electronic",
                        bassBoostPercent = 0.75f,
                        virtualizerPercent = 0.60f,
                        eqBandsLevels = "450,400,200,0,-100,100,250,350,400,450",
                        dynamicsCompressionEnabled = true
                    ),
                    DspPresetEntity(
                        name = "Vocal Intimacy",
                        bassBoostPercent = 0.20f,
                        virtualizerPercent = 0.40f,
                        eqBandsLevels = "-150,-100,50,200,400,450,300,150,50,0",
                        dynamicsCompressionEnabled = true
                    ),
                    DspPresetEntity(
                        name = "Acoustic / Classical",
                        bassBoostPercent = 0.25f,
                        virtualizerPercent = 0.65f,
                        eqBandsLevels = "100,50,0,0,100,150,200,250,300,350",
                        dynamicsCompressionEnabled = false
                    ),
                    DspPresetEntity(
                        name = "Flat Reference Studio",
                        bassBoostPercent = 0.0f,
                        virtualizerPercent = 0.0f,
                        eqBandsLevels = "0,0,0,0,0,0,0,0,0,0",
                        dynamicsCompressionEnabled = false
                    )
                )
                for (p in presets) {
                    dao.insertDspPreset(p)
                }

                // Initial Playlists
                val p1Id = dao.insertPlaylist(
                    PlaylistEntity(
                        name = "Audiophile Master Collection",
                        description = "High-fidelity lossless and binaural reference tracks",
                        coverIcon = "headphones"
                    )
                )
                val p2Id = dao.insertPlaylist(
                    PlaylistEntity(
                        name = "Cyberpunk & Club Beats",
                        description = "High-energy synthwave and dual-deck club tracks",
                        coverIcon = "disc"
                    )
                )

                // Initial Demo High-Fidelity Audio Tracks (Direct reliable CDN audio streams)
                val sampleTracks = listOf(
                    TrackEntity(
                        uri = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
                        title = "Midnight Cyber Lounge",
                        artist = "Aura Audiophile Lab",
                        durationMs = 148000,
                        thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
                        isFavorite = true,
                        bpm = 92,
                        keySignature = "F# min",
                        bitDepth = "24-bit",
                        sampleRate = "96kHz"
                    ),
                    TrackEntity(
                        uri = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=electronic-future-beats-117997.mp3",
                        title = "Neon Velocity (Dual Deck Mix)",
                        artist = "Kinetics Syndicate",
                        durationMs = 156000,
                        thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
                        isFavorite = true,
                        bpm = 128,
                        keySignature = "A min",
                        bitDepth = "24-bit",
                        sampleRate = "192kHz"
                    ),
                    TrackEntity(
                        uri = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=ambient-piano-amp-strings-10711.mp3",
                        title = "Binaural Horizons (Spatial HRTF)",
                        artist = "Elena Rostova",
                        durationMs = 184000,
                        thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
                        isFavorite = false,
                        bpm = 78,
                        keySignature = "D maj",
                        bitDepth = "32-bit Float",
                        sampleRate = "192kHz"
                    ),
                    TrackEntity(
                        uri = "https://cdn.pixabay.com/download/audio/2021/09/06/audio_82c61e3895.mp3?filename=tuesday-glitch-12278.mp3",
                        title = "Quantum Pulse Sub-Bass",
                        artist = "Zero-G Audio",
                        durationMs = 132000,
                        thumbnailUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=400",
                        isFavorite = false,
                        bpm = 140,
                        keySignature = "C min",
                        bitDepth = "24-bit",
                        sampleRate = "96kHz"
                    )
                )

                sampleTracks.forEachIndexed { index, track ->
                    val tId = dao.insertTrack(track)
                    if (index % 2 == 0) {
                        dao.insertPlaylistCrossRef(PlaylistTrackCrossRef(p1Id, tId, index))
                    }
                    dao.insertPlaylistCrossRef(PlaylistTrackCrossRef(p2Id, tId, index))
                }
            }
        }
    }
}
