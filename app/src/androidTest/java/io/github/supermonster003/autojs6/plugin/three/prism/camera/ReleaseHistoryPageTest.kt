package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.app.Activity
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.release.ReleaseHistoryAssets
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.release.loadReleaseHistory
import io.github.supermonster003.autojs6.plugin.three.prism.camera.ui.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The release history page (plugin-center P21.1, release history specification section 10): the title "Release
 * history" with the plugin name as the subtitle, the newest release expanded and the others collapsed with their
 * category counts, a header toggles its release, the top bar expands or collapses all, the choice survives recreation,
 * the settings row opens the page, and Tab walks every release down to the oldest one.
 * zh-CN: 发行历史页 (plugin-center P21.1, 发行历史规范第 10 节): 标题 "发行历史", 子标题为插件名称; 最新版本展开, 其余收起并显示各类别条目数; 点版本头
 * 切换该版本, 顶栏按钮展开或收起全部, 重建后保留; 设置页那一行打开本页; Tab 依次走过各版本直到最早的版本.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseHistoryPageTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @After
    fun finishAll() {
        instrumentation.setInTouchMode(true)
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED).flatMap { monitor.getActivitiesInStage(it) }.forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
    }

    /** Unmerged: the count chips sit inside the clickable header, which merges its children. zh-CN: 未合并树: 计数胶囊在合并子节点的可点按版本头里. */
    private fun exists(tag: String): Boolean = compose.shows(hasTestTag(tag), useUnmergedTree = true)

    private fun versions(): List<String> {
        val locale = context.resources.configuration.locales[0]
        return loadReleaseHistory(ReleaseHistoryAssets.candidatesFor(locale)) { path ->
            context.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
        }.map { it.version }
    }

    private fun assertTitleAndSubtitle() {
        compose.onNode(hasText(context.getString(R.string.release_history))).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.app_name))).assertIsDisplayed()
    }

    @Test
    fun theTitleIsReleaseHistoryAndTheSubtitleThePluginName() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("prism.releaseHistory.list") }
            assertTitleAndSubtitle()
            assertFalse(exists("prism.emptyState"))
        }
    }

    /**
     * The newest release starts expanded, the others show their category counts; a header expands or collapses its
     * release, the top bar expands or collapses all of them, and the choice survives recreation.
     * zh-CN: 最新版本默认展开, 其余显示各类别计数; 点版本头展开或收起该版本, 顶栏按钮展开或收起全部, 选择在重建后保留.
     */
    @Test
    fun releasesExpandAndCollapse() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("prism.releaseHistory.list") }
            val versions = versions()
            assertTrue("releases: ${versions.size}", versions.size >= 2)
            val (newest, second) = versions
            assertTrue(exists("prism.releaseHistory.body.$newest"))
            assertFalse(exists("prism.releaseHistory.counts.$newest"))
            assertFalse(exists("prism.releaseHistory.body.$second"))
            assertTrue(exists("prism.releaseHistory.counts.$second"))

            compose.onNodeWithTag("prism.releaseHistory.entry.$second").performClick()
            compose.waitUntil(TIMEOUT) { exists("prism.releaseHistory.body.$second") }
            compose.onNodeWithTag("prism.releaseHistory.entry.$newest").performClick()
            compose.waitUntil(TIMEOUT) { !exists("prism.releaseHistory.body.$newest") && exists("prism.releaseHistory.counts.$newest") }

            scenario.recreate()
            compose.waitUntil(TIMEOUT) { exists("prism.releaseHistory.list") }
            assertTrue(exists("prism.releaseHistory.body.$second"))
            assertFalse(exists("prism.releaseHistory.body.$newest"))

            compose.onNodeWithTag("prism.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_expand_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { exists("prism.releaseHistory.body.$newest") }
            compose.onNodeWithTag("prism.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_collapse_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { !exists("prism.releaseHistory.body.$newest") && !exists("prism.releaseHistory.body.$second") }
        }
    }

    /** The "Release history" row of the settings page opens the page. zh-CN: 设置页的 "发行历史" 一行打开本页. */
    @Test
    fun theSettingsRowOpensThePage() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("prism.settings.releaseHistory") }
            compose.onNodeWithTag("prism.settings.releaseHistory").performScrollTo()
                .assert(hasText(context.getString(R.string.release_history)))
                .performClick()
            compose.waitUntil(TIMEOUT) { resumed() is ReleaseHistoryActivity && exists("prism.releaseHistory.list") }
            assertTitleAndSubtitle()
        }
    }

    /**
     * Every release takes focus in turn, scrolled into view, down to the oldest one; the back button and the top bar
     * button are reachable too.
     * zh-CN: 各版本依次获得焦点并滚入视野, 直到最早的版本; 返回按钮与顶栏按钮同样可达.
     */
    @Test
    fun theReleaseHistoryCanBeReadToTheEndFromTheKeyboard() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("prism.releaseHistory.list") }
            // Newer versions take setInTouchMode at once; API 24 learns it only from a key. zh-CN: 新版本立即生效; API 24 要靠按键得知.
            instrumentation.setInTouchMode(false)
            key(KeyEvent.KEYCODE_DPAD_UP)
            val versions = versions()
            val visited = tabCycle()
            assertTrue(visited.toString(), visited.containsAll(listOf("prism.topBar.back", "prism.releaseHistory.toggleAll")))
            assertEquals(versions.map { "prism.releaseHistory.entry.$it" }, visited.filter { it.startsWith("prism.releaseHistory.entry.") })
        }
    }

    private fun resumed(): Activity? {
        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    /** The test tag of the focused control or of its nearest tagged ancestor. zh-CN: 获得焦点的控件或其最近的带标签祖先的测试标签. */
    private fun focusedTag(): String? {
        val node = compose.onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull() ?: return null
        return generateSequence(node) { it.parent }.firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.TestTag) } ?: UNTAGGED
    }

    /** Presses Tab until focus returns to a control seen before; every focused control must be on screen. zh-CN: 按 Tab 直到焦点回到见过的控件. */
    private fun tabCycle(): List<String> {
        val visited = mutableListOf<String>()
        repeat(MAX_TABS) {
            key(KeyEvent.KEYCODE_TAB)
            val tag = focusedTag() ?: return@repeat
            if (tag in visited) return visited
            compose.onNode(isFocused(), useUnmergedTree = true).assertIsDisplayed()
            visited += tag
        }
        return visited
    }

    private companion object {
        const val TIMEOUT = 15_000L
        const val MAX_TABS = 60
        const val UNTAGGED = "(untagged)"
    }
}
