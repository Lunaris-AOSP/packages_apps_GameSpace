/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package io.chaldeaprjkt.gamespace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.android.axion.compose.preferences.ExpressiveShapes
import com.android.axion.compose.theme.AxionTheme

@Composable
fun GameSpaceTheme(content: @Composable () -> Unit) {
    AxionTheme { MaterialTheme(shapes = ExpressiveShapes, content = content) }
}
