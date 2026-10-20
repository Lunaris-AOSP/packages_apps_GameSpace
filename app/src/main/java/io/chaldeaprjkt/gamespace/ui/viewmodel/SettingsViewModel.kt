/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package io.chaldeaprjkt.gamespace.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.os.UserHandle
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.GameConfig
import io.chaldeaprjkt.gamespace.data.GameOptimizationManager
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RegisteredGame(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val mode: Int,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val systemSettings: SystemSettings,
    private val gameModeUtils: GameModeUtils,
    private val gameOptimization: GameOptimizationManager,
) : ViewModel() {

    var callOverlayEnabled by mutableStateOf(appSettings.callOverlayEnabled)
        private set

    var danmakuNotification by mutableStateOf(appSettings.danmakuNotification)
        private set

    var callsMode by mutableIntStateOf(appSettings.callsMode)
        private set

    var ringerMode by mutableIntStateOf(appSettings.ringerMode)
        private set

    var noAutoBrightness by mutableStateOf(appSettings.noAutoBrightness)
        private set

    var noThreeScreenshot by mutableStateOf(appSettings.noThreeScreenshot)
        private set

    var stayAwake by mutableStateOf(appSettings.stayAwake)
        private set

    var registeredGames by mutableStateOf<List<RegisteredGame>>(emptyList())
        private set

    var quickStartApps by mutableStateOf(appSettings.quickStartApps)
        private set

    var bypassChargeEnabled by mutableStateOf(systemSettings.bypassChargeEnabled)
        private set

    var iconIdleAlpha by mutableFloatStateOf(appSettings.iconIdleAlpha.toFloat())
        private set

    var autoDnd by mutableStateOf(appSettings.autoDnd)
        private set

    var musicPlayerEnabled by mutableStateOf(appSettings.musicPlayerEnabled)
        private set

    var crosshairEnabled by mutableStateOf(appSettings.crosshairEnabled)
        private set

    var crosshairStyle by mutableIntStateOf(appSettings.crosshairStyle)
        private set

    var crosshairSize by mutableIntStateOf(appSettings.crosshairSize)
        private set

    var crosshairColor by mutableIntStateOf(appSettings.crosshairColor)
        private set

    var crosshairOpacity by mutableFloatStateOf(appSettings.crosshairOpacity)
        private set

    var crosshairOffsetX by mutableIntStateOf(appSettings.crosshairOffsetX)
        private set

    var crosshairOffsetY by mutableIntStateOf(appSettings.crosshairOffsetY)
        private set

    val isBypassSupported =
        Build.MANUFACTURER.equals("Google", ignoreCase = true) ||
            SystemProperties.getBoolean("persist.sys.battery_bypass_supported", false)

    val hasVibrator = context.getSystemService(Vibrator::class.java)?.hasVibrator() == true

    var autoGameDetect by mutableStateOf(systemSettings.autoGameDetect)
        private set

    var lockGesture by mutableStateOf(appSettings.lockGesture)
        private set

    var noPulseBassHaptics by mutableStateOf(appSettings.noPulseBassHaptics)
        private set

    var memoryManagement by mutableStateOf(gameOptimization.isMemoryManagementEnabled)
        private set

    var cacheManagement by mutableStateOf(gameOptimization.isCacheManagementEnabled)
        private set

    var availableApps by mutableStateOf<List<AppItem>>(emptyList())
        private set

    var appsLoading by mutableStateOf(false)
        private set

    var appsLoadFailed by mutableStateOf(false)
        private set

    var gamesLoading by mutableStateOf(true)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    private var gamesJob: Job? = null
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshPreferences() }
    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refresh()
            }
        }

    init {
        appSettings.registerListener(preferenceListener)
        listOf("gamespace_game_list", "gamespace_auto_game_detect", "bypass_charge_enabled")
            .forEach {
                context.contentResolver.registerContentObserver(
                    Settings.System.getUriFor(it),
                    false,
                    settingsObserver,
                    UserHandle.USER_CURRENT,
                )
            }
    }

    fun refresh() {
        refreshPreferences()
        loadRegisteredGames()
    }

    private fun refreshPreferences() {
        callOverlayEnabled = appSettings.callOverlayEnabled
        danmakuNotification = appSettings.danmakuNotification
        callsMode = appSettings.callsMode
        ringerMode = appSettings.ringerMode
        noAutoBrightness = appSettings.noAutoBrightness
        noThreeScreenshot = appSettings.noThreeScreenshot
        stayAwake = appSettings.stayAwake
        quickStartApps = appSettings.quickStartApps
        bypassChargeEnabled = systemSettings.bypassChargeEnabled
        iconIdleAlpha = appSettings.iconIdleAlpha.toFloat()
        autoDnd = appSettings.autoDnd
        musicPlayerEnabled = appSettings.musicPlayerEnabled
        crosshairEnabled = appSettings.crosshairEnabled
        crosshairStyle = appSettings.crosshairStyle
        crosshairSize = appSettings.crosshairSize
        crosshairColor = appSettings.crosshairColor
        crosshairOpacity = appSettings.crosshairOpacity
        crosshairOffsetX = appSettings.crosshairOffsetX
        crosshairOffsetY = appSettings.crosshairOffsetY
        autoGameDetect = systemSettings.autoGameDetect
        lockGesture = appSettings.lockGesture
        noPulseBassHaptics = appSettings.noPulseBassHaptics
        memoryManagement = gameOptimization.isMemoryManagementEnabled
        cacheManagement = gameOptimization.isCacheManagementEnabled
    }

    fun updateAutoGameDetect(enabled: Boolean) {
        systemSettings.autoGameDetect = enabled
        autoGameDetect = enabled
    }

    fun updateLockGesture(enabled: Boolean) {
        appSettings.lockGesture = enabled
        lockGesture = enabled
    }

    fun updateNoPulseBassHaptics(disabled: Boolean) {
        appSettings.noPulseBassHaptics = disabled
        noPulseBassHaptics = disabled
    }

    fun updateMemoryManagement(enabled: Boolean) {
        gameOptimization.isMemoryManagementEnabled = enabled
        memoryManagement = enabled
    }

    fun updateCacheManagement(enabled: Boolean) {
        gameOptimization.isCacheManagementEnabled = enabled
        cacheManagement = enabled
    }

    override fun onCleared() {
        appSettings.unregisterListener(preferenceListener)
        context.contentResolver.unregisterContentObserver(settingsObserver)
        super.onCleared()
    }

    fun updateCrosshairEnabled(enabled: Boolean) {
        crosshairEnabled = enabled
        appSettings.crosshairEnabled = enabled
    }

    fun updateCrosshairStyle(style: Int) {
        crosshairStyle = style
        appSettings.crosshairStyle = style
    }

    fun updateCrosshairSize(size: Int) {
        crosshairSize = size
        appSettings.crosshairSize = size
    }

    fun updateCrosshairColor(color: Int) {
        crosshairColor = color
        appSettings.crosshairColor = color
    }

    fun updateCrosshairOpacity(opacity: Float) {
        crosshairOpacity = opacity
        appSettings.crosshairOpacity = opacity
    }

    fun updateCrosshairOffsets(x: Int, y: Int) {
        crosshairOffsetX = x
        crosshairOffsetY = y
        appSettings.crosshairOffsetX = x
        appSettings.crosshairOffsetY = y
    }

    fun updateCallOverlay(enabled: Boolean) {
        callOverlayEnabled = enabled
        appSettings.callOverlayEnabled = enabled
    }

    fun updateDanmakuNotification(enabled: Boolean) {
        danmakuNotification = enabled
        appSettings.danmakuNotification = enabled
    }

    fun updateCallsMode(mode: Int) {
        callsMode = mode
        appSettings.callsMode = mode
    }

    fun updateRingerMode(mode: Int) {
        ringerMode = mode
        appSettings.ringerMode = mode
    }

    fun updateNoAutoBrightness(disabled: Boolean) {
        noAutoBrightness = disabled
        appSettings.noAutoBrightness = disabled
    }

    fun updateNoThreeScreenshot(disabled: Boolean) {
        noThreeScreenshot = disabled
        appSettings.noThreeScreenshot = disabled
    }

    fun updateStayAwake(enabled: Boolean) {
        stayAwake = enabled
        appSettings.stayAwake = enabled
    }

    fun updateQuickStartApps(apps: String) {
        quickStartApps = apps
        appSettings.quickStartApps = apps
    }

    fun updateBypassChargeEnabled(enabled: Boolean) {
        bypassChargeEnabled = enabled
        systemSettings.bypassChargeEnabled = enabled
    }

    fun updateMusicPlayerEnabled(enabled: Boolean) {
        musicPlayerEnabled = enabled
        appSettings.musicPlayerEnabled = enabled
    }

    fun updateIconIdleAlpha(alpha: Float) {
        iconIdleAlpha = alpha
        appSettings.iconIdleAlpha = alpha.toInt()
    }

    fun updateAutoDnd(enabled: Boolean) {
        autoDnd = enabled
        appSettings.autoDnd = enabled
    }

    fun loadRegisteredGames() {
        gamesJob?.cancel()
        gamesJob =
            viewModelScope.launch {
                registeredGames =
                    withContext(Dispatchers.IO) {
                        val pm = context.packageManager
                        systemSettings.userGames
                            .mapNotNull { game ->
                                try {
                                    val info =
                                        pm.getApplicationInfo(
                                            game.packageName,
                                            PackageManager.ApplicationInfoFlags.of(0),
                                        )
                                    RegisteredGame(
                                        game.packageName,
                                        info.loadLabel(pm).toString(),
                                        info.loadIcon(pm),
                                        game.mode,
                                    )
                                } catch (_: PackageManager.NameNotFoundException) {
                                    null
                                }
                            }
                            .sortedBy { it.label.lowercase() }
                    }
                gamesLoading = false
            }
    }

    fun loadAvailableApps() {
        if (appsLoading) return
        appsLoading = true
        appsLoadFailed = false
        viewModelScope.launch {
            try {
                availableApps =
                    withContext(Dispatchers.IO) {
                        val pm = context.packageManager
                        val intent =
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
                            .mapNotNull { it.activityInfo?.applicationInfo }
                            .filter { it.packageName != context.packageName }
                            .distinctBy { it.packageName }
                            .map {
                                AppItem(
                                    it.packageName,
                                    it.loadLabel(pm).toString(),
                                    it.loadIcon(pm),
                                )
                            }
                            .sortedBy { it.label.lowercase() }
                    }
            } catch (_: SecurityException) {
                appsLoadFailed = true
            } finally {
                appsLoading = false
            }
        }
    }

    private fun updateDeniedList(packageName: String, denied: Boolean) {
        val resolver = context.contentResolver
        val packages =
            Settings.System.getStringForUser(
                    resolver,
                    "gamespace_denied_list",
                    UserHandle.USER_CURRENT,
                )
                .orEmpty()
                .split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
        if (denied) packages.add(packageName) else packages.remove(packageName)
        Settings.System.putStringForUser(
            resolver,
            "gamespace_denied_list",
            packages.joinToString(";"),
            UserHandle.USER_CURRENT,
        )
    }

    fun registerGame(packageName: String) {
        updateDeniedList(packageName, false)
        val games = systemSettings.userGames.toMutableList()
        if (games.none { it.packageName == packageName }) {
            games.add(UserGame(packageName))
            systemSettings.userGames = games
            gameModeUtils.setIntervention(packageName, GameConfig.ModeBuilder.build())
        }
        loadRegisteredGames()
    }

    fun unregisterGame(packageName: String) {
        val info =
            try {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        if (info?.category == ApplicationInfo.CATEGORY_GAME) updateDeniedList(packageName, true)
        gameModeUtils.setAngleDriverChoice(packageName, GameModeUtils.DRIVER_CHOICE_DEFAULT)
        systemSettings.userGames =
            systemSettings.userGames.filterNot { it.packageName == packageName }
        gameModeUtils.setIntervention(packageName, null)
        loadRegisteredGames()
    }

    fun launchGame(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            message = context.getString(R.string.game_launch_unavailable)
            return
        }
        try {
            context.startActivity(
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } catch (_: android.content.ActivityNotFoundException) {
            message = context.getString(R.string.game_launch_unavailable)
        }
    }

    fun clearMessage() {
        message = null
    }

    fun updateGameMode(packageName: String, mode: Int) {
        gameModeUtils.setGameModeFor(packageName, systemSettings, mode)
        loadRegisteredGames()
    }

    val angleFeatureAvailable: Boolean
        get() =
            gameModeUtils.findAnglePackage()?.isEnabled == true && gameModeUtils.isVulkanSupported()

    fun getAngleDriverChoice(packageName: String): String =
        gameModeUtils.getAngleDriverChoice(packageName)

    fun updateAngleDriverChoice(packageName: String, choice: String) {
        gameModeUtils.setAngleDriverChoice(packageName, choice)
    }
}
