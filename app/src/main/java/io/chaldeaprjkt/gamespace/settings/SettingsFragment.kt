/*
 * Copyright (C) 2026 crDroid Android Project
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

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemProperties
import android.os.Vibrator
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.ListPreference

import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.crdroid.settings.preferences.CustomSeekBarPreference

import dagger.hilt.android.AndroidEntryPoint

import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.GameOptimizationManager
import io.chaldeaprjkt.gamespace.preferences.AppListPreferences
import io.chaldeaprjkt.gamespace.preferences.QuickStartAppPreference
import io.chaldeaprjkt.gamespace.preferences.QuickStartAppPreferenceDialogFragment
import io.chaldeaprjkt.gamespace.preferences.appselector.AppSelectorActivity

import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint(SettingsBasePreferenceFragment::class)
class SettingsFragment : Hilt_SettingsFragment(),
    QuickStartAppPreferenceDialogFragment.QuickStartAppListener,
    Preference.OnPreferenceChangeListener {

    private var apps: AppListPreferences? = null

    @Inject
    lateinit var gameOptimization: GameOptimizationManager

    @Inject
    lateinit var appSettings: AppSettings

    private val selectorResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            apps?.useSelectorResult(it)
        }

    private val perAppResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            apps?.usePerAppResult(it)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        updatePreferences()
        updateCrosshairPreferences()
    }

    private fun updateCrosshairPreferences() {
        findPreference<SwitchPreferenceCompat>(AppSettings.KEY_CROSSHAIR_ENABLED)?.apply {
            isChecked = appSettings.crosshairEnabled
            setOnPreferenceChangeListener { _, value ->
                appSettings.crosshairEnabled = value as Boolean
                true
            }
        }

        findPreference<ListPreference>(AppSettings.KEY_CROSSHAIR_STYLE)?.apply {
            entries = (1..8).map { getString(R.string.crosshair_style_entry, it) }.toTypedArray()
            entryValues = (1..8).map { it.toString() }.toTypedArray()
        }
        bindCrosshairList(AppSettings.KEY_CROSSHAIR_STYLE, appSettings.crosshairStyle) {
            appSettings.crosshairStyle = it
        }
        bindCrosshairList(AppSettings.KEY_CROSSHAIR_COLOR, appSettings.crosshairColor) {
            appSettings.crosshairColor = it
        }
        bindCrosshairSlider(AppSettings.KEY_CROSSHAIR_SIZE, appSettings.crosshairSize) {
            appSettings.crosshairSize = it
        }
        bindCrosshairSlider(
            AppSettings.KEY_CROSSHAIR_OPACITY,
            (appSettings.crosshairOpacity * 100).roundToInt()
        ) {
            appSettings.crosshairOpacity = it / 100f
        }
        bindCrosshairSlider(AppSettings.KEY_CROSSHAIR_OFFSET_X, appSettings.crosshairOffsetX) {
            appSettings.crosshairOffsetX = it
        }
        bindCrosshairSlider(AppSettings.KEY_CROSSHAIR_OFFSET_Y, appSettings.crosshairOffsetY) {
            appSettings.crosshairOffsetY = it
        }
        findPreference<Preference>("crosshair_reset_position")?.setOnPreferenceClickListener {
            appSettings.crosshairOffsetX = 0
            appSettings.crosshairOffsetY = 0
            updateCrosshairPreferences()
            true
        }
    }

    // Persist through AppSettings to retain numeric types and per-game overrides.
    private fun bindCrosshairList(key: String, currentValue: Int, update: (Int) -> Unit) {
        findPreference<ListPreference>(key)?.apply {
            value = currentValue.toString()
            setOnPreferenceChangeListener { _, value ->
                update((value as String).toInt())
                true
            }
        }
    }

    private fun bindCrosshairSlider(key: String, currentValue: Int, update: (Int) -> Unit) {
        findPreference<CustomSeekBarPreference>(key)?.apply {
            value = currentValue
            setOnPreferenceChangeListener { _, value ->
                update(value as Int)
                true
            }
        }
    }

    private fun hasVibrator(): Boolean {
        val vibrator = context?.getSystemService(Vibrator::class.java)
        return vibrator?.hasVibrator() == true
    }

    private fun updatePreferences() {
        val isBypassSupported =
            Build.MANUFACTURER.equals("Google", ignoreCase = true) ||
            SystemProperties.getBoolean("persist.sys.battery_bypass_supported", false)

        if (!isBypassSupported) {
            findPreference<PreferenceCategory>("in_game_preferences")
                ?.removePreference(findPreference("bypass_charge_enabled")!!)
        }

        if (!hasVibrator()) {
            findPreference<PreferenceCategory>("in_game_preferences")
                ?.removePreference(findPreference("gamespace_pulse_bass_haptics_disabled")!!)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apps = findPreference("gamespace_game_list")
        apps?.onRegisteredAppClick { pkg ->
            perAppResult.launch(
                Intent(context, PerAppSettingsActivity::class.java).apply {
                    putExtra(PerAppSettingsActivity.EXTRA_PACKAGE, pkg)
                }
            )
        }

        findPreference<Preference>(AppListPreferences.KEY_ADD_GAME)
            ?.setOnPreferenceClickListener {
                selectorResult.launch(Intent(context, AppSelectorActivity::class.java))
                true
            }

        // Game Optimization preferences
        findPreference<SwitchPreferenceCompat>("game_memory_management")?.apply {
            isChecked = gameOptimization.isMemoryManagementEnabled
            onPreferenceChangeListener = this@SettingsFragment
        }

        findPreference<SwitchPreferenceCompat>("game_cache_management")?.apply {
            isChecked = gameOptimization.isCacheManagementEnabled
            onPreferenceChangeListener = this@SettingsFragment
        }
    }

    override fun onResume() {
        super.onResume()
        apps?.updateAppList()
        updateCrosshairPreferences()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is QuickStartAppPreference) {
            val dialogFragment =
                QuickStartAppPreferenceDialogFragment.newInstance(preference.key)
            dialogFragment.setListener(this)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, "QuickStartAppPreferenceDialogFragment")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun getSavedQuickStartApps(): String {
        val prefs = preferenceManager.sharedPreferences ?: return ""
        return prefs.getString(io.chaldeaprjkt.gamespace.data.AppSettings.KEY_QUICK_START_APPS, "") ?: ""
    }

    override fun saveQuickStartApps(apps: String) { /* no-op */ }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            "game_memory_management" -> {
                gameOptimization.isMemoryManagementEnabled = newValue as Boolean
                return true
            }
            "game_cache_management" -> {
                gameOptimization.isCacheManagementEnabled = newValue as Boolean
                return true
            }
        }
        return false
    }
}
