package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import org.autojs.plugin.camera.api.CameraErrorCodes
import org.autojs.plugin.camera.api.CameraLimits
import org.autojs.plugin.camera.api.CameraResultKeys
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * The picture files of `capture` and the viewfinder (protocol "Capture", roadmap D45, appendix D Q13 = (b)): [write]
 * copies the JPEG of an in-memory capture to `cacheDir/captures/<uuid>.jpg` and settles its EXIF orientation; [seal]
 * reads its meta, opens it read-only and deletes the name, so only the descriptor handed to the host keeps the bytes.
 * zh-CN: `capture` 与取景页的照片文件 (协议 "Capture" 一节, 路线图 D45, 附录 D Q13 = (b)): [write] 把内存拍照的 JPEG 复制到
 * `cacheDir/captures/<uuid>.jpg` 并确定其 EXIF 方向; [seal] 读取其元数据, 以只读方式打开并删除文件名, 因此只有交给宿主的描述符保有这些字节.
 */
internal object CaptureWriter {

    const val DIRECTORY = "captures"

    class Sealed(val descriptor: ParcelFileDescriptor, val meta: Bundle)

    fun newFile(context: Context): File {
        val directory = File(context.applicationContext.cacheDir, DIRECTORY).apply { mkdirs() }
        return File(directory, "${UUID.randomUUID()}.jpg")
    }

    /**
     * Copies the JPEG of [image] to [file] (the caller closes [image]) and sets the EXIF orientation: a 90 or 270
     * degree turn the camera left out is filled in from [expectedRotation] (appendix D Q14, see
     * [ExifOrientation.completed]), then [mirror] flips it horizontally (1 <-> 2, 3 <-> 4, 5 <-> 6, 7 <-> 8) instead
     * of re-encoding the pixels. A picture above `MAX_CAPTURE_BYTES` fails with `LIMIT_EXCEEDED` before anything is
     * written.
     * zh-CN: 把 [image] 的 JPEG 复制到 [file] (由调用方关闭 [image]) 并设置 EXIF 方向: 先按 [expectedRotation] 补上相机遗漏的 90 或 270 度旋转
     * (附录 D Q14, 见 [ExifOrientation.completed]), 再按 [mirror] 水平翻转方向 (1 <-> 2, 3 <-> 4, 5 <-> 6, 7 <-> 8), 不重新编码像素.
     * 超过 `MAX_CAPTURE_BYTES` 的照片在写入前以 `LIMIT_EXCEEDED` 失败.
     */
    fun write(image: ImageProxy, file: File, mirror: Boolean, expectedRotation: Int) {
        if (image.format != ImageFormat.JPEG) throw CameraFailure(CameraErrorCodes.INTERNAL, "the camera delivered image format ${image.format} instead of JPEG")
        val buffer = image.planes[0].buffer.duplicate().apply { rewind() }
        val byteCount = buffer.remaining().toLong()
        if (byteCount > CameraLimits.MAX_CAPTURE_BYTES) {
            throw CameraFailure(CameraErrorCodes.LIMIT_EXCEEDED, "the picture has $byteCount bytes", limit = CameraLimits.MAX_CAPTURE_BYTES)
        }
        if (byteCount <= 0L) throw CameraFailure(CameraErrorCodes.INTERNAL, "the camera delivered an empty picture")
        FileOutputStream(file).use { output ->
            val channel = output.channel
            while (buffer.hasRemaining()) channel.write(buffer)
        }
        val exif = ExifInterface(file)
        val stored = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val upright = ExifOrientation.completed(stored, expectedRotation, bounds.outWidth > bounds.outHeight)
        val orientation = if (mirror) ExifOrientation.mirroredOf(upright) else upright
        if (orientation != stored) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
        }
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
                ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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
