package io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.supermonster003.autojs6.plugin.three.prism.camera.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.prism.camera.R
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.CheckRow
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.PrismDialog
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.components.MessageDialog
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** The latest GitHub release of the plugin. zh-CN: 插件在 GitHub 上的最新发行版. */
internal data class AppRelease(val tag: String, val name: String, val notes: String, val pageUrl: String)

/** Semantic version comparison of 3-Stone AI (as in 3-Macaw Speech P5.4). Pure Kotlin. zh-CN: 沿用 3-Stone AI 的语义化版本比较 (与 3-Macaw Speech P5.4 相同). 纯 Kotlin. */
internal object UpdateVersionPolicy {
    fun isNewer(candidateTag: String, installedVersion: String): Boolean {
        val candidate = parse(candidateTag) ?: return false
        val installed = parse(installedVersion) ?: return false
        val width = maxOf(candidate.numbers.size, installed.numbers.size)
        repeat(width) { index ->
            val comparison = candidate.numbers.getOrElse(index) { 0 }.compareTo(installed.numbers.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return when {
            candidate.preRelease == installed.preRelease -> false
            candidate.preRelease == null -> true
            installed.preRelease == null -> false
            else -> candidate.preRelease > installed.preRelease
        }
    }

    private fun parse(value: String): ParsedVersion? {
        val match = VERSION_PATTERN.matchEntire(value.trim()) ?: return null
        return ParsedVersion(
            numbers = match.groupValues[1].split('.').map { part -> part.toIntOrNull() ?: return null },
            preRelease = match.groupValues[2].takeIf(String::isNotEmpty),
        )
    }

    private data class ParsedVersion(val numbers: List<Int>, val preRelease: String?)

    private val VERSION_PATTERN = Regex("^[vV]?([0-9]+(?:\\.[0-9]+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$")
}

/**
 * Update preferences (3-Stone AI rules): automatic checks on by default and at most once every 12 hours, ignored tags.
 * [clock] is injectable for the JVM tests.
 * zh-CN: 更新偏好 (沿用 3-Stone AI): 自动检查默认开启且每 12 小时至多一次, 已忽略的版本. [clock] 可注入, 供 JVM 测试使用.
 */
internal class AppUpdateStore(private val storage: Storage, private val clock: () -> Long = System::currentTimeMillis) {

    /** The persistence behind the store (SharedPreferences in the app). zh-CN: 存储后端 (应用中为 SharedPreferences). */
    interface Storage {
        fun getBoolean(key: String, default: Boolean): Boolean
        fun putBoolean(key: String, value: Boolean)
        fun getLong(key: String, default: Long): Long
        fun putLong(key: String, value: Long)
        fun getStrings(key: String): Set<String>
        fun putStrings(key: String, value: Set<String>)
    }

    var automaticChecks: Boolean
        get() = storage.getBoolean(KEY_AUTOMATIC, true)
        set(value) = storage.putBoolean(KEY_AUTOMATIC, value)

    val ignoredTags: Set<String> get() = storage.getStrings(KEY_IGNORED)

    fun ignore(tag: String) = storage.putStrings(KEY_IGNORED, ignoredTags + tag)

    fun stopIgnoring(tags: Collection<String>) = storage.putStrings(KEY_IGNORED, ignoredTags - tags.toSet())

    fun shouldCheckAutomatically(): Boolean {
        if (!automaticChecks) return false
        val last = storage.getLong(KEY_LAST_ATTEMPT, 0L)
        val now = clock()
        // A clock set back counts as due as well. zh-CN: 时钟被调回时同样视为到期.
        return last <= 0L || now < last || now - last >= INTERVAL_MILLIS
    }

    fun recordAutomaticAttempt() = storage.putLong(KEY_LAST_ATTEMPT, clock())

    companion object {
        val INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(12)
        /** The preferences file of the update check. zh-CN: 更新检查的偏好文件. */
        internal const val FILE = "app-update-settings"
        internal const val KEY_AUTOMATIC = "automatic-checks"
        private const val KEY_IGNORED = "ignored-tags"
        private const val KEY_LAST_ATTEMPT = "last-automatic-attempt"

        fun of(context: Context): AppUpdateStore {
            val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            return AppUpdateStore(object : Storage {
                override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
                override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
                override fun getLong(key: String, default: Long) = preferences.getLong(key, default)
                override fun putLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()
                override fun getStrings(key: String): Set<String> = preferences.getStringSet(key, emptySet()).orEmpty().toSet()
                override fun putStrings(key: String, value: Set<String>) = preferences.edit().putStringSet(key, value).apply()
            })
        }
    }
}

/** What a check found. zh-CN: 检查结果. */
internal sealed interface UpdateResult {
    data class Release(val release: AppRelease) : UpdateResult

    /** The repository has no published release yet (HTTP 404). zh-CN: 仓库尚无已发布的版本 (HTTP 404). */
    data object NoRelease : UpdateResult
}

/**
 * Reads the latest release from the GitHub API (3-Stone AI limits: 15 s connect, 20 s read, 30 s in total, no
 * redirects, at most 256 KiB and 6000 characters of notes). [cancel] aborts the connection.
 * zh-CN: 从 GitHub API 读取最新发行版 (沿用 3-Stone AI 的限制: 连接 15 秒, 读取 20 秒, 总计 30 秒, 不跟随重定向, 至多 256 KiB, 说明至多 6000 字符).
 * [cancel] 中止连接.
 */
internal class AppUpdateChecker(private val apiUrl: String = LATEST_RELEASE_API) {

    inner class Call internal constructor() {
        private val cancelled = AtomicBoolean(false)
        @Volatile var connection: HttpURLConnection? = null

        val isCancelled: Boolean get() = cancelled.get()

        /** The total time limit closed the connection (a failure, not a user cancel). zh-CN: 总时限关闭了连接 (属于失败, 不是用户取消). */
        @Volatile var timedOut = false

        fun cancel() {
            cancelled.set(true)
            runCatching { connection?.disconnect() }
        }
    }

    fun check(callback: (Call, Result<UpdateResult>) -> Unit): Call {
        val call = Call()
        worker.execute {
            // The total limit: the connection is closed when the check takes longer. zh-CN: 总时限: 超时即关闭连接.
            val timeout = Runnable {
                call.timedOut = true
                call.cancel()
            }
            main.postDelayed(timeout, TOTAL_TIMEOUT_MILLIS)
            val result = runCatching { fetch(call) }
            main.removeCallbacks(timeout)
            callback(call, result)
        }
        return call
    }

    private fun fetch(call: Call): UpdateResult {
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Three-Prism-Camera-update-checker/${BuildConfig.VERSION_NAME}")
        }
        call.connection = connection
        try {
            if (call.isCancelled) throw IOException("cancelled")
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return UpdateResult.NoRelease
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Release service returned HTTP $code")
            val declared = connection.contentLengthLong
            if (declared > MAXIMUM_RESPONSE_BYTES) throw IOException("Release response is too large")
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    if (output.size() > MAXIMUM_RESPONSE_BYTES) throw IOException("Release response is too large")
                }
            }
            return UpdateResult.Release(decode(output.toString(Charsets.UTF_8.name())))
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val LATEST_RELEASE_API = "https://api.github.com/repos/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases/latest"
        const val RELEASES_PAGE = "https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 20_000
        const val TOTAL_TIMEOUT_MILLIS = 30_000L
        const val MAXIMUM_RESPONSE_BYTES = 256 * 1024
        const val MAXIMUM_NOTES_CHARACTERS = 6_000
        private val TAG_PATTERN = Regex("^[0-9A-Za-z._-]{1,100}$")
        private val worker = Executors.newSingleThreadExecutor { Thread(it, "PrismUpdateCheck").apply { isDaemon = true } }
        private val main = Handler(Looper.getMainLooper())

        /** Parses the release JSON of the GitHub API. Pure apart from `org.json`. zh-CN: 解析 GitHub API 的发行版 JSON. */
        fun decode(text: String): AppRelease {
            val json = JSONObject(text)
            val tag = json.getString("tag_name").trim()
            require(TAG_PATTERN.matches(tag)) { "Release tag is invalid" }
            return AppRelease(
                tag = tag,
                name = json.optString("name").trim().ifEmpty { tag },
                notes = json.optString("body").trim().take(MAXIMUM_NOTES_CHARACTERS),
                pageUrl = "$RELEASES_PAGE/tag/$tag",
            )
        }
    }
}

