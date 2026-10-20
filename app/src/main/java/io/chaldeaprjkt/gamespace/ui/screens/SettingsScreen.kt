/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.chaldeaprjkt.gamespace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.ui.components.*
import io.chaldeaprjkt.gamespace.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGame by rememberSaveable { mutableStateOf<String?>(null) }
    val gamesScroll = rememberLazyListState()
    val settingsScroll = rememberLazyListState()
    val crosshairScroll = rememberLazyListState()
    val listState =
        when (tab) {
            0 -> gamesScroll
            1 -> settingsScroll
            else -> crosshairScroll
        }
    val showTitle by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val snackbar = remember { SnackbarHostState() }
    val motion = MaterialTheme.motionScheme
    val title =
        stringResource(
            when (tab) {
                0 -> R.string.settings_title
                1 -> R.string.in_game_preferences_title
                else -> R.string.crosshair_settings_title
            }
        )
    val openPicker: (String) -> Unit = {
        viewModel.loadAvailableApps()
        picker = it
    }
    BackHandler(enabled = tab != 0 && picker == null && selectedGame == null) { tab = 0 }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(showTitle, enter = fadeIn(), exit = fadeOut()) {
                        Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (tab != 0) tab = 0 else onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (tab == 0) {
                        FilledTonalIconButton(onClick = { openPicker("games") }) {
                            Icon(Icons.Rounded.Add, stringResource(R.string.add_game))
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
            )
        },
        bottomBar = { SettingsNavigation(tab, onSelect = { tab = it }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) +
                        slideInHorizontally(motion.defaultSpatialSpec()) {
                            if (targetState > initialState) it / 8 else -it / 8
                        }) togetherWith fadeOut(motion.fastEffectsSpec())
                },
                label = "settings_page",
            ) { page ->
                when (page) {
                    0 ->
                        GamesPage(
                            viewModel,
                            gamesScroll,
                            { openPicker("games") },
                            { selectedGame = it },
                        )
                    1 -> OptionsPage(viewModel, settingsScroll, { openPicker("quick_start") })
                    else -> CrosshairScreen(viewModel, crosshairScroll)
                }
            }
        }
    }
    if (picker != null) {
        AppPickerSheet(
            viewModel,
            quickStart = picker == "quick_start",
            onDismiss = { picker = null },
        )
    }
    selectedGame?.let { packageName ->
        val game = viewModel.registeredGames.firstOrNull { it.packageName == packageName }
        if (game != null) {
            GameOptionsSheet(game, viewModel, onDismiss = { selectedGame = null })
        }
    }
}

