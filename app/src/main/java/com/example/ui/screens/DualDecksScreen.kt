package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DeckMode
import com.example.engine.DeckUiState
import com.example.ui.AuraDeckViewModel
import com.example.ui.MainTab
import com.example.ui.components.*
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import com.example.ui.theme.DspSurroundViolet
import com.example.ui.theme.MatrixGreen

@Composable
fun DualDecksScreen(
    viewModel: AuraDeckViewModel,
    modifier: Modifier = Modifier
) {
    val dualDeckState by viewModel.dualDeckState.collectAsState()
    val visualizerFrame by viewModel.visualizerFrame.collectAsState()
    val visualizerMode by viewModel.visualizerMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080B11))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector: 1 DECK (Default) vs 2 DECKS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Console Mode",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DECK CONFIGURATION",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF101625),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283C))
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = dualDeckState.deckMode == DeckMode.SINGLE_DECK,
                        onClick = { viewModel.setDeckMode(DeckMode.SINGLE_DECK) },
                        label = {
                            Text(
                                "1 DECK (SOLO)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeckACyan,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Transparent,
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp).testTag("mode_single_deck")
                    )

                    FilterChip(
                        selected = dualDeckState.deckMode == DeckMode.DUAL_DECK,
                        onClick = { viewModel.setDeckMode(DeckMode.DUAL_DECK) },
                        label = {
                            Text(
                                "2 DECKS (DJ)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeckBAmber,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Transparent,
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp).testTag("mode_dual_deck")
                    )
                }
            }
        }

        // Mini Live Visualizer Header Window
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .clickable { viewModel.selectTab(MainTab.VISUALIZER) }
        ) {
            VisualizerSurface(
                frame = visualizerFrame,
                mode = visualizerMode,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MatrixGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DSP LIVE: ${visualizerMode.displayName.uppercase()}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Expand Visualizer",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Console Layout based on Selected Mode
        if (dualDeckState.deckMode == DeckMode.SINGLE_DECK) {
            // SINGLE DECK (SOLO AUDIOPHILE PLAYER)
            val activeDeckId = dualDeckState.selectedSingleDeck
            val activeDeckState = if (activeDeckId == "A") dualDeckState.deckA else dualDeckState.deckB
            val activeAccent = if (activeDeckId == "A") DeckACyan else DeckBAmber

            SingleDeckView(
                deckState = activeDeckState,
                deckId = activeDeckId,
                accentColor = activeAccent,
                onSwitchDeck = { newDeckId -> viewModel.selectSingleDeck(newDeckId) },
                onPlayPause = { viewModel.togglePlayPause(activeDeckId) },
                onSeek = { pos -> viewModel.seekTo(activeDeckId, pos) },
                onSpeedChanged = { speed -> viewModel.setDeckSpeed(activeDeckId, speed) },
                onLoop = { viewModel.toggleLoop(activeDeckId) },
                onCue = { viewModel.setCue(activeDeckId) },
                onJumpCue = { viewModel.jumpToCue(activeDeckId) },
                onVolumeChanged = { vol -> viewModel.setDeckVolume(activeDeckId, vol) },
                onMasterVolumeChanged = { vol -> viewModel.setMasterVolume(vol) },
                masterVolume = dualDeckState.masterVolume,
                onOpenLibrary = { viewModel.selectTab(MainTab.PLAYLISTS) }
            )
        } else {
            // DUAL DECKS (PRO DJ CONSOLE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DECK A
                DeckCard(
                    deckState = dualDeckState.deckA,
                    deckId = "A",
                    accentColor = DeckACyan,
                    onPlayPause = { viewModel.togglePlayPause("A") },
                    onSeek = { pos -> viewModel.seekTo("A", pos) },
                    onSpeedChanged = { speed -> viewModel.setDeckSpeed("A", speed) },
                    onLoop = { viewModel.toggleLoop("A") },
                    onCue = { viewModel.setCue("A") },
                    onJumpCue = { viewModel.jumpToCue("A") },
                    onSync = { viewModel.syncBpm("A") },
                    modifier = Modifier.weight(1f)
                )

                // DECK B
                DeckCard(
                    deckState = dualDeckState.deckB,
                    deckId = "B",
                    accentColor = DeckBAmber,
                    onPlayPause = { viewModel.togglePlayPause("B") },
                    onSeek = { pos -> viewModel.seekTo("B", pos) },
                    onSpeedChanged = { speed -> viewModel.setDeckSpeed("B", speed) },
                    onLoop = { viewModel.toggleLoop("B") },
                    onCue = { viewModel.setCue("B") },
                    onJumpCue = { viewModel.jumpToCue("B") },
                    onSync = { viewModel.syncBpm("B") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Center Mixer Console: VU Meters & Crossfader
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101624)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // VU Meters & Master Level
                    Row(
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StereoVuMeter(
                            levelL = dualDeckState.deckA.vuLevelL,
                            levelR = dualDeckState.deckA.vuLevelR,
                            label = "CH A"
                        )

                        // Master Volume Dial
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AudiophileRotaryDial(
                                label = "MASTER VOL",
                                valueFraction = dualDeckState.masterVolume,
                                onValueChanged = { viewModel.setMasterVolume(it) },
                                activeColor = DspSurroundViolet
                            )
                        }

                        StereoVuMeter(
                            levelL = dualDeckState.deckB.vuLevelL,
                            levelR = dualDeckState.deckB.vuLevelR,
                            label = "CH B"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hardware Crossfader
                    AudiophileCrossfader(
                        position = dualDeckState.crossfaderPosition,
                        onPositionChanged = { viewModel.setCrossfader(it) }
                    )
                }
            }
        }
    }
}

