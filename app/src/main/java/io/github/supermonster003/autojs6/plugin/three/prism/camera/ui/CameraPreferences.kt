package io.github.supermonster003.autojs6.plugin.three.prism.camera.ui

import android.content.Context
import org.autojs.plugin.camera.api.CameraContract

/**
 * The plugin's own camera settings (roadmap P3.2, appendix D Q41 = (a)): only the default lens of the viewfinder opened
 * from the launcher. Script sessions and `openCaptureUi` use the camera of the request, and the analysis size and rate
 * stay the protocol defaults the host sends, so nothing here changes what scripts get.
 * zh-CN: 插件自己的相机设置 (路线图 P3.2, 附录 D Q41 = (a)): 只有从启动器打开的取景页的默认镜头. 脚本会话与 `openCaptureUi` 使用请求中的镜头,
 * 分析尺寸与帧率仍是宿主发送的协议缺省值, 因此这里的设置不改变脚本得到的结果.
 */
internal object CameraPreferences {

    private const val FILE = "camera_preferences"
    private const val KEY_DEFAULT_FACING = "default_facing"

    /** The lenses the setting offers. zh-CN: 设置提供的镜头. */
    val FACINGS = listOf(CameraContract.FACING_BACK, CameraContract.FACING_FRONT)

    fun defaultFacing(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_FACING, null)?.takeIf { it in FACINGS } ?: CameraContract.FACING_BACK

    /** Saves at once; false when the write failed. zh-CN: 立即保存; 写入失败时为 false. */
    fun saveDefaultFacing(context: Context, facing: String): Boolean {
        require(facing in FACINGS) { "unknown facing $facing" }
        return prefs(context).edit().putString(KEY_DEFAULT_FACING, facing).commit()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

}
