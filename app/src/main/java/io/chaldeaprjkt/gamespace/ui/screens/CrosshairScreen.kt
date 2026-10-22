/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package io.chaldeaprjkt.gamespace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.ui.components.*
import io.chaldeaprjkt.gamespace.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

private val crosshairStyles =
    listOf(
        R.drawable.crosshair_style_1,
        R.drawable.crosshair_style_2,
        R.drawable.crosshair_style_3,
        R.drawable.crosshair_style_4,
        R.drawable.crosshair_style_5,
        R.drawable.crosshair_style_6,
        R.drawable.crosshair_style_7,
        R.drawable.crosshair_style_8,
    )

@Composable
fun CrosshairScreen(viewModel: SettingsViewModel, listState: LazyListState, bottomPadding: Dp) {
    val colors =
        listOf(
            0xFF00FF00.toInt() to R.string.crosshair_color_green,
            0xFFFF0000.toInt() to R.string.crosshair_color_red,
            0xFF00FFFF.toInt() to R.string.crosshair_color_cyan,
            0xFFFFFF00.toInt() to R.string.crosshair_color_yellow,
            0xFFFFFFFF.toInt() to R.string.crosshair_color_white,
            0xFF2196F3.toInt() to R.string.crosshair_color_blue,
        )
    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "heading") {
            SettingsHeading(
                stringResource(R.string.crosshair_settings_title),
                stringResource(R.string.settings_crosshair_subtitle),
            )
        }
        item(key = "preview") { CrosshairPreview(viewModel) }
        item(key = "enabled") {
            Surface(shape = MaterialTheme.shapes.large) {
                SettingsToggle(
                    stringResource(R.string.crosshair_settings_title),
                    stringResource(R.string.crosshair_settings_summary),
                    viewModel.crosshairEnabled,
                    viewModel::updateCrosshairEnabled,
                )
            }
        }
        item(key = "controls") {
            AnimatedVisibility(
                viewModel.crosshairEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    SettingsGroup(
                        stringResource(R.string.crosshair_appearance),
                        Icons.Rounded.Palette,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    stringResource(R.string.crosshair_style_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Column(
                                    Modifier.selectableGroup(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    crosshairStyles.chunked(4).forEachIndexed { row, styles ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            styles.forEachIndexed { column, drawable ->
                                                val style = row * 4 + column + 1
                                                val selected = viewModel.crosshairStyle == style
                                                val label =
                                                    stringResource(
                                                        R.string.crosshair_style_entry,
                                                        style,
                                                    )
                                                Surface(
                                                    selected = selected,
                                                    onClick = {
                                                        viewModel.updateCrosshairStyle(style)
                                                    },
                                                    modifier =
                                                        Modifier.weight(1f)
                                                            .heightIn(min = 64.dp)
                                                            .semantics {
                                                                role = Role.RadioButton
                                                                contentDescription = label
                                                            },
                                                    shape =
                                                        RoundedCornerShape(
                                                            if (selected) 24.dp else 16.dp
                                                        ),
                                                    color =
                                                        if (selected)
                                                            MaterialTheme.colorScheme
                                                                .primaryContainer
                                                        else
                                                            MaterialTheme.colorScheme
                                                                .surfaceContainerHigh,
                                                    contentColor =
                                                        if (selected)
                                                            MaterialTheme.colorScheme
                                                                .onPrimaryContainer
                                                        else
                                                            MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            painterResource(drawable),
                                                            null,
                                                            Modifier.size(28.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    stringResource(R.string.crosshair_color_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                FlowRow(
                                    Modifier.fillMaxWidth().selectableGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    colors.forEach { (color, labelResource) ->
                                        val selected = viewModel.crosshairColor == color
                                        val label = stringResource(labelResource)
                                        Box(
                                            Modifier.size(48.dp)
                                                .clip(CircleShape)
                                                .selectable(
                                                    selected,
                                                    role = Role.RadioButton,
                                                    onClick = {
                                                        viewModel.updateCrosshairColor(color)
                                                    },
                                                )
                                                .semantics { contentDescription = label },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                Modifier.size(36.dp)
                                                    .background(Color(color), CircleShape)
                                                    .border(
                                                        2.dp,
                                                        MaterialTheme.colorScheme.outlineVariant,
                                                        CircleShape,
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (selected)
                                                    Icon(
                                                        Icons.Rounded.Check,
                                                        null,
                                                        Modifier.size(22.dp),
                                                        tint = Color.Black,
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SettingsSlider(
                            stringResource(R.string.crosshair_size_title),
                            viewModel.crosshairSize.toFloat(),
                            stringResource(R.string.settings_value_dp, viewModel.crosshairSize),
                            24f..64f,
                            { viewModel.updateCrosshairSize(it.roundToInt()) },
                        )
                        SettingsSlider(
                            stringResource(R.string.crosshair_opacity_title),
                            viewModel.crosshairOpacity,
                            stringResource(
                                R.string.settings_value_percent,
                                (viewModel.crosshairOpacity * 100).roundToInt(),
                            ),
                            0.2f..1f,
                            viewModel::updateCrosshairOpacity,
                        )
                    }
                    SettingsGroup(
                        stringResource(R.string.crosshair_position),
                        Icons.Rounded.OpenWith,
                    ) {
                        SettingsSlider(
                            stringResource(R.string.crosshair_offset_x_title),
                            viewModel.crosshairOffsetX.toFloat(),
                            stringResource(R.string.settings_value_dp, viewModel.crosshairOffsetX),
                            -100f..100f,
                            {
                                viewModel.updateCrosshairOffsets(
                                    it.roundToInt(),
                                    viewModel.crosshairOffsetY,
                                )
                            },
                        )
                        SettingsSlider(
                            stringResource(R.string.crosshair_offset_y_title),
                            viewModel.crosshairOffsetY.toFloat(),
                            stringResource(R.string.settings_value_dp, viewModel.crosshairOffsetY),
                            -100f..100f,
                            {
                                viewModel.updateCrosshairOffsets(
                                    viewModel.crosshairOffsetX,
                                    it.roundToInt(),
                                )
                            },
                        )
                        SettingsAction(
                            stringResource(R.string.crosshair_reset_position_title),
                            null,
                        ) {
                            viewModel.updateCrosshairOffsets(0, 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrosshairPreview(viewModel: SettingsViewModel) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val label = stringResource(R.string.crosshair_preview)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(
            Modifier.fillMaxWidth().height(288.dp).semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 24.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                var x = center.x % step
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height))
                    x += step
                }
                var y = center.y % step
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y))
                    y += step
                }
                drawCircle(
                    gridColor,
                    radius = 72.dp.toPx(),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                )
            }
            Icon(
                painterResource(crosshairStyles[(viewModel.crosshairStyle - 1).coerceIn(0, 7)]),
                null,
                Modifier.absoluteOffset(
                        viewModel.crosshairOffsetX.dp,
                        viewModel.crosshairOffsetY.dp,
                    )
                    .size(viewModel.crosshairSize.coerceIn(24, 64).dp)
                    .alpha(viewModel.crosshairOpacity.coerceIn(0.2f, 1f)),
                tint = Color(viewModel.crosshairColor),
            )
            Surface(
                Modifier.align(Alignment.TopStart).padding(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                )
            }
        }
    }
}
