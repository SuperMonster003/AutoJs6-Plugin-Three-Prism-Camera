package io.github.supermonster003.autojs6.plugin.three.prism.camera.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.prism.camera.R
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CameraController
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.CaptureUiRegistry

/**
 * Launcher target of the plugin. Roadmap P3.1 turns it into the capture screen (CameraXViewfinder of `camera-compose`,
 * roadmap D41, appendix D Q3); the skeleton shows the plugin description and the camera count of the CameraX probe
 * (roadmap P0.2), which is enough to see on a device that CameraX initializes.
 * zh-CN: 插件的启动器目标. 路线图 P3.1 把它改为取景页 (`camera-compose` 的 CameraXViewfinder, 路线图 D41, 附录 D Q3); 骨架显示
 * 插件描述与 CameraX 探测得到的镜头数 (路线图 P0.2), 足以在设备上确认 CameraX 能够初始化.
 *
 * Started by `openCaptureUi` it carries [CaptureUiRegistry.EXTRA_TOKEN]; closing it without a picture answers
 * `CANCELLED` through the registry (protocol "Capture").
 * zh-CN: 由 `openCaptureUi` 启动时携带 [CaptureUiRegistry.EXTRA_TOKEN]; 未拍照就关闭时经登记表应答 `CANCELLED` (协议 "Capture" 一节).
 */
class ThreePrismCameraCaptureActivity : ComponentActivity() {

    private val probe = mutableStateOf<CameraController.Probe?>(null)
    private var captureToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        captureToken = intent?.getStringExtra(CaptureUiRegistry.EXTRA_TOKEN)
        CameraController(this).probe { probe.value = it }
        setContent {
            PrismTheme {
                SkeletonScreen(probe.value)
            }
        }
    }

    override fun onDestroy() {
        // A rotation recreates the activity; only a real close cancels the pending result.
        // zh-CN: 旋转会重建 Activity; 只有真正关闭时才取消待返回的结果.
        if (isFinishing) captureToken?.let(CaptureUiRegistry::cancel)
        super.onDestroy()
    }

}

@Composable
private fun PrismTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content)
}

@Composable
private fun SkeletonScreen(probe: CameraController.Probe?) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.plugin_name), style = MaterialTheme.typography.headlineSmall)
            Text(text = stringResource(R.string.plugin_description), style = MaterialTheme.typography.bodyLarge)
            val status = when {
                probe == null -> null
                probe.cameraCount != null -> stringResource(R.string.capture_camera_count, probe.cameraCount)
                else -> stringResource(R.string.capture_camera_unavailable)
            }
            if (status != null) {
                Text(text = status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = stringResource(R.string.capture_skeleton_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
