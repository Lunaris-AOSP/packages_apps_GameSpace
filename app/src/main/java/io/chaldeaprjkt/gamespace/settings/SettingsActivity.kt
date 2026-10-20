/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * Copyright (C) 2021 Chaldeaprjkt
 * Copyright (C) 2022-2026 crDroid Android Project
 * Copyright (C) 2025-2026 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.chaldeaprjkt.gamespace.ui.screens.SettingsScreen
import io.chaldeaprjkt.gamespace.ui.theme.GameSpaceTheme
import io.chaldeaprjkt.gamespace.ui.viewmodel.SettingsViewModel

@AndroidEntryPoint(ComponentActivity::class)
class SettingsActivity : Hilt_SettingsActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameSpaceTheme { SettingsScreen(viewModel = viewModel, onBack = { finish() }) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