/**
 * High-end Single Deck View for immersive solo listening and audio manipulation.
 */
@Composable
private fun SingleDeckView(
    deckState: DeckUiState,
    deckId: String,
    accentColor: Color,
    onSwitchDeck: (String) -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onLoop: () -> Unit,
    onCue: () -> Unit,
    onJumpCue: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onMasterVolumeChanged: (Float) -> Unit,
    masterVolume: Float,
    onOpenLibrary: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111728)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("single_deck_console")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Channel Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DECK $deckId",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Switch Channel Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "A",
                            color = if (deckId == "A") DeckACyan else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (deckId == "A") Color(0xFF1E293B) else Color.Transparent)
                                .clickable { onSwitchDeck("A") }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "B",
                            color = if (deckId == "B") DeckBAmber else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (deckId == "B") Color(0xFF1E293B) else Color.Transparent)
                                .clickable { onSwitchDeck("B") }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Library / Load Track Button
                Button(
                    onClick = onOpenLibrary,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Library",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIBRARY",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Big Turntable Platter
            TurntablePlatter(
                deckId = deckId,
                isPlaying = deckState.isPlaying,
                playbackSpeed = deckState.playbackSpeed,
                thumbnailUrl = deckState.track?.thumbnailUrl,
                bpm = deckState.track?.bpm ?: 124,
                keySignature = deckState.track?.keySignature ?: "C min",
                accentColor = accentColor,
                modifier = Modifier.size(170.dp).testTag("turntable_platter_$deckId")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Track Title & Metadata
            Text(
                text = deckState.track?.title ?: "No Track Loaded",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = deckState.track?.artist ?: "Tap 'Library' above to load a local track or stream",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Audio Spec Tags
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Text(
                        text = if (deckState.isYoutubeTrack) "YOUTUBE STREAM" else (deckState.track?.bitDepth ?: "24-BIT / 96k"),
                        color = if (deckState.isYoutubeTrack) Color(0xFFEF4444) else MatrixGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Text(
                        text = "${deckState.track?.bpm ?: 124} BPM",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Text(
                        text = deckState.track?.keySignature ?: "C min",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Buffering / Loading Progress Indicator
            if (deckState.isLoading || deckState.isBuffering) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = deckState.statusText ?: "Buffering audio stream...",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (deckState.bufferPercent > 0) {
                            Text(
                                text = "${deckState.bufferPercent}%",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    if (deckState.bufferPercent > 0) {
                        LinearProgressIndicator(
                            progress = { deckState.bufferPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = accentColor,
                            trackColor = Color(0xFF1E293B)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = accentColor,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rock-Solid Audio Timeline Scrubber (Fixes jumping and seek stutter)
            DeckTimelineScrubber(
                currentPositionMs = deckState.currentPositionMs,
                durationMs = deckState.durationMs,
                accentColor = accentColor,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth().testTag("scrubber_$deckId")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CUE Button
                IconButton(
                    onClick = onJumpCue,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .testTag("cue_$deckId")
                ) {
                    Text("CUE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // JUMP BACK 10 SECONDS
                IconButton(
                    onClick = {
                        val newPos = (deckState.currentPositionMs - 10000L).coerceAtLeast(0L)
                        onSeek(newPos)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Back 10s",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // PLAY / PAUSE Main Center Button (Large 60.dp)
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(60.dp)
                        .background(if (deckState.isPlaying) accentColor else Color(0xFF1E293B), CircleShape)
                        .border(2.dp, accentColor, CircleShape)
                        .testTag("play_pause_$deckId")
                ) {
                    Icon(
                        imageVector = if (deckState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Deck $deckId",
                        tint = if (deckState.isPlaying) Color.Black else accentColor,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // JUMP FORWARD 10 SECONDS
                IconButton(
                    onClick = {
                        val newPos = if (deckState.durationMs > 0) {
                            (deckState.currentPositionMs + 10000L).coerceAtMost(deckState.durationMs)
                        } else {
                            deckState.currentPositionMs + 10000L
                        }
                        onSeek(newPos)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // LOOP Button
                IconButton(
                    onClick = onLoop,
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (deckState.isLooping) MatrixGreen.copy(alpha = 0.25f) else Color(0xFF1E293B), CircleShape)
                        .border(1.dp, if (deckState.isLooping) MatrixGreen else Color.Transparent, CircleShape)
                        .testTag("loop_$deckId")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Loop Deck $deckId",
                        tint = if (deckState.isLooping) MatrixGreen else Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Speed Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPEED:",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                listOf(0.8f, 1.0f, 1.2f, 1.5f).forEach { speed ->
                    val isCurrent = kotlin.math.abs(deckState.playbackSpeed - speed) < 0.05f
                    Text(
                        text = "${speed}x",
                        color = if (isCurrent) Color.Black else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCurrent) accentColor else Color(0xFF1E293B))
                            .clickable { onSpeedChanged(speed) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Stereo VU Meters & Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StereoVuMeter(
                    levelL = deckState.vuLevelL,
                    levelR = deckState.vuLevelR,
                    label = "OUT L/R"
                )

                // Master Volume Slider
                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OUTPUT VOLUME",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(masterVolume * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = masterVolume,
                        onValueChange = onMasterVolumeChanged,
                        colors = SliderDefaults.colors(
                            thumbColor = DspSurroundViolet,
                            activeTrackColor = DspSurroundViolet,
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Robust timeline scrubber that supports smooth drag gestures without stutter or jumping.
 */
@Composable
fun DeckTimelineScrubber(
    currentPositionMs: Long,
    durationMs: Long,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val actualProgress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayProgress = if (isDragging) dragFraction else actualProgress
    val displayPositionMs = if (isDragging && durationMs > 0) {
        (dragFraction * durationMs).toLong()
    } else currentPositionMs

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(displayPositionMs),
                color = accentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatMs(durationMs),
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Slider(
            value = displayProgress,
            onValueChange = { frac ->
                isDragging = true
                dragFraction = frac
            },
            onValueChangeFinished = {
                if (durationMs > 0) {
                    val targetMs = (dragFraction * durationMs).toLong()
                    onSeek(targetMs)
                }
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.fillMaxWidth().height(22.dp)
        )
    }
}

@Composable
private fun DeckCard(
    deckState: DeckUiState,
    deckId: String,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onLoop: () -> Unit,
    onCue: () -> Unit,
    onJumpCue: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111728)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.testTag("deck_card_$deckId")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Track Info Header
            Text(
                text = deckState.track?.title ?: "No Track Loaded",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = deckState.track?.artist ?: "Select from Library",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Buffering / Loading Bar
            if (deckState.isLoading || deckState.isBuffering) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = deckState.statusText ?: "Buffering stream...",
                            color = accentColor,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (deckState.bufferPercent > 0) {
                            Text(
                                text = "${deckState.bufferPercent}%",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    if (deckState.bufferPercent > 0) {
                        LinearProgressIndicator(
                            progress = { deckState.bufferPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = accentColor,
                            trackColor = Color(0xFF1E293B)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = accentColor,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Turntable Platter
            TurntablePlatter(
                deckId = deckId,
                isPlaying = deckState.isPlaying,
                playbackSpeed = deckState.playbackSpeed,
                thumbnailUrl = deckState.track?.thumbnailUrl,
                bpm = deckState.track?.bpm ?: 124,
                keySignature = deckState.track?.keySignature ?: "C min",
                accentColor = accentColor,
                modifier = Modifier.size(110.dp).testTag("turntable_platter_$deckId")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Timeline Scrubber
            DeckTimelineScrubber(
                currentPositionMs = deckState.currentPositionMs,
                durationMs = deckState.durationMs,
                accentColor = accentColor,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth().testTag("scrubber_$deckId")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Primary DJ Transport Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CUE Button
                IconButton(
                    onClick = { onJumpCue() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .testTag("cue_$deckId")
                ) {
                    Text("CUE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // PLAY / PAUSE Button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (deckState.isPlaying) accentColor else Color(0xFF1E293B), CircleShape)
                        .testTag("play_pause_$deckId")
                ) {
                    Icon(
                        imageVector = if (deckState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Deck $deckId",
                        tint = if (deckState.isPlaying) Color.Black else accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // SYNC Button
                IconButton(
                    onClick = onSync,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .testTag("sync_$deckId")
                ) {
                    Text("SYNC", color = accentColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                // LOOP Button
                IconButton(
                    onClick = onLoop,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (deckState.isLooping) MatrixGreen.copy(alpha = 0.25f) else Color(0xFF1E293B), CircleShape)
                        .border(1.dp, if (deckState.isLooping) MatrixGreen else Color.Transparent, CircleShape)
                        .testTag("loop_$deckId")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Loop Deck $deckId",
                        tint = if (deckState.isLooping) MatrixGreen else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Speed & Pitch Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PITCH ${"%.1f".format((deckState.playbackSpeed - 1f) * 100)}%",
                    color = Color(0xFF64748B),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${deckState.track?.bpm ?: 124} BPM",
                    color = accentColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = deckState.playbackSpeed,
                onValueChange = onSpeedChanged,
                valueRange = 0.75f..1.25f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.fillMaxWidth().height(20.dp).testTag("speed_slider_$deckId")
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
