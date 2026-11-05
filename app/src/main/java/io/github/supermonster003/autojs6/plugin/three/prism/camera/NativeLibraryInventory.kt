package io.github.supermonster003.autojs6.plugin.three.prism.camera

import android.content.Context
import java.util.zip.ZipFile

/**
 * The ABIs of the installed package (roadmap D22, appendix D Q8 = (b)). The splits share one BuildConfig, so
 * `PluginInfo.supportedAbis` is derived at run time from the `lib/<abi>/` entries of the installed APK files: a single-ABI
 * APK reports its ABI, the universal APK all four. An ABI counts only when every whitelisted library of
 * [ThreePrismCameraContract.NATIVE_LIBRARIES] is present for it.
 * zh-CN: 已安装包的 ABI (路线图 D22, 附录 D Q8 = (b)). 各分包共用一个 BuildConfig, 因此 `PluginInfo.supportedAbis` 在运行时根据
 * 已安装 APK 文件中的 `lib/<abi>/` 条目得出: 单 ABI 包报告其 ABI, Universal 包报告全部四个. 只有
 * [ThreePrismCameraContract.NATIVE_LIBRARIES] 中的白名单库在某个 ABI 下全部存在时, 才计入该 ABI.
 */
internal object NativeLibraryInventory {

    @Volatile
    private var cached: List<String>? = null

    /**
     * Scans the base APK and the split APKs of this package once per process (the installed files do not change while
     * the process lives); an unreadable APK contributes no entries.
     * zh-CN: 每个进程扫描一次本包的基础 APK 与拆分 APK (进程存活期间已安装文件不变); 无法读取的 APK 不贡献条目.
     */
    fun supportedAbis(context: Context): List<String> = cached ?: run {
        val applicationInfo = context.applicationInfo
        val apkPaths = listOfNotNull(applicationInfo.sourceDir) + applicationInfo.splitSourceDirs.orEmpty()
        val entries = buildSet {
            apkPaths.forEach { path ->
                runCatching {
                    ZipFile(path).use { zip ->
                        val enumeration = zip.entries()
                        while (enumeration.hasMoreElements()) add(enumeration.nextElement().name)
                    }
                }
            }
        }
        supportedAbis(entries).also { cached = it }
    }

    /**
     * The ABIs, in the order of [ThreePrismCameraContract.NATIVE_ABIS], for which [entryNames] hold every whitelisted
     * library. zh-CN: [entryNames] 中白名单库齐全的 ABI, 顺序同 [ThreePrismCameraContract.NATIVE_ABIS].
     */
    fun supportedAbis(entryNames: Iterable<String>): List<String> {
        val entries = entryNames.toHashSet()
        return ThreePrismCameraContract.NATIVE_ABIS.filter { abi ->
            ThreePrismCameraContract.NATIVE_LIBRARIES.all { library -> "lib/$abi/$library" in entries }
        }
    }

}