/**
 * The update check of the settings page (appendix D Q42 = (b), ported from 3-Macaw Speech P5.4, 3-Stone AI rules): a
 * manual check shows a cancellable progress dialog and every result; an automatic check (when the settings page opens,
 * appendix D Q46, at most every 12 hours) stays silent unless a newer release that is not ignored exists. Compose state,
 * main thread only.
 * zh-CN: 设置页的更新检查 (附录 D Q42 = (b), 移植自 3-Macaw Speech P5.4, 沿用 3-Stone AI 规则): 手动检查显示可取消的进度对话框与每种结果; 自动检查
 * (打开设置页时, 附录 D Q46, 每 12 小时至多一次) 只在存在未被忽略的新版本时提示. Compose 状态, 仅在主线程使用.
 */
internal class AppUpdateController(
    private val store: AppUpdateStore,
    private val installedVersion: String,
    private val checker: AppUpdateChecker = AppUpdateChecker(),
) {
    sealed interface State {
        data object Checking : State
        data class UpToDate(val noRelease: Boolean) : State
        data class Available(val release: AppRelease) : State
        data object Failed : State
    }

    var state by mutableStateOf<State?>(null)
        private set

    private var active: AppUpdateChecker.Call? = null

    fun checkManually() = check(manual = true)

    fun checkAutomaticallyIfDue() {
        if (active != null || !store.shouldCheckAutomatically()) return
        store.recordAutomaticAttempt()
        check(manual = false)
    }

    fun dismiss() {
        if (state == State.Checking) active?.cancel()
        state = null
    }

    fun ignore(tag: String) = store.ignore(tag)

    fun cancel() {
        active?.cancel()
        active = null
        if (state == State.Checking) state = null
    }

    private fun check(manual: Boolean) {
        if (active != null) return
        if (manual) state = State.Checking
        active = checker.check { call, result ->
            mainHandler.post {
                if (active === call) active = null
                if (call.isCancelled && !call.timedOut) {
                    if (state == State.Checking) state = null
                    return@post
                }
                result.fold(
                    onSuccess = { found ->
                        when (found) {
                            UpdateResult.NoRelease -> if (manual) state = State.UpToDate(noRelease = true)
                            is UpdateResult.Release -> {
                                val release = found.release
                                when {
                                    !UpdateVersionPolicy.isNewer(release.tag, installedVersion) -> if (manual) state = State.UpToDate(noRelease = false)
                                    !manual && release.tag in store.ignoredTags -> Unit
                                    else -> state = State.Available(release)
                                }
                            }
                        }
                    },
                    onFailure = { if (manual) state = State.Failed },
                )
            }
        }
    }

    private companion object {
        val mainHandler = Handler(Looper.getMainLooper())
    }
}