@Composable
private fun SettingsNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val labels =
        listOf(R.string.settings_tab_games, R.string.settings_tab_settings, R.string.tile_crosshair)
    val icons = listOf(Icons.Rounded.SportsEsports, Icons.Rounded.Tune, Icons.Rounded.GpsFixed)
    Box(
        Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border =
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                Modifier.padding(6.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    val active = index == selected
                    val color by
                        animateColorAsState(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.motionScheme.defaultEffectsSpec(),
                            label = "tab_color",
                        )
                    Surface(
                        selected = active,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f).semantics { role = Role.Tab },
                        shape = RoundedCornerShape(if (active) 26.dp else 16.dp),
                        color = color,
                        contentColor =
                            if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Column(
                            Modifier.heightIn(min = 64.dp)
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(icons[index], null, Modifier.size(22.dp))
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelMediumEmphasized,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamesPage(
    viewModel: SettingsViewModel,
    listState: LazyListState,
    onAdd: () -> Unit,
    onGame: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "heading") {
            SettingsHeading(
                stringResource(R.string.settings_title),
                stringResource(R.string.settings_library_subtitle),
            )
        }
        item(key = "library") {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        GameEmblem(
                            Icons.Rounded.SportsEsports,
                            Modifier.size(80.dp),
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(R.string.library_hero_title),
                                style = MaterialTheme.typography.headlineSmallEmphasized,
                            )
                            Text(
                                pluralStringResource(
                                    R.plurals.library_game_count,
                                    viewModel.registeredGames.size,
                                    viewModel.registeredGames.size,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.library_hero_summary),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.add_game),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
        item(key = "detection") {
            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Surface(shape = MaterialTheme.shapes.large) {
                    SettingsToggle(
                        stringResource(R.string.auto_game_detect_title),
                        stringResource(R.string.auto_game_detect_summary),
                        viewModel.autoGameDetect,
                        viewModel::updateAutoGameDetect,
                    )
                }
            }
        }
        if (viewModel.gamesLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        } else if (viewModel.registeredGames.isEmpty()) {
            item(key = "empty") {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GameEmblem(
                        Icons.Rounded.Gamepad,
                        Modifier.size(72.dp),
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        stringResource(R.string.library_empty_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                    Text(
                        stringResource(R.string.library_empty_summary),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(viewModel.registeredGames, key = { it.packageName }) { game ->
                ExpressiveActionCard(onClick = { onGame(game.packageName) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GameAppIcon(game.icon, Modifier.size(56.dp))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(game.label, style = MaterialTheme.typography.titleMediumEmphasized)
                            Text(
                                gameModeLabel(game.mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.launchGame(game.packageName) }
                        ) {
                            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.launch_game))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionsPage(
    viewModel: SettingsViewModel,
    listState: LazyListState,
    onQuickStart: () -> Unit,
) {
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    val calls =
        listOf(
            0 to stringResource(R.string.in_game_calls_no_action),
            1 to stringResource(R.string.in_game_calls_auto_answer),
            2 to stringResource(R.string.in_game_calls_auto_reject),
        )
    val ringers =
        listOf(
            0 to stringResource(R.string.ringer_mode_silent),
            1 to stringResource(R.string.ringer_mode_vibrate),
            2 to stringResource(R.string.ringer_mode_normal),
            3 to stringResource(R.string.ringer_mode_no_change),
        )
    val quickStartCount = viewModel.quickStartApps.split(',').count { it.isNotBlank() }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item(key = "heading") {
            SettingsHeading(
                stringResource(R.string.in_game_preferences_title),
                stringResource(R.string.settings_options_subtitle),
            )
        }
        item(key = "notifications") {
            SettingsGroup(
                stringResource(R.string.settings_notifications),
                Icons.Rounded.Notifications,
            ) {
                SettingsToggle(
                    stringResource(R.string.auto_dnd_title),
                    stringResource(R.string.auto_dnd_summary),
                    viewModel.autoDnd,
                    viewModel::updateAutoDnd,
                )
                SettingsToggle(
                    stringResource(R.string.call_overlay_enabled_title),
                    stringResource(R.string.call_overlay_enabled_summary),
                    viewModel.callOverlayEnabled,
                    viewModel::updateCallOverlay,
                )
                SettingsAction(
                    stringResource(R.string.in_game_calls_title),
                    calls.firstOrNull { it.first == viewModel.callsMode }?.second,
                ) {
                    choice = "calls"
                }
                SettingsAction(
                    stringResource(R.string.ringer_mode_title),
                    ringers.firstOrNull { it.first == viewModel.ringerMode }?.second,
                ) {
                    choice = "ringer"
                }
                SettingsToggle(
                    stringResource(R.string.danmaku_notification_mode_title),
                    stringResource(R.string.danmaku_notification_mode_summary),
                    viewModel.danmakuNotification,
                    viewModel::updateDanmakuNotification,
                )
            }
        }
        item(key = "display") {
            SettingsGroup(stringResource(R.string.settings_display), Icons.Rounded.TouchApp) {
                SettingsToggle(
                    stringResource(R.string.auto_brightness_disabled_title),
                    stringResource(R.string.auto_brightness_disabled_summary),
                    viewModel.noAutoBrightness,
                    viewModel::updateNoAutoBrightness,
                )
                SettingsToggle(
                    stringResource(R.string.stay_awake_title),
                    stringResource(R.string.stay_awake_summary),
                    viewModel.stayAwake,
                    viewModel::updateStayAwake,
                )
                SettingsToggle(
                    stringResource(R.string.lock_gesture_title),
                    stringResource(R.string.lock_gesture_summary),
                    viewModel.lockGesture,
                    viewModel::updateLockGesture,
                )
                SettingsToggle(
                    stringResource(R.string.three_screenshot_disabled_title),
                    stringResource(R.string.three_screenshot_disabled_summary),
                    viewModel.noThreeScreenshot,
                    viewModel::updateNoThreeScreenshot,
                )
                if (viewModel.hasVibrator) {
                    SettingsToggle(
                        stringResource(R.string.pulse_bass_haptics_disabled_title),
                        stringResource(R.string.pulse_bass_haptics_disabled_summary),
                        viewModel.noPulseBassHaptics,
                        viewModel::updateNoPulseBassHaptics,
                    )
                }
            }
        }
        item(key = "sidebar") {
            SettingsGroup(stringResource(R.string.settings_sidebar), Icons.Rounded.Dashboard) {
                SettingsToggle(
                    stringResource(R.string.music_player_enabled_title),
                    stringResource(R.string.music_player_enabled_summary),
                    viewModel.musicPlayerEnabled,
                    viewModel::updateMusicPlayerEnabled,
                )
                SettingsAction(
                    stringResource(R.string.quick_start_apps_title),
                    pluralStringResource(
                        R.plurals.quick_start_app_count,
                        quickStartCount,
                        quickStartCount,
                    ),
                    onQuickStart,
                )
                SettingsSlider(
                    stringResource(R.string.icon_idle_opacity_title),
                    viewModel.iconIdleAlpha,
                    stringResource(
                        R.string.settings_value_percent,
                        viewModel.iconIdleAlpha.roundToInt(),
                    ),
                    5f..100f,
                    viewModel::updateIconIdleAlpha,
                )
            }
        }
        item(key = "optimization") {
            SettingsGroup(stringResource(R.string.game_optimization_title), Icons.Rounded.Bolt) {
                SettingsToggle(
                    stringResource(R.string.game_memory_management_title),
                    stringResource(R.string.game_memory_management_summary),
                    viewModel.memoryManagement,
                    viewModel::updateMemoryManagement,
                )
                SettingsToggle(
                    stringResource(R.string.game_cache_management_title),
                    stringResource(R.string.game_cache_management_summary),
                    viewModel.cacheManagement,
                    viewModel::updateCacheManagement,
                )
                if (viewModel.isBypassSupported) {
                    SettingsToggle(
                        stringResource(R.string.bypass_charge_enabled_title),
                        stringResource(R.string.bypass_charge_enabled_summary),
                        viewModel.bypassChargeEnabled,
                        viewModel::updateBypassChargeEnabled,
                    )
                }
            }
        }
    }
    when (choice) {
        "calls" ->
            SettingsChoiceDialog(
                stringResource(R.string.in_game_calls_title),
                calls,
                viewModel.callsMode,
                viewModel::updateCallsMode,
            ) {
                choice = null
            }
        "ringer" ->
            SettingsChoiceDialog(
                stringResource(R.string.ringer_mode_title),
                ringers,
                viewModel.ringerMode,
                viewModel::updateRingerMode,
            ) {
                choice = null
            }
    }
}

@Composable
internal fun gameModeLabel(mode: Int): String =
    stringResource(
        when (mode) {
            2 -> R.string.game_mode_performance
            3 -> R.string.game_mode_battery
            else -> R.string.game_mode_balanced
        }
    )
