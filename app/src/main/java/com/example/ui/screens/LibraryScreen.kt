package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.TrackEntity
import com.example.ui.AuraDeckViewModel
import com.example.ui.MainTab
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import com.example.ui.theme.DspSurroundViolet
import com.example.ui.theme.MatrixGreen

@Composable
fun LibraryScreen(
    viewModel: AuraDeckViewModel,
    modifier: Modifier = Modifier
) {
    val allTracks by viewModel.allTracks.collectAsState()
    val streamInputState by viewModel.streamInputState.collectAsState()

    // Local file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLocalAudio(it) }
    }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTracks = remember(allTracks, selectedFilter) {
        when (selectedFilter) {
            "FAVORITES" -> allTracks.filter { it.isFavorite }
            "STREAMS" -> allTracks.filter { it.isRemoteStream }
            "LOCAL" -> allTracks.filter { !it.isRemoteStream }
            else -> allTracks
        }
    }

    val localCount = remember(allTracks) { allTracks.count { !it.isRemoteStream } }
    val streamCount = remember(allTracks) { allTracks.count { it.isRemoteStream } }
    val favCount = remember(allTracks) { allTracks.count { it.isFavorite } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080B11))
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // Library Header Bar: Title, Count, & Device File Import Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Library",
                        tint = DeckACyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUDIO LIBRARY",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${allTracks.size} TRACKS",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Import Local Device Audio
                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("pick_local_audio")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Import",
                        tint = MatrixGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IMPORT FILE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Stream Ingestion Card (YouTube / Web Stream URL)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101625)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Stream Ingestion",
                                tint = DeckACyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STREAM & YOUTUBE INGESTION",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Target Deck Selector
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(2.dp)
                        ) {
                            Text(
                                text = "DECK A",
                                color = if (streamInputState.targetDeck == "A") Color.Black else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (streamInputState.targetDeck == "A") DeckACyan else Color.Transparent)
                                    .clickable { viewModel.setStreamTargetDeck("A") }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Text(
                                text = "DECK B",
                                color = if (streamInputState.targetDeck == "B") Color.Black else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (streamInputState.targetDeck == "B") DeckBAmber else Color.Transparent)
                                    .clickable { viewModel.setStreamTargetDeck("B") }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // URL Input Row
                    OutlinedTextField(
                        value = streamInputState.urlInput,
                        onValueChange = { viewModel.setStreamUrlInput(it) },
                        placeholder = {
                            Text(
                                text = "Paste YouTube link (watch, shorts, youtu.be) or audio stream",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        trailingIcon = {
                            if (streamInputState.urlInput.isNotBlank()) {
                                IconButton(onClick = { viewModel.setStreamUrlInput("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeckACyan,
                            unfocusedBorderColor = Color(0xFF243048),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0C101A),
                            unfocusedContainerColor = Color(0xFF0C101A)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("url_input_field")
                    )

                    // Loading & Buffering Bar for Stream Ingestion
                    if (streamInputState.isResolving) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (streamInputState.targetDeck == "A") DeckACyan else DeckBAmber,
                                trackColor = Color(0xFF1E283C)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "RESOLVING AUDIO METADATA & BUFFERING STREAM...",
                                color = if (streamInputState.targetDeck == "A") DeckACyan else DeckBAmber,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ingestion Action Button
                    Button(
                        onClick = {
                            viewModel.resolveAndLoadStream(streamInputState.urlInput, autoPlay = true)
                        },
                        enabled = streamInputState.urlInput.isNotBlank() && !streamInputState.isResolving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (streamInputState.targetDeck == "A") DeckACyan else DeckBAmber,
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("resolve_stream_button")
                    ) {
                        if (streamInputState.isResolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BUFFERING STREAM...",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "INGEST & PLAY ON DECK ${streamInputState.targetDeck}",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    streamInputState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = error,
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // YouTube Music Search Card
        item {
            var searchQuery by remember { mutableStateOf("") }
            val searchResults by viewModel.searchResults.collectAsState()
            val isSearching by viewModel.isSearching.collectAsState()

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151020)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D1E3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DspSurroundViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YOUTUBE MUSIC SEARCH",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (searchResults.isNotEmpty()) {
                            Text(
                                text = "CLEAR",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clickable {
                                    viewModel.clearSearchResults()
                                    searchQuery = ""
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search songs, artists, mixes...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DspSurroundViolet,
                                unfocusedBorderColor = Color(0xFF243048),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0C101A),
                                unfocusedContainerColor = Color(0xFF0C101A)
                            ),
                            modifier = Modifier.weight(1f).height(50.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.searchYoutube(searchQuery) },
                            enabled = searchQuery.isNotBlank() && !isSearching,
                            colors = ButtonDefaults.buttonColors(containerColor = DspSurroundViolet),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(50.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                            }
                        }
                    }

                    if (searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchResults.forEach { resultInfo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A1423))
                                        .clickable {
                                            // Instantiate track and save it, then play it!
                                            viewModel.resolveAndLoadStream(resultInfo.uri, autoPlay = true)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = resultInfo.thumbnailUrl,
                                        contentDescription = "Thumb",
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(resultInfo.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(resultInfo.artist, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = DspSurroundViolet)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Pills Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterTabChip(
                    title = "ALL (${allTracks.size})",
                    isSelected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                FilterTabChip(
                    title = "LOCAL ($localCount)",
                    isSelected = selectedFilter == "LOCAL",
                    onClick = { selectedFilter = "LOCAL" }
                )
                FilterTabChip(
                    title = "STREAMS ($streamCount)",
                    isSelected = selectedFilter == "STREAMS",
                    onClick = { selectedFilter = "STREAMS" }
                )
                FilterTabChip(
                    title = "FAVORITES ($favCount)",
                    isSelected = selectedFilter == "FAVORITES",
                    onClick = { selectedFilter = "FAVORITES" }
                )
            }
        }

        // Empty State if no tracks match filter
        if (filteredTracks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101524)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283C)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicOff,
                            contentDescription = "Empty",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No tracks found in this category",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import a local audio file or paste a YouTube URL above",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Tracks List Items
        items(filteredTracks, key = { it.trackId }) { track ->
            TrackCard(
                track = track,
                onPlayNow = {
                    viewModel.loadTrackToDeck("A", track, autoPlay = true)
                    viewModel.selectTab(MainTab.DECKS)
                },
                onLoadToDeckA = {
                    viewModel.loadTrackToDeck("A", track, autoPlay = true)
                    viewModel.selectTab(MainTab.DECKS)
                },
                onLoadToDeckB = {
                    viewModel.loadTrackToDeck("B", track, autoPlay = true)
                    viewModel.selectTab(MainTab.DECKS)
                },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onDeleteTrack = { viewModel.deleteTrack(track) }
            )
        }
    }
}

@Composable
private fun FilterTabChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) DspSurroundViolet else Color(0xFF101524),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) DspSurroundViolet else Color(0xFF1E283C)
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TrackCard(
    track: TrackEntity,
    onPlayNow: () -> Unit,
    onLoadToDeckA: () -> Unit,
    onLoadToDeckB: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteTrack: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101524)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283C)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayNow)
            .testTag("track_item_${track.trackId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main Track Row: Artwork, Metadata, Play Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A2234))
                        .border(1.dp, Color(0xFF2E3D56), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Track",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Audio Spec & Source Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source tag
                        val sourceText = when {
                            track.uri.startsWith("youtube:") || track.uri.startsWith("yt:") -> "YOUTUBE"
                            track.isRemoteStream -> "STREAM"
                            else -> "LOCAL"
                        }
                        val sourceColor = when {
                            track.uri.startsWith("youtube:") || track.uri.startsWith("yt:") -> Color(0xFFEF4444)
                            track.isRemoteStream -> DeckACyan
                            else -> MatrixGreen
                        }

                        Text(
                            text = sourceText,
                            color = sourceColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(sourceColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )

                        Text(
                            text = "${track.bpm} BPM",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = track.keySignature,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Instant Play Circle Button
                IconButton(
                    onClick = onPlayNow,
                    modifier = Modifier
                        .size(40.dp)
                        .background(DeckACyan, CircleShape)
                        .testTag("play_track_${track.trackId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Track",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF1E283C), thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row: Deck A, Deck B, Favorite, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deck Target Loading Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeckACyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable(onClick = onLoadToDeckA)
                            .testTag("load_deck_a_${track.trackId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(DeckACyan, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOAD DECK A",
                                color = DeckACyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeckBAmber.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable(onClick = onLoadToDeckB)
                            .testTag("load_deck_b_${track.trackId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(DeckBAmber, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOAD DECK B",
                                color = DeckBAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Utility Actions: Favorite, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) Color(0xFFFFD54F) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteTrack,
                        modifier = Modifier.size(32.dp).testTag("delete_track_${track.trackId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Track",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
