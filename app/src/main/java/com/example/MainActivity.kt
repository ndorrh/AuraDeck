package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.AuraDeckViewModel
import com.example.ui.MainTab
import com.example.ui.screens.DspSuiteScreen
import com.example.ui.screens.DualDecksScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.VisualizerStudioScreen
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import com.example.ui.theme.DspSurroundViolet
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AuraDeckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.RECORD_AUDIO
                ),
                101
            )
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.RECORD_AUDIO
                ),
                101
            )
        }

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                AuraDeckAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                        // Automatically load into Deck A, save to library, and immediately start playing!
                        viewModel.resolveAndLoadStream(sharedText, "A", autoPlay = true)
                        viewModel.selectTab(MainTab.DECKS)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    viewModel.importLocalAudio(uri)
                    viewModel.selectTab(MainTab.DECKS)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraDeckAppContent(viewModel: AuraDeckViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val dspState by viewModel.dspState.collectAsState()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Exit",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Exit AuraDeck?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This will immediately stop all audio playback, kill background audio services, remove notification controls, and close the app.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitApp(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("EXIT APP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("CANCEL", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF111728),
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auradeck_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Logo icon
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "AuraDeck Logo",
                                tint = DeckACyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "AURADECK",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "AUDIOPHILE DUAL-DECK",
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Live DSP State Badge
                        Surface(
                            shape = CircleShape,
                            color = if (dspState.isEnabled) DspSurroundViolet.copy(alpha = 0.2f) else Color(0xFF1E283C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (dspState.isEnabled) DspSurroundViolet else Color(0xFF334155)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (dspState.isEnabled) MatrixGreen else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (dspState.isEnabled) "DSP 96k" else "DSP OFF",
                                    color = if (dspState.isEnabled) Color.White else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Exit App Power Button
                        IconButton(
                            onClick = { showExitDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E283C))
                                .testTag("exit_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Exit App",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0E18)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0A0E18),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav")
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val icon = when (tab) {
                        MainTab.DECKS -> Icons.Default.Album
                        MainTab.DSP_SUITE -> Icons.Default.Equalizer
                        MainTab.VISUALIZER -> Icons.Default.GraphicEq
                        MainTab.PLAYLISTS -> Icons.Default.LibraryMusic
                    }
                    val selectedColor = when (tab) {
                        MainTab.DECKS -> DeckACyan
                        MainTab.DSP_SUITE -> DspSurroundViolet
                        MainTab.VISUALIZER -> MatrixGreen
                        MainTab.PLAYLISTS -> DeckBAmber
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) selectedColor else Color(0xFF64748B)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (isSelected) selectedColor else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = selectedColor.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Attached headless WebViews to ensure YouTube web audio runs inside window hierarchy
            Box(modifier = Modifier.size(1.dp).alpha(0.001f)) {
                AndroidView(factory = { ctx -> viewModel.audioEngine.getOrCreateBridgeView(ctx, "A") })
                AndroidView(factory = { ctx -> viewModel.audioEngine.getOrCreateBridgeView(ctx, "B") })
            }

            when (currentTab) {
                MainTab.DECKS -> DualDecksScreen(viewModel = viewModel)
                MainTab.DSP_SUITE -> DspSuiteScreen(viewModel = viewModel)
                MainTab.VISUALIZER -> VisualizerStudioScreen(viewModel = viewModel)
                MainTab.PLAYLISTS -> LibraryScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "AuraDeck: $name", modifier = modifier)
}

