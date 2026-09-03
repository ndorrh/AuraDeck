package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.example.AuraDeckApp
import com.example.MainActivity
import com.example.R

class AuraPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "auradeck_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.auradeck.ACTION_PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.example.auradeck.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.example.auradeck.ACTION_NEXT"
        const val ACTION_STOP = "com.example.auradeck.ACTION_STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_POSITION_MS = "extra_position_ms"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"

        fun update(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            positionMs: Long = 0L,
            durationMs: Long = 0L,
            thumbnailUrl: String? = null
        ) {
            val intent = Intent(context, AuraPlaybackService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_POSITION_MS, positionMs)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_THUMBNAIL_URL, thumbnailUrl)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, AuraPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var mediaSession: MediaSessionCompat

    private var currentTitle: String = "AuraDeck Audiophile"
    private var currentArtist: String = "Dual-Deck Active"
    private var isPlayingState: Boolean = false
    private var currentPosMs: Long = 0L
    private var currentDurMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AuraDeck:PlaybackWakeLock").apply {
            setReferenceCounted(false)
        }

        initMediaSession()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "AuraDeckMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    handlePlayPause()
                }

                override fun onPause() {
                    handlePlayPause()
                }

                override fun onSkipToNext() {
                    handleNext()
                }

                override fun onSkipToPrevious() {
                    handlePrevious()
                }

                override fun onSeekTo(pos: Long) {
                    try {
                        val engine = AuraDeckApp.instance.audioEngine
                        val activeDeckId = if (engine.uiState.value.deckB.isPlaying) "B" else "A"
                        engine.seekTo(activeDeckId, pos)
                    } catch (_: Exception) {}
                }

                override fun onStop() {
                    handleStop()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_PLAY_PAUSE -> {
                handlePlayPause()
                return START_STICKY
            }
            ACTION_PREVIOUS -> {
                handlePrevious()
                return START_STICKY
            }
            ACTION_NEXT -> {
                handleNext()
                return START_STICKY
            }
            ACTION_STOP -> {
                handleStop()
                return START_NOT_STICKY
            }
        }

        currentTitle = intent?.getStringExtra(EXTRA_TITLE) ?: currentTitle
        currentArtist = intent?.getStringExtra(EXTRA_ARTIST) ?: currentArtist
        isPlayingState = intent?.getBooleanExtra(EXTRA_IS_PLAYING, isPlayingState) ?: isPlayingState
        currentPosMs = intent?.getLongExtra(EXTRA_POSITION_MS, currentPosMs) ?: currentPosMs
        currentDurMs = intent?.getLongExtra(EXTRA_DURATION_MS, currentDurMs) ?: currentDurMs

        if (isPlayingState) {
            if (wakeLock?.isHeld == false) wakeLock?.acquire(2 * 60 * 60 * 1000L)
        } else {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }

        syncMediaSession()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun handlePlayPause() {
        try {
            val engine = AuraDeckApp.instance.audioEngine
            val activeDeckId = if (engine.uiState.value.deckB.isPlaying) "B" else "A"
            engine.togglePlayPause(activeDeckId)
            val isNowPlaying = if (activeDeckId == "A") engine.uiState.value.deckA.isPlaying else engine.uiState.value.deckB.isPlaying
            isPlayingState = isNowPlaying
            syncMediaSession()
            val notification = buildNotification()
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun handlePrevious() {
        try {
            val engine = AuraDeckApp.instance.audioEngine
            val activeDeckId = if (engine.uiState.value.deckB.isPlaying) "B" else "A"
            engine.seekTo(activeDeckId, 0L)
        } catch (_: Exception) {}
    }

    private fun handleNext() {
        try {
            val app = AuraDeckApp.instance
            val engine = app.audioEngine
            val activeDeckId = if (engine.uiState.value.deckB.isPlaying) "B" else "A"
            // Jump to cue or restart
            engine.jumpToCue(activeDeckId)
        } catch (_: Exception) {}
    }

    private fun handleStop() {
        try {
            val engine = AuraDeckApp.instance.audioEngine
            if (engine.uiState.value.deckA.isPlaying) engine.togglePlayPause("A")
            if (engine.uiState.value.deckB.isPlaying) engine.togglePlayPause("B")
        } catch (_: Exception) {}
        mediaSession.isActive = false
        if (wakeLock?.isHeld == true) wakeLock?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun syncMediaSession() {
        val state = if (isPlayingState) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, currentPosMs, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "AuraDeck Audiophile")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDurMs)
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AuraPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AuraPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AuraPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, AuraPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlayingState) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val playPauseText = if (isPlayingState) "Pause" else "Play"

        val mediaStyle = MediaNotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText("AuraDeck Dual-Deck")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setStyle(mediaStyle)
            .addAction(R.drawable.ic_skip_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseText, playPausePendingIntent)
            .addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent)
            .addAction(R.drawable.ic_close, "Stop", stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlayingState)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AuraDeck Playback Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Uninterrupted lock screen & notification media controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            mediaSession.release()
        } catch (_: Exception) {}
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