/**
 * The dialogs of [AppUpdateController]: progress (cancel), up to date, failure, and the release dialog with "view
 * release", "later", the neutral "release history" button and the "ignore this version" check box.
 * zh-CN: [AppUpdateController] 的对话框: 进度 (可取消), 已是最新, 失败, 以及带 "查看发行版", "稍后", Neutral "发行历史" 按钮与 "忽略此版本" 复选框的发行版对话框.
 */
@Composable
internal fun UpdateDialogs(controller: AppUpdateController, onOpenRelease: (String) -> Unit, onReleaseHistory: () -> Unit) {
    when (val state = controller.state) {
        null -> Unit
        AppUpdateController.State.Checking -> MessageDialog(
            title = stringResource(R.string.update_checking),
            message = stringResource(R.string.update_checking_summary),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_cancel),
            dismissText = null,
            tag = "prism.update.checking",
        )
        is AppUpdateController.State.UpToDate -> MessageDialog(
            title = stringResource(R.string.update_up_to_date),
            message = if (state.noRelease) stringResource(R.string.update_no_release, BuildConfig.VERSION_NAME) else stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_confirm),
            dismissText = null,
            tag = "prism.update.current",
        )
        AppUpdateController.State.Failed -> MessageDialog(
            title = stringResource(R.string.update_check),
            message = stringResource(R.string.update_check_failed),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_confirm),
            dismissText = null,
            tag = "prism.update.failed",
        )
        is AppUpdateController.State.Available -> {
            val release = state.release
            var ignore by rememberSaveable(release.tag) { mutableStateOf(false) }
            fun close(open: Boolean) {
                if (ignore) controller.ignore(release.tag)
                controller.dismiss()
                if (open) onOpenRelease(release.pageUrl)
            }
            PrismDialog(
                title = release.name,
                onDismissRequest = { close(open = false) },
                confirmText = stringResource(R.string.update_view_release),
                onConfirm = { close(open = true) },
                dismissText = stringResource(R.string.update_later),
                neutralText = stringResource(R.string.release_history_title),
                onNeutral = {
                    if (ignore) controller.ignore(release.tag)
                    controller.dismiss()
                    onReleaseHistory()
                },
                tag = "prism.update.available",
            ) {
                Text(
                    stringResource(R.string.update_available_summary, release.tag) + if (release.notes.isNotBlank()) "\n\n" + release.notes else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding),
                )
                CheckRow(Choice(stringResource(R.string.update_ignore_version)), ignore, { ignore = it }, Modifier.testTag("prism.update.ignore"))
            }
        }
    }
}
