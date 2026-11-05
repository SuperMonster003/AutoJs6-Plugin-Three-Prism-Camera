package io.github.supermonster003.autojs6.plugin.three.prism.camera

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * CameraX smoke test (roadmap P0.4 placeholder): `ProcessCameraProvider` initializes in the plugin process and reports at
 * least one camera. Enumerating cameras needs no `CAMERA` permission, so the test runs without a permission dialog. The
 * CI AVD starts with `-camera-back virtualscene -camera-front emulated` (roadmap D35); roadmap P4.1 adds the session cases.
 * zh-CN: CameraX 冒烟测试 (路线图 P0.4 占位): `ProcessCameraProvider` 能在插件进程内初始化, 并报告至少一个镜头. 枚举镜头不需要
 * `CAMERA` 权限, 因此测试不弹权限对话框. CI 的 AVD 以 `-camera-back virtualscene -camera-front emulated` 启动 (路线图 D35);
 * 会话用例由路线图 P4.1 补充.
 */
@RunWith(AndroidJUnit4::class)
class CameraSmokeTest {

    @Test
    fun cameraXReportsAvailableCameras() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val provider = ProcessCameraProvider.getInstance(context).get(20, TimeUnit.SECONDS)
        val count = provider.availableCameraInfos.size
        assertTrue("availableCameraInfos must not be empty, got $count", count > 0)
    }

}
