/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.chaldeaprjkt.gamespace.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.chaldeaprjkt.gamespace.R

@Composable
internal fun SettingsNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val labels =
        listOf(R.string.settings_tab_games, R.string.settings_tab_settings, R.string.tile_crosshair)
    val icons = listOf(Icons.Rounded.SportsEsports, Icons.Rounded.Tune, Icons.Rounded.GpsFixed)
    val view = LocalView.current
    var pressedTab by remember { mutableIntStateOf(-1) }
    var tapCount by remember { mutableIntStateOf(0) }
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    // Match Optimization Engine's expanding tab segments, adapted for three destinations.
    val weights =
        labels.indices.map { index ->
            val restingWeight = if (selected == index) 1.15f else 0.90f
            val weight = remember(index) { Animatable(restingWeight) }
            LaunchedEffect(tapCount, selected, settleSpec) {
                if (tapCount > 0 && pressedTab == selected) {
                    val pushedWeight = if (pressedTab == index) 1.30f else 0.70f
                    weight.animateTo(pushedWeight, tween(100, easing = FastOutSlowInEasing))
                }
                weight.animateTo(restingWeight, settleSpec)
            }
            weight.value
        }

    Box(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 6.dp,
        ) {
            BoxWithConstraints {
                val stacked = maxWidth < 400.dp || LocalDensity.current.fontScale > 1.3f
                Row(
                    Modifier.padding(8.dp).heightIn(min = 52.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    labels.forEachIndexed { index, label ->
                        SettingsNavigationItem(
                            label = stringResource(label),
                            icon = icons[index],
                            selected = index == selected,
                            stacked = stacked,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                pressedTab = index
                                tapCount++
                                onSelect(index)
                            },
                            modifier = Modifier.weight(weights[index]),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    stacked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by
        animateColorAsState(
            if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "navigation_container",
        )
    val contentColor by
        animateColorAsState(
            if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "navigation_content",
        )
    val labelStyle =
        if (selected) MaterialTheme.typography.labelLargeEmphasized
        else MaterialTheme.typography.labelLarge
    Surface(modifier, shape = CircleShape, color = containerColor, contentColor = contentColor) {
        val itemModifier =
            Modifier.clip(CircleShape)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = if (stacked) 6.dp else 12.dp, vertical = 8.dp)
        if (stacked) {
            Column(
                itemModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(icon, null, Modifier.size(24.dp))
                Text(label, style = labelStyle, textAlign = TextAlign.Center)
            }
        } else {
            Row(
                itemModifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, Modifier.size(24.dp))
                Text(label, style = labelStyle)
            }
        }
    }
}
