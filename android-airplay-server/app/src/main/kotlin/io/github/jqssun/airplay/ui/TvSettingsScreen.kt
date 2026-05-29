package io.github.jqssun.airplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text as TvText
import io.github.jqssun.airplay.viewmodel.MainViewModel

// ─── Category definitions ────────────────────────────────────────────────────

private enum class Category(val label: String) {
    SERVER("Server"),
    CONNECTION("Connection"),
    DISPLAY("Display"),
    CODECS("Codecs"),
    DEVELOPER("Developer"),
}

// ─── Root composable ─────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsScreen(viewModel: MainViewModel) {
    val developerOptions by viewModel.developerOptions.collectAsState()
    val categories = remember(developerOptions) {
        Category.entries.filter { it != Category.DEVELOPER || developerOptions }
    }
    var selectedCategory by remember { mutableIntStateOf(0) }
    val sidebarFocusRequester = remember { FocusRequester() }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1A))) {
        // ── Left sidebar: category list ───────────────────────────────────
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xFF16162A))
                .padding(vertical = 24.dp),
        ) {
            TvText(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            categories.forEachIndexed { i, cat ->
                CategoryItem(
                    label = cat.label,
                    selected = selectedCategory == i,
                    focusRequester = if (i == 0) sidebarFocusRequester else null,
                    onFocus = { selectedCategory = i },
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxHeight().width(1.dp),
            color = Color(0xFF2A2A44),
            thickness = 1.dp,
        )

        // ── Right panel: settings for selected category ───────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            when (categories.getOrNull(selectedCategory)) {
                Category.SERVER     -> ServerSettings(viewModel)
                Category.CONNECTION -> ConnectionSettings(viewModel)
                Category.DISPLAY    -> DisplaySettings(viewModel)
                Category.CODECS     -> CodecSettings(viewModel)
                Category.DEVELOPER  -> DeveloperSettings(viewModel)
                null                -> {}
            }
        }
    }

    LaunchedEffect(Unit) { sidebarFocusRequester.requestFocus() }
}

