package io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.prism.camera.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.prism.camera.R
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ThreePrismCameraApplication
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraEngine
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraExclusivity
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraExecutors
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.MainThread
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.SessionDefaults
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.CameraPreferences
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.appearance.LauncherIconMode
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.appearance.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.appearance.ThemeColorValue
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.MultiChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.PrismScreen
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.SwitchRow
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.ThemeColorDialog
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.update.UpdateDialogs
import java.util.Locale
import org.autojs.plugin.camera.api.CameraContract
import org.autojs.plugin.camera.api.CameraResultKeys

/**
 * The settings screen (roadmap P3.2, standalone settings specification, precedent 3-Macaw Speech), opened from the
 * viewfinder only: appearance (language, night mode, theme color, launcher icon; the first three follow AutoJs6 by
 * default), camera (permissions, the default lens, analysis size and frame rate that script requests omitting them fall
 * back to, the foreground service notification; appendix D Q41 = (b)), diagnostics (CameraX version, lenses and their
 * capabilities, the current holder) and about (version, release history, the in-app update check with its automatic
 * check and ignored releases; appendix D Q42 = (b), Q46: the automatic check runs when this page opens). Every choice
 * dialog edits a draft and saves once on confirm.
 * zh-CN: 设置页 (路线图 P3.2, 独立设置规范, 先例 3-Macaw Speech), 只从取景页打开: 外观 (语言, 夜间模式, 主题色, 启动器图标; 前三项默认跟随
 * AutoJs6), 相机 (权限, 省略对应键的脚本请求所回退的默认镜头, 分析分辨率与帧率, 前台服务通知; 附录 D Q41 = (b)), 诊断 (CameraX 版本, 镜头及其
 * 能力, 当前持有者) 与关于 (版本, 发行历史, 应用内更新检查及其自动检查与已忽略版本; 附录 D Q42 = (b), Q46: 自动检查在打开本页时运行). 每个选择
 * 对话框编辑草稿, 确定时保存一次.
 */
class SettingsActivity : StandaloneActivity() {

    private enum class Dialog { LANGUAGE, DARK_MODE, THEME_COLOR, LAUNCHER_ICON, DEFAULT_LENS, ANALYSIS_SIZE, ANALYSIS_FPS, LENSES, IGNORED }

    /** The lenses read for the diagnostics; null while loading. zh-CN: 诊断读取的镜头; 读取中为 null. */
    private sealed interface Lenses {
        data object Loading : Lenses
        data object Failed : Lenses
        data class Loaded(val cameras: List<Bundle>) : Lenses
    }

    private val snackbar = SnackbarHostState()
    private var revision by mutableIntStateOf(0)
    private var pendingMessage by mutableStateOf<String?>(null)
    private var dialog by mutableStateOf<Dialog?>(null)
    private var cameraGranted by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(true)
    private var holder by mutableStateOf<CameraExclusivity.Holder?>(null)
    private var lenses by mutableStateOf<Lenses>(Lenses.Loading)
    private lateinit var updates: AppUpdateController

    /** A denial without rationale means "don't ask again": the next tap opens the app settings. zh-CN: 无理由的拒绝即 "不再询问": 下次点按打开应用设置. */
    private var cameraBlocked = false
    private var notificationsBlocked = false

