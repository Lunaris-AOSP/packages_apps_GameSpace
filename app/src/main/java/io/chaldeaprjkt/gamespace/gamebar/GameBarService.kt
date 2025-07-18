/*
 * Copyright (C) 2021 Chaldeaprjkt
 *               2022 crDroid Android Project
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
package io.chaldeaprjkt.gamespace.gamebar

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.android.systemui.screenrecord.IRecordingCallback
import dagger.hilt.android.AndroidEntryPoint
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.GameOptimizationManager
import io.chaldeaprjkt.gamespace.settings.SettingsActivity
import io.chaldeaprjkt.gamespace.utils.ScreenUtils
import io.chaldeaprjkt.gamespace.utils.dp
import io.chaldeaprjkt.gamespace.utils.registerDraggableTouchListener
import io.chaldeaprjkt.gamespace.utils.statusbarHeight
import io.chaldeaprjkt.gamespace.widget.MenuSwitcher
import io.chaldeaprjkt.gamespace.widget.PanelView
import javax.inject.Inject

@AndroidEntryPoint(Service::class)
class GameBarService : Hilt_GameBarService() {
    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var screenUtils: ScreenUtils

    @Inject
    lateinit var danmakuService: DanmakuService

    @Inject
    lateinit var gameOptimization: GameOptimizationManager

    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val barLayoutParam =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            preferMinimalPostProcessing = true
            gravity = Gravity.TOP
        }

    private val panelLayoutParam =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            gravity = Gravity.CENTER_VERTICAL
        }

    private lateinit var rootBarView: View
    private lateinit var barView: LinearLayout
    private lateinit var menuSwitcher: MenuSwitcher
    private lateinit var rootPanelView: LinearLayout
    private lateinit var panelView: PanelView
    private val binder = GameBarBinder()
    private val firstPaint = Runnable { initActions() }
    private var barExpanded: Boolean = false
        set(value) {
            field = value
            menuSwitcher.updateIconState(value, barLayoutParam.x)
            barView.children.forEach {
                if (it.id != R.id.action_menu_switcher) {
                    it.isVisible = value
                }
            }
            updateBackground()
            updateContainerGaps()
        }

    private var showPanel: Boolean = false
        set(value) {
            field = value
            if (value) {
                if (!::rootPanelView.isInitialized) {
                    setupPanelView()
                }
                if (!rootPanelView.isAttachedToWindow) {
                    wm.addView(rootPanelView, panelLayoutParam)
                    rootPanelView.alpha = 0f
                    rootPanelView.visibility = View.VISIBLE
                    rootPanelView.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                } else {
                    wm.updateViewLayout(rootPanelView, panelLayoutParam)
                }
            } else {
                if (::rootPanelView.isInitialized && rootPanelView.isAttachedToWindow) {
                    rootPanelView.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            rootPanelView.visibility = View.INVISIBLE
                            handler.postDelayed({
                                runCatching {
                                    if (rootPanelView.isAttachedToWindow) {
                                        wm.removeView(rootPanelView)
                                    }
                                }.onFailure { it.printStackTrace() }
                            }, 50)
                        }
                        .start()
                }
            }
        }

    private var barAdded = false

    // Whether to ignore the initActions (floating action) or not
    private var shouldClose = false

    override fun onCreate() {
        super.onCreate()
        val frame = FrameLayout(this)
        rootBarView = LayoutInflater.from(this)
            .inflate(R.layout.window_util, frame, false)!!
        barView = rootBarView.findViewById(R.id.container_bar)!!
        barView.alpha = appSettings.menuOpacity / 100f
        menuSwitcher = rootBarView.findViewById(R.id.action_menu_switcher)!!
        menuSwitcher.alpha = appSettings.menuOpacity / 100f
        danmakuService.init()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> onGameLeave()
            ACTION_START -> onGameStart()
        }
        intent?.getStringExtra("package")?.let { packageName ->
            // Apply game optimizations when game starts
            gameOptimization.optimizeGameLaunch(packageName)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent) = binder

    inner class GameBarBinder : Binder() {
        fun getService() = this@GameBarService
    }

    override fun onDestroy() {
        onGameLeave()
        danmakuService.destroy()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!rootBarView.isVisible) {
            handler.removeCallbacks(firstPaint)
            handler.postDelayed({
                firstPaint.run()
                dockCollapsedMenu()
            }, 100)
        } else {
            dockCollapsedMenu()
        }
        danmakuService.updateConfiguration(newConfig)
    }

    // for client service
    fun onGameStart() {
        shouldClose = false
        rootBarView.isVisible = false
        rootBarView.alpha = 0f
        updateRootBarView()
        handler.postDelayed(firstPaint, 500)
    }

    fun onGameLeave() {
        shouldClose = true

        handler.removeCallbacksAndMessages(null)

        runCatching {
            if (::rootPanelView.isInitialized 
                    && rootPanelView.isAttachedToWindow) {
                wm.removeViewImmediate(rootPanelView)
            }
        }.onFailure { it.printStackTrace() }

        runCatching {
            if (::rootBarView.isInitialized 
                    && rootBarView.isAttachedToWindow) {
                wm.removeViewImmediate(rootBarView)
            }
        }.onFailure { it.printStackTrace() }

        barAdded = false

        stopForeground(true)
        stopSelf()
    }

    private fun updateRootBarView() {
        if (!::rootBarView.isInitialized) return

        runCatching {
            if (barAdded) {
                wm.removeViewImmediate(rootBarView)
            }
            wm.addView(rootBarView, barLayoutParam)
            barAdded = true
        }.onFailure {
            it.printStackTrace()
            runCatching {
                if (barAdded) {
                    wm.updateViewLayout(rootBarView, barLayoutParam)
                }
            }.onFailure { err -> err.printStackTrace() }
        }
    }

    private fun updateLayout(with: (WindowManager.LayoutParams) -> Unit = {}) {
        if (rootBarView.isAttachedToWindow) {
            wm.updateViewLayout(rootBarView, barLayoutParam.apply(with))
        }
    }

    private fun initActions() {
        if (shouldClose) return
        rootBarView.isVisible = true
        rootBarView.animate()
            .alpha(1f)
            .apply { duration = 300 }
            .start()
        barExpanded = false
        barLayoutParam.x = appSettings.x
        barLayoutParam.y = appSettings.y
        dockCollapsedMenu()

        menuSwitcherButton()
        panelButton()
        screenshotButton()
        recorderButton()
    }

    private fun updateBackground() {
        val barDragged = !barExpanded && barView.translationX == 0f
        val collapsedAtStart = !barDragged && barLayoutParam.x < 0
        val collapsedAtEnd = !barDragged && barLayoutParam.x > 0
        barView.setBackgroundResource(
            when {
                barExpanded -> R.drawable.bar_expanded
                collapsedAtStart -> R.drawable.bar_collapsed_start
                collapsedAtEnd -> R.drawable.bar_collapsed_end
                else -> R.drawable.bar_dragged
            }
        )
    }

    private fun updateContainerGaps() {
        if (barExpanded) {
            barView.updatePadding(8, 8, 8, 8)
            (barView.layoutParams as ViewGroup.MarginLayoutParams)
                .updateMargins(right = 48, left = 48)
        } else {
            barView.updatePadding(0, 0, 0, 0)
            (barView.layoutParams as ViewGroup.MarginLayoutParams)
                .updateMargins(right = 0, left = 0)
        }
    }

    private fun dockCollapsedMenu() {
        val halfWidth = wm.maximumWindowMetrics.bounds.width() / 2
        if (barLayoutParam.x < 0) {
            barView.translationX = -22f
            barLayoutParam.x = -halfWidth
        } else {
            barView.translationX = 22f
            barLayoutParam.x = halfWidth
        }

        val safeArea = statusbarHeight + 4.dp
        val safeHeight = wm.maximumWindowMetrics.bounds.height() - safeArea
        barLayoutParam.y = barLayoutParam.y.coerceIn(safeArea, safeHeight)

        updateBackground()
        updateContainerGaps()
        menuSwitcher.showFps = if (barExpanded) false else appSettings.showFps
        menuSwitcher.updateIconState(barExpanded, barLayoutParam.x)
        updateRootBarView()
    }

    private fun setupPanelView() {
        rootPanelView = LayoutInflater.from(this)
            .inflate(R.layout.window_panel, FrameLayout(this), false) as LinearLayout
        rootPanelView.alpha = 0f
        rootPanelView.visibility = View.INVISIBLE

        panelView = rootPanelView.findViewById(R.id.panel_view)!!
        panelView.alpha = appSettings.menuOpacity / 100f

        rootPanelView.setOnClickListener {
            showPanel = false
        }

        val barWidth = barView.width + barView.marginStart
        if (barLayoutParam.x < 0) {
            rootPanelView.gravity = Gravity.START
            rootPanelView.setPaddingRelative(barWidth, 16, 16, 16)
        } else {
            rootPanelView.gravity = Gravity.END
            rootPanelView.setPaddingRelative(16, 16, barWidth, 16)
        }
    }

    private fun takeShot() {
        val afterShot: () -> Unit = {
            barExpanded = false
            handler.postDelayed({
                updateLayout { it.alpha = 1f }
            }, 100)
        }

        updateLayout { it.alpha = 0f }
        handler.postDelayed({
            runCatching {
                screenUtils.takeScreenshot { afterShot() }
            }.onFailure {
                it.printStackTrace()
                afterShot()
            }
        }, 250)
    }

    private fun menuSwitcherButton() {
        menuSwitcher.setOnClickListener {
            barExpanded = !barExpanded
        }
        menuSwitcher.registerDraggableTouchListener(
            initPoint = { Point(barLayoutParam.x, barLayoutParam.y) },
            listener = { x, y ->
                if (!menuSwitcher.isDragged) {
                    menuSwitcher.isDragged = true
                    barView.translationX = 0f
                }
                updateLayout {
                    it.x = x
                    it.y = y
                }
                updateBackground()
            },
            onComplete = {
                menuSwitcher.isDragged = false
                dockCollapsedMenu()
                updateBackground()
                appSettings.x = barLayoutParam.x
                appSettings.y = barLayoutParam.y
            }
        )
    }

    private fun panelButton() {
        val actionPanel = rootBarView.findViewById<ImageButton>(R.id.action_panel)!!
        actionPanel.alpha = appSettings.menuOpacity / 100f
        actionPanel.setOnClickListener {
            showPanel = !showPanel
        }
        actionPanel.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }
    }

    private fun screenshotButton() {
        val actionScreenshot = rootBarView.findViewById<ImageButton>(R.id.action_screenshot)!!
        actionScreenshot.alpha = appSettings.menuOpacity / 100f
        actionScreenshot.setOnClickListener {
            takeShot()
        }
    }

    private fun recorderButton() {
        val actionRecorder = rootBarView.findViewById<ImageButton>(R.id.action_record)!!
        actionRecorder.alpha = appSettings.menuOpacity / 100f
        val recorder = screenUtils.recorder ?: let { actionRecorder?.isVisible = false; return }
        recorder.addRecordingCallback(object : IRecordingCallback.Stub() {
            override fun onRecordingStart() {
                handler.post {
                    actionRecorder.isSelected = true
                }
            }

            override fun onRecordingEnd() {
                handler.post {
                    actionRecorder.isSelected = false
                }
            }
        })
        actionRecorder.setOnClickListener {
            if (recorder.isStarting) {
                return@setOnClickListener
            }

            if (!recorder.isRecording) {
                recorder.startRecording()
            } else {
                recorder.stopRecording()
            }

            barExpanded = false
        }
    }

    companion object {
        const val TAG = "GameBar"
        const val ACTION_START = "GameBar.ACTION_START"
        const val ACTION_STOP = "GameBar.ACTION_STOP"
    }
}
