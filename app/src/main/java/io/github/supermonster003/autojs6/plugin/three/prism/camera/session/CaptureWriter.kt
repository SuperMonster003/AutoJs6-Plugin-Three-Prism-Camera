package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Bundle
import android.os.ParcelFileDescriptor
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraResultKeys
import java.io.File
import java.util.UUID

/**
 * The picture files of `capture` and the viewfinder (protocol "Capture", roadmap D45): CameraX writes the JPEG to
 * `cacheDir/captures/<uuid>.jpg`; [seal] reads its meta, opens it read-only and deletes the name, so only the
 * descriptor handed to the host keeps the bytes.
 * zh-CN: `capture` 与取景页的照片文件 (协议 "Capture" 一节, 路线图 D45): CameraX 把 JPEG 写到 `cacheDir/captures/<uuid>.jpg`; [seal] 读取其元数据,
 * 以只读方式打开并删除文件名, 因此只有交给宿主的描述符保有这些字节.
 */
internal object CaptureWriter {

    const val DIRECTORY = "captures"

    class Sealed(val descriptor: ParcelFileDescriptor, val meta: Bundle)

    fun newFile(context: Context): File {
        val directory = File(context.applicationContext.cacheDir, DIRECTORY).apply { mkdirs() }
        return File(directory, "${UUID.randomUUID()}.jpg")
    }

    /**
     * The descriptor and the `onCaptured` meta of a saved picture; the file is deleted on every path. A picture above
     * `MAX_CAPTURE_BYTES` fails with `LIMIT_EXCEEDED`.
     * zh-CN: 已保存照片的描述符与 `onCaptured` 元数据; 任何路径下文件都会被删除. 超过 `MAX_CAPTURE_BYTES` 的照片以 `LIMIT_EXCEEDED` 失败.
     */
    fun seal(file: File, facing: String, version: Int): Sealed {
        try {
            val byteCount = file.length()
            if (byteCount > CameraLimits.MAX_CAPTURE_BYTES) {
                throw CameraFailure(CameraErrorCodes.LIMIT_EXCEEDED, "the picture has $byteCount bytes", limit = CameraLimits.MAX_CAPTURE_BYTES)
            }
            if (byteCount <= 0L) throw CameraFailure(CameraErrorCodes.INTERNAL, "the camera saved an empty picture")
            val orientation = runCatching {
                ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val transform = ExifOrientation.transformOf(orientation)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val meta = answer(version) {
                putLong(CameraResultKeys.BYTE_COUNT, byteCount)
                putInt(CameraResultKeys.WIDTH, bounds.outWidth)
                putInt(CameraResultKeys.HEIGHT, bounds.outHeight)
                putInt(CameraResultKeys.ROTATION_DEGREES, transform.rotationDegrees)
                putBoolean(CameraResultKeys.MIRRORED, transform.mirrored)
                putString(CameraResultKeys.FACING, facing)
            }
            return Sealed(descriptor, meta)
        } finally {
            file.delete()
        }
    }

    /** Removes pictures a killed process left behind. zh-CN: 删除被杀进程遗留的照片. */
    fun cleanStale(context: Context) {
        File(context.applicationContext.cacheDir, DIRECTORY).listFiles()?.forEach { it.delete() }
    }

}