    private val cameraRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        cameraBlocked = !granted && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        if (granted) loadLenses()
    }

    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsGranted = granted
        notificationsBlocked = !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val holderTicker = object : Runnable {
        override fun run() {
            holder = CameraExclusivity.shared.holder()
            MainThread.handler.postDelayed(this, HOLDER_TICK_MILLIS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updates = AppUpdateController(AppUpdateStore.of(this), BuildConfig.VERSION_CODE.toLong())
        loadLenses()
        // Appendix D Q46: the plugin has no home screen, so the automatic check runs when this page opens (not on a
        // recreation). zh-CN: 附录 D Q46: 插件没有首页, 自动检查在打开本页时运行 (重建时不运行).
        if (savedInstanceState == null) updates.checkAutomaticallyIfDue()
    }

    override fun onResume() {
        super.onResume()
        // Back from the app or notification settings: read the permissions again. zh-CN: 从应用或通知设置返回: 重新读取权限.
        cameraGranted = CameraEngine.hasPermission(this)
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        MainThread.handler.removeCallbacks(holderTicker)
        holderTicker.run()
    }

    override fun onPause() {
        MainThread.handler.removeCallbacks(holderTicker)
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) updates.cancel()
        super.onDestroy()
    }

    override fun holdsAppearance(): Boolean = dialog != null || updates.state != null

    /**
     * The provider is awaited on the camera I/O thread (never on the main thread), the cameras are described on the main
     * thread like `listCameras`. zh-CN: 在相机 I/O 线程等待提供者 (从不在主线程等待), 与 `listCameras` 一样在主线程描述镜头.
     */
    private fun loadLenses() {
        lenses = Lenses.Loading
        val application = applicationContext
        CameraExecutors.io.execute {
            val provider = runCatching { CameraEngine.provider(application) }
                .onFailure { Log.w(TAG, "The camera provider is unavailable", it) }
                .getOrNull()
            MainThread.post {
                if (isDestroyed) return@post
                lenses = provider?.let { runCatching { Lenses.Loaded(CameraEngine.cameras(it).map(CameraEngine::describe)) }.getOrNull() } ?: Lenses.Failed
            }
        }
    }

    @Composable
    override fun Content() {
        val message = pendingMessage
        LaunchedEffect(message) {
            if (message != null) {
                snackbar.showSnackbar(message)
                pendingMessage = null
            }
        }
        // Saved choices bump the revision, so the summaries below read the stored values again.
        val appearancePreferences = remember(revision) { AppearancePreferences.read(this) }
        val defaultFacing = remember(revision) { CameraPreferences.defaultFacing(this) }
        val analysisSize = remember(revision) { CameraPreferences.analysisSize(this) }
        val analysisFps = remember(revision) { CameraPreferences.fps(this) }
        PrismScreen(title = stringResource(R.string.settings_title), onBack = ::finish, snackbarHostState = snackbar) { padding ->
            // A saved choice bumps the revision and rebuilds the rows from the stored values. zh-CN: 保存后修订号增加, 各行按存储值重建.
            key(revision) {
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("prism.settings")) {
                    SectionHeader(stringResource(R.string.settings_appearance))
                    SettingsRow(stringResource(R.string.settings_language), summary = languageSummary(appearancePreferences.language), icon = R.drawable.ic_language_24,
                        onClick = { dialog = Dialog.LANGUAGE }, modifier = Modifier.testTag("prism.settings.language"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_night_mode), summary = darkModeSummary(appearancePreferences.darkMode), icon = R.drawable.ic_dark_mode_24,
                        onClick = { dialog = Dialog.DARK_MODE }, modifier = Modifier.testTag("prism.settings.night"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_theme_color), summary = colorSummary(appearancePreferences.color), icon = R.drawable.ic_palette_24,
                        onClick = { dialog = Dialog.THEME_COLOR }, modifier = Modifier.testTag("prism.settings.color"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_launcher_icon), summary = stringResource(iconLabel(LauncherIcons.current(this@SettingsActivity))), icon = R.drawable.ic_apps_24,
                        onClick = { dialog = Dialog.LAUNCHER_ICON }, modifier = Modifier.testTag("prism.settings.icon"))

                    SectionHeader(stringResource(R.string.settings_camera))
                    SettingsRow(stringResource(R.string.settings_camera_permission),
                        summary = stringResource(if (cameraGranted) R.string.settings_permission_granted else R.string.settings_permission_missing),
                        summaryColor = if (cameraGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        icon = R.drawable.ic_photo_camera_24, showChevron = !cameraGranted,
                        onClick = if (cameraGranted) null else ::askCamera, modifier = Modifier.testTag("prism.settings.cameraPermission"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Hairline()
                        SettingsRow(stringResource(R.string.settings_notification_permission),
                            summary = stringResource(if (notificationsGranted) R.string.settings_permission_granted else R.string.settings_notification_missing),
                            summaryColor = if (notificationsGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            icon = R.drawable.ic_notifications_24, showChevron = !notificationsGranted,
                            onClick = if (notificationsGranted) null else ::askNotifications, modifier = Modifier.testTag("prism.settings.notificationPermission"))
                    }
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_default_lens), summary = stringResource(lensLabel(defaultFacing)), icon = R.drawable.ic_cameraswitch_24,
                        onClick = { dialog = Dialog.DEFAULT_LENS }, modifier = Modifier.testTag("prism.settings.defaultLens"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_analysis_size), summary = SessionDefaults.formatSize(analysisSize), icon = R.drawable.ic_tune_24,
                        onClick = { dialog = Dialog.ANALYSIS_SIZE }, modifier = Modifier.testTag("prism.settings.analysisSize"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_analysis_fps), summary = fpsLabel(analysisFps), icon = R.drawable.ic_tune_24,
                        onClick = { dialog = Dialog.ANALYSIS_FPS }, modifier = Modifier.testTag("prism.settings.analysisFps"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_foreground_service), summary = stringResource(R.string.settings_foreground_service_summary), icon = R.drawable.ic_notifications_24,
                        onClick = ::openNotificationSettings, modifier = Modifier.testTag("prism.settings.foregroundService"))

                    SectionHeader(stringResource(R.string.settings_diagnostics))
                    SettingsRow(stringResource(R.string.settings_camerax_version), summary = CameraEngine.VERSION, icon = R.drawable.ic_info_24,
                        showChevron = false, modifier = Modifier.testTag("prism.settings.cameraxVersion"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_lenses), summary = lensesSummary(), icon = R.drawable.ic_photo_camera_24,
                        enabled = lenses is Lenses.Loaded, onClick = { dialog = Dialog.LENSES }, modifier = Modifier.testTag("prism.settings.lenses"))
                    Hairline()
                    SettingsRow(stringResource(R.string.settings_holder), summary = holderSummary(), icon = R.drawable.ic_person_24,
                        showChevron = false, modifier = Modifier.testTag("prism.settings.holder"))

                    SectionHeader(stringResource(R.string.settings_about))
                    SettingsRow(stringResource(R.string.settings_version),
                        summary = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, stringResource(R.string.plugin_version_date)),
                        icon = R.drawable.ic_info_24, showChevron = false, modifier = Modifier.testTag("prism.settings.version"))
                    Hairline()
                    SettingsRow(stringResource(R.string.release_history_title), summary = stringResource(R.string.settings_release_history_summary), icon = R.drawable.ic_history_24,
                        onClick = { startActivity(Intent(this@SettingsActivity, ReleaseHistoryActivity::class.java)) }, modifier = Modifier.testTag("prism.settings.releaseHistory"))
                    Hairline()
                    SettingsRow(stringResource(R.string.update_check), summary = stringResource(R.string.settings_update_summary), icon = R.drawable.ic_update_24,
                        onClick = updates::checkManually, modifier = Modifier.testTag("prism.settings.update"))
                    Hairline()
                    val store = remember { AppUpdateStore.of(this@SettingsActivity) }
                    var automatic by remember { mutableStateOf(store.automaticChecks) }
                    SwitchRow(stringResource(R.string.update_automatic), automatic, { automatic = it; store.automaticChecks = it },
                        summary = stringResource(R.string.update_automatic_summary), icon = R.drawable.ic_notifications_24, modifier = Modifier.testTag("prism.settings.autoUpdate"))
                    Hairline()
                    val ignored = store.ignoredTags.size
                    SettingsRow(stringResource(R.string.update_ignored), summary = ignoredSummary(ignored), icon = R.drawable.ic_close_24,
                        enabled = ignored > 0, onClick = { dialog = Dialog.IGNORED }, modifier = Modifier.testTag("prism.settings.ignored"))
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        Dialogs()
        UpdateDialogs(updates, onOpenRelease = { url -> open(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            onReleaseHistory = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) })
    }

    @Composable
    private fun ignoredSummary(count: Int): String =
        if (count == 0) stringResource(R.string.update_ignored_none) else pluralStringResource(R.plurals.update_ignored_count, count, count)

    @Composable
    private fun Dialogs() {
        val close = { dialog = null }
        when (dialog) {
            null -> Unit
            Dialog.LANGUAGE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_language),
                    options = AppearancePreferences.LANGUAGES.map { Choice(languageLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.LANGUAGES.indexOf(current.language).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(language = AppearancePreferences.LANGUAGES[index])) },
                    tag = "prism.dialog.language",
                )
            }
            Dialog.DARK_MODE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_night_mode),
                    options = AppearancePreferences.DARK_MODES.map { Choice(darkModeLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.DARK_MODES.indexOf(current.darkMode).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(darkMode = AppearancePreferences.DARK_MODES[index])) },
                    tag = "prism.dialog.night",
                )
            }
            Dialog.THEME_COLOR -> {
                val current = AppearancePreferences.read(this)
                ThemeColorDialog(
                    currentColor = current.color,
                    hostColor = HostAppearanceReader.cached()?.primary ?: AppearancePreferences.DEFAULT_COLOR,
                    onDismissRequest = close,
                    onConfirm = { color -> close(); saveAppearance(current.copy(color = color)) },
                )
            }
            Dialog.LAUNCHER_ICON -> ChoiceDialog(
                title = stringResource(R.string.settings_launcher_icon),
                options = LauncherIconMode.entries.map { mode ->
                    Choice(stringResource(iconLabel(mode)), when (mode) {
                        LauncherIconMode.AUTO -> stringResource(R.string.launcher_icon_auto_note)
                        LauncherIconMode.TRANSPARENT -> stringResource(R.string.launcher_icon_transparent_note)
                        else -> null
                    })
                },
                selectedIndex = LauncherIcons.current(this).ordinal,
                onDismissRequest = close,
                onConfirm = { index ->
                    close()
                    val result = runCatching { LauncherIcons.select(this, LauncherIconMode.entries[index]) }
                    revision++
                    pendingMessage = getString(if (result.isSuccess) R.string.launcher_icon_applied_note else R.string.launcher_icon_failed)
                },
                tag = "prism.dialog.icon",
            )
            Dialog.DEFAULT_LENS -> {
                val current = CameraPreferences.defaultFacing(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_default_lens),
                    message = stringResource(R.string.settings_default_lens_message),
                    options = CameraPreferences.FACINGS.map { Choice(stringResource(lensLabel(it))) },
                    selectedIndex = CameraPreferences.FACINGS.indexOf(current).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index ->
                        close()
                        val next = CameraPreferences.FACINGS[index]
                        if (next != current) {
                            if (CameraPreferences.saveDefaultFacing(this, next)) revision++ else pendingMessage = getString(R.string.settings_save_failed)
                        }
                    },
                    tag = "prism.dialog.defaultLens",
                )
            }
            Dialog.ANALYSIS_SIZE -> {
                val current = CameraPreferences.analysisSize(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_analysis_size),
                    message = stringResource(R.string.settings_analysis_size_message),
                    options = SessionDefaults.SIZES.map { Choice(SessionDefaults.formatSize(it)) },
                    selectedIndex = SessionDefaults.SIZES.indexOf(current).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index ->
                        close()
                        val next = SessionDefaults.SIZES[index]
                        if (next != current) {
                            if (CameraPreferences.saveAnalysisSize(this, next)) revision++ else pendingMessage = getString(R.string.settings_save_failed)
                        }
                    },
                    tag = "prism.dialog.analysisSize",
                )
            }
            Dialog.ANALYSIS_FPS -> {
                val current = CameraPreferences.fps(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_analysis_fps),
                    message = stringResource(R.string.settings_analysis_fps_message),
                    options = SessionDefaults.FPS_CHOICES.map { Choice(fpsLabel(it)) },
                    selectedIndex = SessionDefaults.FPS_CHOICES.indexOf(current).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index ->
                        close()
                        val next = SessionDefaults.FPS_CHOICES[index]
                        if (next != current) {
                            if (CameraPreferences.saveFps(this, next)) revision++ else pendingMessage = getString(R.string.settings_save_failed)
                        }
                    },
                    tag = "prism.dialog.analysisFps",
                )
            }
            Dialog.LENSES -> MessageDialog(
                title = stringResource(R.string.settings_lenses),
                message = lensesDetails(),
                onDismissRequest = close,
                onConfirm = close,
                confirmText = stringResource(R.string.ui_dialog_close),
                dismissText = null,
                tag = "prism.dialog.lenses",
            )
            Dialog.IGNORED -> {
                val store = AppUpdateStore.of(this)
                val tags = store.ignoredTags.sorted()
                MultiChoiceDialog(
                    title = stringResource(R.string.update_ignored),
                    message = stringResource(R.string.update_ignored_message),
                    options = tags.map { Choice(it) },
                    selected = emptySet(),
                    onDismissRequest = close,
                    onConfirm = { chosen -> close(); store.stopIgnoring(chosen.map(tags::get)); revision++ },
                    confirmText = stringResource(R.string.update_stop_ignoring),
                    requireOne = true,
                    tag = "prism.dialog.ignored",
                )
            }
        }
    }

    // Actions -------------------------------------------------------------------------------------------------------

    private fun saveAppearance(next: AppearancePreferences) {
        if (next == AppearancePreferences.read(this)) return
        if (next.save(this)) {
            revision++
            applyAppearanceChange()
        } else {
            pendingMessage = getString(R.string.settings_save_failed)
        }
    }

    private fun askCamera() {
        if (cameraBlocked) openAppSettings() else cameraRequest.launch(Manifest.permission.CAMERA)
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (notificationsBlocked) openAppSettings() else notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openAppSettings() {
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    /** The settings of the camera-in-use channel (API 26+), else the app details. zh-CN: 相机使用中通知渠道的设置 (API 26+), 否则为应用详情. */
    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, ThreePrismCameraApplication.CHANNEL_CAMERA))
        } else {
            openAppSettings()
        }
    }

    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure {
            Log.w(TAG, "Cannot open ${intent.action}", it)
            pendingMessage = getString(R.string.settings_open_failed)
        }
    }

    // Summaries and labels --------------------------------------------------------------------------------------

    @Composable
    private fun lensesSummary(): String = when (val state = lenses) {
        Lenses.Loading -> stringResource(R.string.settings_loading)
        Lenses.Failed -> stringResource(R.string.capture_camera_unavailable)
        is Lenses.Loaded -> if (state.cameras.isEmpty()) {
            stringResource(R.string.capture_no_camera)
        } else {
            stringResource(R.string.settings_lenses_summary, state.cameras.size, state.cameras.map { stringResource(lensLabel(it.getString(CameraResultKeys.FACING))) }.joinToString(", "))
        }
    }

    /** One paragraph per lens, one capability per line. zh-CN: 每个镜头一段, 每项能力一行. */
    @Composable
    private fun lensesDetails(): String {
        val cameras = (lenses as? Lenses.Loaded)?.cameras.orEmpty()
        val newline = System.lineSeparator()
        return cameras.map { camera ->
            val analysis = camera.getStringArray(CameraResultKeys.ANALYSIS_RESOLUTIONS).orEmpty()
            val capture = camera.getStringArray(CameraResultKeys.CAPTURE_RESOLUTIONS).orEmpty()
            val exposureMin = camera.getInt(CameraResultKeys.EXPOSURE_MIN)
            val exposureMax = camera.getInt(CameraResultKeys.EXPOSURE_MAX)
            listOf(
                stringResource(R.string.settings_lens_heading, stringResource(lensLabel(camera.getString(CameraResultKeys.FACING))), camera.getString(CameraResultKeys.ID).orEmpty()),
                stringResource(R.string.settings_lens_zoom, ratio(camera.getFloat(CameraResultKeys.ZOOM_MIN, 1f)), ratio(camera.getFloat(CameraResultKeys.ZOOM_MAX, 1f))),
                stringResource(R.string.settings_lens_flash, stringResource(if (camera.getBoolean(CameraResultKeys.HAS_FLASH)) R.string.settings_yes else R.string.settings_no)),
                if (exposureMin == 0 && exposureMax == 0) stringResource(R.string.settings_lens_exposure_none) else stringResource(R.string.settings_lens_exposure, exposureMin, exposureMax),
                stringResource(R.string.settings_lens_analysis, analysis.size, analysis.firstOrNull() ?: "-"),
                stringResource(R.string.settings_lens_capture, capture.firstOrNull() ?: "-"),
            ).joinToString(newline)
        }.joinToString(newline + newline)
    }

    @Composable
    private fun fpsLabel(fps: Int?): String =
        if (fps == null) stringResource(R.string.settings_analysis_fps_device) else stringResource(R.string.settings_analysis_fps_value, fps)

    @Composable
    private fun holderSummary(): String {
        val current = holder ?: return stringResource(R.string.settings_holder_none)
        return if (current.viewfinder) stringResource(R.string.settings_holder_viewfinder) else stringResource(R.string.settings_holder_script, current.label)
    }

    @Composable
    private fun followNote(isFollow: Boolean): String? =
        if (isFollow && HostAppearanceReader.cached() == null) stringResource(R.string.settings_follow_autojs6_unavailable) else null

    @Composable
    private fun languageSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6_summary, languageLabel(appearance.language))
        else -> languageLabel(value)
    }

    @Composable
    private fun darkModeSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(
            R.string.settings_follow_autojs6_summary,
            stringResource(if (appearance.dark) R.string.settings_dark_mode_dark else R.string.settings_dark_mode_light),
        )
        else -> darkModeLabel(value)
    }

    @Composable
    private fun colorSummary(color: Int?): String =
        if (color == null) stringResource(R.string.settings_follow_autojs6_summary, ThemeColorValue.hex(appearance.primary)) else ThemeColorValue.hex(color)

    @Composable
    private fun languageLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6)
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        "zh-Hans" -> stringResource(R.string.settings_language_zh_hans)
        "zh-Hant-HK" -> stringResource(R.string.settings_language_zh_hant_hk)
        "zh-Hant-TW" -> stringResource(R.string.settings_language_zh_hant_tw)
        "en" -> stringResource(R.string.settings_language_en)
        "fr" -> stringResource(R.string.settings_language_fr)
        "es" -> stringResource(R.string.settings_language_es)
        "ja" -> stringResource(R.string.settings_language_ja)
        "ko" -> stringResource(R.string.settings_language_ko)
        "ru" -> stringResource(R.string.settings_language_ru)
        "ar" -> stringResource(R.string.settings_language_ar)
        else -> Locale.forLanguageTag(value).getDisplayName(Locale.forLanguageTag(value)).ifBlank { value }
    }

    @Composable
    private fun darkModeLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        AppearancePreferences.LIGHT -> stringResource(R.string.settings_dark_mode_light)
        AppearancePreferences.DARK -> stringResource(R.string.settings_dark_mode_dark)
        else -> stringResource(R.string.settings_follow_autojs6)
    }

    @StringRes
    private fun lensLabel(facing: String?): Int = when (facing) {
        CameraContract.FACING_FRONT -> R.string.capture_lens_front
        CameraContract.FACING_EXTERNAL -> R.string.capture_lens_external
        else -> R.string.capture_lens_back
    }

    @StringRes
    private fun iconLabel(mode: LauncherIconMode): Int = when (mode) {
        LauncherIconMode.LIGHT -> R.string.launcher_icon_light
        LauncherIconMode.DARK -> R.string.launcher_icon_dark
        LauncherIconMode.AUTO -> R.string.launcher_icon_auto
        LauncherIconMode.TRANSPARENT -> R.string.launcher_icon_transparent
    }

    private fun ratio(value: Float): String = String.format(Locale.ROOT, "%.1fx", value)

    private companion object {
        const val TAG = "PrismSettings"
        const val HOLDER_TICK_MILLIS = 1_000L
    }

}
