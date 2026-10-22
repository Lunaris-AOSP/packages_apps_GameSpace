/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.chaldeaprjkt.gamespace.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.ui.components.*
import io.chaldeaprjkt.gamespace.ui.viewmodel.RegisteredGame
import io.chaldeaprjkt.gamespace.ui.viewmodel.SettingsViewModel
import io.chaldeaprjkt.gamespace.utils.GameModeUtils

@Composable
fun AppPickerSheet(viewModel: SettingsViewModel, quickStart: Boolean, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable {
        mutableStateOf(viewModel.quickStartApps.split(',').filter { it.isNotBlank() }.distinct())
    }
    val registered = viewModel.registeredGames.map { it.packageName }.toSet()
    val apps =
        viewModel.availableApps.filter {
            (quickStart || it.packageName !in registered) &&
                (it.label.contains(query.trim(), ignoreCase = true) ||
                    it.packageName.contains(query.trim(), ignoreCase = true))
        }
    LaunchedEffect(Unit) { viewModel.loadAvailableApps() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(
                    if (quickStart) R.string.quick_start_apps_title else R.string.add_game
                ),
                Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                label = { Text(stringResource(R.string.app_picker_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon =
                    if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.search_clear))
                            }
                        }
                    } else null,
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true,
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    viewModel.appsLoading ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    viewModel.appsLoadFailed ->
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(R.string.app_picker_load_error))
                            TextButton(onClick = viewModel::loadAvailableApps) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    apps.isEmpty() ->
                        Box(
                            Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(
                                    if (query.isBlank()) R.string.app_picker_empty
                                    else R.string.app_picker_no_results
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    else ->
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(apps, key = { it.packageName }) { app ->
                                val checked = app.packageName in selected
                                val action =
                                    if (quickStart) {
                                        Modifier.toggleable(
                                            checked,
                                            role = Role.Checkbox,
                                            onValueChange = { enabled ->
                                                selected =
                                                    if (enabled) selected + app.packageName
                                                    else selected - app.packageName
                                            },
                                        )
                                    } else {
                                        Modifier.clickable {
                                            viewModel.registerGame(app.packageName)
                                            onDismiss()
                                        }
                                    }
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color =
                                        if (quickStart && checked)
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceBright,
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().then(action).padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        GameAppIcon(app.icon, Modifier.size(48.dp))
                                        Text(
                                            app.label,
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        if (quickStart) Checkbox(checked, onCheckedChange = null)
                                        else Icon(Icons.Rounded.Add, null)
                                    }
                                }
                            }
                        }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                if (quickStart) {
                    Button(
                        onClick = {
                            viewModel.updateQuickStartApps(selected.joinToString(","))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.save))
                            Text(
                                pluralStringResource(
                                    R.plurals.quick_start_app_count,
                                    selected.size,
                                    selected.size,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameOptionsSheet(game: RegisteredGame, viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    var modeDialog by rememberSaveable { mutableStateOf(false) }
    var driverDialog by rememberSaveable { mutableStateOf(false) }
    var removeDialog by rememberSaveable { mutableStateOf(false) }
    var driver by
        remember(game.packageName) {
            mutableStateOf(viewModel.getAngleDriverChoice(game.packageName))
        }
    val hasAngle = remember(game.packageName) { viewModel.angleFeatureAvailable }
    val modes = listOf(1 to gameModeLabel(1), 2 to gameModeLabel(2), 3 to gameModeLabel(3))
    val drivers =
        listOf(
            GameModeUtils.DRIVER_CHOICE_DEFAULT to stringResource(R.string.game_driver_default),
            GameModeUtils.DRIVER_CHOICE_ANGLE to stringResource(R.string.game_driver_angle),
            GameModeUtils.DRIVER_CHOICE_NATIVE to stringResource(R.string.game_driver_native),
        )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                GameAppIcon(game.icon, Modifier.size(72.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(game.label, style = MaterialTheme.typography.headlineSmallEmphasized)
                    Text(
                        stringResource(R.string.game_options_title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = {
                    viewModel.launchGame(game.packageName)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.launch_game),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            SettingsGroup(stringResource(R.string.game_options_title), Icons.Rounded.Tune) {
                SettingsAction(
                    stringResource(R.string.per_app_mode_title),
                    gameModeLabel(game.mode),
                ) {
                    modeDialog = true
                }
                if (hasAngle) {
                    SettingsAction(
                        stringResource(R.string.per_app_angle_title),
                        drivers.firstOrNull { it.first == driver }?.second,
                    ) {
                        driverDialog = true
                    }
                }
            }
            TextButton(
                onClick = { removeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Rounded.DeleteOutline, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.game_remove_title))
            }
        }
    }
    if (modeDialog) {
        SettingsChoiceDialog(
            stringResource(R.string.per_app_mode_title),
            modes,
            game.mode,
            { viewModel.updateGameMode(game.packageName, it) },
        ) {
            modeDialog = false
        }
    }
    if (driverDialog) {
        SettingsChoiceDialog(
            stringResource(R.string.per_app_angle_title),
            drivers,
            driver,
            {
                viewModel.updateAngleDriverChoice(game.packageName, it)
                driver = it
            },
        ) {
            driverDialog = false
        }
    }
    if (removeDialog) {
        AlertDialog(
            onDismissRequest = { removeDialog = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, null) },
            title = { Text(stringResource(R.string.game_remove_title)) },
            text = { Text(stringResource(R.string.game_remove_message, game.label)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unregisterGame(game.packageName)
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                ) {
                    Text(stringResource(R.string.game_remove_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { removeDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