// ─── Sidebar item ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryItem(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
) {
    val bg = if (selected) Color(0xFF3A3A6A) else Color.Transparent
    val textColor = if (selected) Color.White else Color(0xFFAAAAAA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        TvText(
            text = label,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

// ─── Toggle row ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        selected = false,
        onClick = { onCheckedChange(!checked) },
        headlineContent = {
            TvText(title, fontSize = 18.sp, color = Color.White)
        },
        supportingContent = {
            TvText(description, fontSize = 14.sp, color = Color(0xFF888888))
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0xFF252545),
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

// ─── Value cycle row (press OK to cycle through options) ─────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvCycleRow(
    title: String,
    description: String,
    options: List<Pair<String, String>>,  // (value, displayLabel)
    currentValue: String,
    onValueChange: (String) -> Unit,
) {
    val currentLabel = options.firstOrNull { it.first == currentValue }?.second ?: currentValue
    ListItem(
        selected = false,
        onClick = {
            val idx = options.indexOfFirst { it.first == currentValue }
            val next = options[(idx + 1) % options.size]
            onValueChange(next.first)
        },
        headlineContent = {
            TvText(title, fontSize = 18.sp, color = Color.White)
        },
        supportingContent = {
            TvText(description, fontSize = 14.sp, color = Color(0xFF888888))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvText("‹ ", fontSize = 16.sp, color = Color(0xFF6666AA))
                TvText(currentLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF88AAFF))
                TvText(" ›", fontSize = 16.sp, color = Color(0xFF6666AA))
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0xFF252545),
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

// ─── Info row (read-only, e.g. server name on TV where keyboard is awkward) ──

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvInfoRow(title: String, value: String, hint: String = "") {
    ListItem(
        selected = false,
        onClick = {},
        headlineContent = { TvText(title, fontSize = 18.sp, color = Color.White) },
        supportingContent = {
            Column {
                TvText(value, fontSize = 16.sp, color = Color(0xFF88AAFF), fontWeight = FontWeight.SemiBold)
                if (hint.isNotBlank()) TvText(hint, fontSize = 13.sp, color = Color(0xFF666666))
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0xFF1E1E3A),
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
private fun PanelHeader(title: String) {
    TvText(
        text = title,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

// ─── Category panels ──────────────────────────────────────────────────────────

@Composable
private fun ServerSettings(viewModel: MainViewModel) {
    val serverName by viewModel.serverName.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val autoStart by viewModel.autoStart.collectAsState()
    val bootAutoStart by viewModel.bootAutoStart.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { PanelHeader("Server") }
        item {
            TvInfoRow(
                title = "Server name",
                value = serverName,
                hint = "Change in app settings on a phone/tablet",
            )
        }
        item {
            TvInfoRow(
                title = "Server port",
                value = serverPort.toString(),
                hint = "Change in app settings on a phone/tablet",
            )
        }
        item {
            TvToggleRow(
                title = "Start server automatically",
                description = "Start the AirPlay server when the app launches",
                checked = autoStart,
                onCheckedChange = { viewModel.setAutoStart(it) },
            )
        }
        item {
            TvToggleRow(
                title = "Start server at boot",
                description = "Start the AirPlay server when the device starts",
                checked = bootAutoStart,
                onCheckedChange = { viewModel.setBootAutoStart(it) },
            )
        }
    }
}

@Composable
private fun ConnectionSettings(viewModel: MainViewModel) {
    val requirePin by viewModel.requirePin.collectAsState()
    val allowNewConn by viewModel.allowNewConn.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { PanelHeader("Connection") }
        item {
            TvToggleRow(
                title = "Require PIN",
                description = "Require a 4-digit PIN for each new connection",
                checked = requirePin,
                onCheckedChange = { viewModel.setRequirePin(it) },
            )
        }
        item {
            TvToggleRow(
                title = "Allow new connections",
                description = "Drop current client when a new one connects",
                checked = allowNewConn,
                onCheckedChange = { viewModel.setAllowNewConn(it) },
            )
        }
    }
}

@Composable
private fun DisplaySettings(viewModel: MainViewModel) {
    val resolution by viewModel.resolution.collectAsState()
    val maxFps by viewModel.maxFps.collectAsState()
    val overscanned by viewModel.overscanned.collectAsState()
    val autoFullscreen by viewModel.autoFullscreen.collectAsState()
    val autoAudioMode by viewModel.autoAudioMode.collectAsState()

    val resolutionOptions = listOf(
        "auto"      to "Auto",
        "1280x720"  to "1280 × 720  (720p)",
        "1920x1080" to "1920 × 1080  (1080p)",
        "3840x2160" to "3840 × 2160  (4K)",
    )
    val fpsOptions = listOf(
        "24"  to "24 fps",
        "30"  to "30 fps",
        "60"  to "60 fps",
        "120" to "120 fps",
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { PanelHeader("Display") }
        item {
            TvCycleRow(
                title = "Resolution",
                description = "Video resolution advertised to clients — press OK to cycle",
                options = resolutionOptions,
                currentValue = if (resolutionOptions.any { it.first == resolution }) resolution else "auto",
                onValueChange = { viewModel.setResolution(it) },
            )
        }
        item {
            TvCycleRow(
                title = "Maximum FPS",
                description = "Maximum frame rate advertised to clients — press OK to cycle",
                options = fpsOptions,
                currentValue = maxFps.toString().let { v -> if (fpsOptions.any { it.first == v }) v else "60" },
                onValueChange = { viewModel.setMaxFps(it.toIntOrNull() ?: 60) },
            )
        }
        item {
            TvToggleRow(
                title = "Overscanned",
                description = "Add pixel boundary for full-screen overscan displays",
                checked = overscanned,
                onCheckedChange = { viewModel.setOverscanned(it) },
            )
        }
        item {
            TvToggleRow(
                title = "Enter fullscreen automatically",
                description = "Enter fullscreen when a client connects",
                checked = autoFullscreen,
                onCheckedChange = { viewModel.setAutoFullscreen(it) },
            )
        }
        item {
            TvToggleRow(
                title = "Enter audio mode automatically",
                description = "Skip the prompt when switching to audio mode",
                checked = autoAudioMode,
                onCheckedChange = { viewModel.setAutoAudioMode(it) },
            )
        }
    }
}

@Composable
private fun CodecSettings(viewModel: MainViewModel) {
    val h265Enabled by viewModel.h265Enabled.collectAsState()
    val alacEnabled by viewModel.alacEnabled.collectAsState()
    val swAlacEnabled by viewModel.swAlacEnabled.collectAsState()
    val aacEnabled by viewModel.aacEnabled.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { PanelHeader("Codecs") }
        item {
            TvToggleRow(
                title = "H.265 (HEVC)",
                description = "Enable H.265 video decoding if device supports it",
                checked = h265Enabled,
                onCheckedChange = { viewModel.setH265Enabled(it) },
            )
        }
        item {
            TvToggleRow(
                title = "ALAC audio",
                description = "ALAC codec for AirPlay music streaming",
                checked = alacEnabled,
                onCheckedChange = { viewModel.setAlacEnabled(it) },
            )
        }
        item {
            TvToggleRow(
                title = "Software ALAC",
                description = "Use software decoder for ALAC (fallback)",
                checked = swAlacEnabled,
                onCheckedChange = { viewModel.setSwAlacEnabled(it) },
            )
        }
        item {
            TvToggleRow(
                title = "AAC audio",
                description = "AAC-ELD or AAC-LC codec for screen mirroring audio",
                checked = aacEnabled,
                onCheckedChange = { viewModel.setAacEnabled(it) },
            )
        }
    }
}

@Composable
private fun DeveloperSettings(viewModel: MainViewModel) {
    val developerOptions by viewModel.developerOptions.collectAsState()
    val keyAllowFrameDrop by viewModel.keyAllowFrameDrop.collectAsState()
    val enforceSdr by viewModel.enforceSdr.collectAsState()
    val realtimeDecoderPriority by viewModel.realtimeDecoderPriority.collectAsState()
    val operatingRateHint by viewModel.operatingRateHint.collectAsState()
    val scheduledOutputBufferRelease by viewModel.scheduledOutputBufferRelease.collectAsState()
    val debugEnabled by viewModel.debugEnabled.collectAsState()
    val benchmarkLog by viewModel.benchmarkLog.collectAsState()
    val audioLatencyMs by viewModel.audioLatencyMs.collectAsState()
    val audioBufferMultiplier by viewModel.audioBufferMultiplier.collectAsState()

    // Audio delay presets: -1 = disabled, otherwise ms
    val audioDelayOptions = listOf(
        "-1"   to "Off",
        "0"    to "0 ms",
        "50"   to "50 ms",
        "100"  to "100 ms",
        "150"  to "150 ms",
        "200"  to "200 ms",
        "300"  to "300 ms",
        "500"  to "500 ms",
        "750"  to "750 ms",
        "1000" to "1000 ms",
    )
    val audioBufferOptions = listOf(
        "4" to "4× (low latency)",
        "5" to "5×",
        "6" to "6× (default)",
        "7" to "7×",
        "8" to "8× (most stable)",
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { PanelHeader("Developer") }

        // ── A/V sync tuning ──────────────────────────────────────────────
        item {
            TvText(
                text = "A/V Sync",
                fontSize = 13.sp,
                color = Color(0xFF6666AA),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            )
        }
        item {
            TvCycleRow(
                title = "Audio delay",
                description = "Delay audio to compensate A/V sync drift — press OK to cycle. " +
                        "Note: bitrate is set by the iPad; lower resolution/FPS reduces it.",
                options = audioDelayOptions,
                currentValue = audioLatencyMs.toString().let { v ->
                    if (audioDelayOptions.any { it.first == v }) v else "-1"
                },
                onValueChange = { viewModel.setAudioLatencyMs(it.toIntOrNull() ?: -1) },
            )
        }
        item {
            TvCycleRow(
                title = "Audio buffer size",
                description = "Larger buffer = more stable audio but higher latency — press OK to cycle",
                options = audioBufferOptions,
                currentValue = audioBufferMultiplier.toString().let { v ->
                    if (audioBufferOptions.any { it.first == v }) v else "6"
                },
                onValueChange = { viewModel.setAudioBufferMultiplier(it.toIntOrNull() ?: 6) },
            )
        }

        // ── Developer toggle ─────────────────────────────────────────────
        item {
            TvText(
                text = "Advanced",
                fontSize = 13.sp,
                color = Color(0xFF6666AA),
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        item {
            TvToggleRow(
                title = "Developer options",
                description = "Show advanced decoder options",
                checked = developerOptions,
                onCheckedChange = { viewModel.setDeveloperOptions(it) },
            )
        }
        if (developerOptions) {
            item {
                TvToggleRow(
                    title = "Allow frame drop",
                    description = "Allow the decoder to drop frames under load",
                    checked = keyAllowFrameDrop,
                    onCheckedChange = { viewModel.setKeyAllowFrameDrop(it) },
                )
            }
            item {
                TvToggleRow(
                    title = "Enforce SDR",
                    description = "Force standard dynamic range output",
                    checked = enforceSdr,
                    onCheckedChange = { viewModel.setEnforceSdr(it) },
                )
            }
            item {
                TvToggleRow(
                    title = "Realtime decoder priority",
                    description = "Boost decoder thread priority",
                    checked = realtimeDecoderPriority,
                    onCheckedChange = { viewModel.setRealtimeDecoderPriority(it) },
                )
            }
            item {
                TvToggleRow(
                    title = "Operating rate hint",
                    description = "Hint the codec about expected frame rate",
                    checked = operatingRateHint,
                    onCheckedChange = { viewModel.setOperatingRateHint(it) },
                )
            }
            item {
                TvToggleRow(
                    title = "Scheduled buffer release",
                    description = "Release output buffers on a schedule",
                    checked = scheduledOutputBufferRelease,
                    onCheckedChange = { viewModel.setScheduledOutputBufferRelease(it) },
                )
            }
            item {
                TvToggleRow(
                    title = "Debug overlay",
                    description = "Show video/audio stats overlay while mirroring",
                    checked = debugEnabled,
                    onCheckedChange = { viewModel.setDebugEnabled(it) },
                )
            }
            item {
                TvToggleRow(
                    title = "Benchmark log",
                    description = "Log detailed performance data",
                    checked = benchmarkLog,
                    onCheckedChange = { viewModel.setBenchmarkLog(it) },
                )
            }
        }
    }
}
