package io.github.supermonster003.autojs6.plugin.three.prism.camera.ui

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.prism.camera.session.SessionDefaults
import org.autojs.plugin.camera.api.CameraContract

/**
 * The plugin's own camera settings (roadmap P3.2, appendix D Q41 = (b)): the default lens, analysis size and frame rate.
 * The lens applies to the viewfinder opened from the launcher and, like the size and the rate, to every script request
 * that omits the key (`openSession`, `startAnalysis`, `openCaptureUi`); a key the script sets always wins.
 * zh-CN: 插件自己的相机设置 (路线图 P3.2, 附录 D Q41 = (b)): 默认镜头, 分析分辨率与帧率. 镜头作用于从启动器打开的取景页, 并与分辨率, 帧率一样作用于
 * 省略该键的每个脚本请求 (`openSession`, `startAnalysis`, `openCaptureUi`); 脚本设置的键总是优先.
 */
internal object CameraPreferences {

    private const val FILE = "camera_preferences"
    private const val KEY_DEFAULT_FACING = "default_facing"
    private const val KEY_ANALYSIS_SIZE = "default_analysis_size"

    /** The stored frame rate; 0 is the camera's own rate. zh-CN: 存储的帧率; 0 为相机自身的帧率. */
    private const val KEY_FPS = "default_fps"

    /** The lenses the setting offers. zh-CN: 设置提供的镜头. */
    val FACINGS = listOf(CameraContract.FACING_BACK, CameraContract.FACING_FRONT)

    fun defaultFacing(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_FACING, null)?.takeIf { it in FACINGS } ?: SessionDefaults.PROTOCOL.facing

    fun analysisSize(context: Context): Pair<Int, Int> =
        SessionDefaults.parseSize(prefs(context).getString(KEY_ANALYSIS_SIZE, null))?.takeIf { it in SessionDefaults.SIZES }
            ?: (SessionDefaults.PROTOCOL.width to SessionDefaults.PROTOCOL.height)

    fun fps(context: Context): Int? = prefs(context).getInt(KEY_FPS, 0).takeIf { it != 0 && it in SessionDefaults.FPS_CHOICES }

    /** What a script request falls back to (read on the Binder thread). zh-CN: 脚本请求回退到的值 (在 Binder 线程读取). */
    fun sessionDefaults(context: Context): SessionDefaults {
        val (width, height) = analysisSize(context)
        return SessionDefaults(defaultFacing(context), width, height, fps(context))
    }

    /** Saves at once; false when the write failed. zh-CN: 立即保存; 写入失败时为 false. */
    fun saveDefaultFacing(context: Context, facing: String): Boolean {
        require(facing in FACINGS) { "unknown facing $facing" }
        return prefs(context).edit().putString(KEY_DEFAULT_FACING, facing).commit()
    }

    fun saveAnalysisSize(context: Context, size: Pair<Int, Int>): Boolean {
        require(size in SessionDefaults.SIZES) { "unknown analysis size $size" }
        return prefs(context).edit().putString(KEY_ANALYSIS_SIZE, SessionDefaults.formatSize(size)).commit()
    }

    fun saveFps(context: Context, fps: Int?): Boolean {
        require(fps in SessionDefaults.FPS_CHOICES) { "unknown frame rate $fps" }
        return prefs(context).edit().putInt(KEY_FPS, fps ?: 0).commit()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

}
