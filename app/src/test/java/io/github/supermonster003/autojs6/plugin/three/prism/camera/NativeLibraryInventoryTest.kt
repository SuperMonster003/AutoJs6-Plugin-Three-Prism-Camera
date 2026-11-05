package io.github.supermonster003.autojs6.plugin.three.prism.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `PluginInfo.supportedAbis` per package (roadmap D22, appendix D Q8 = (b)): the ABIs whose whitelisted native libraries
 * the installed APK files carry.
 * zh-CN: 按包填写的 `PluginInfo.supportedAbis` (路线图 D22, 附录 D Q8 = (b)): 已安装 APK 文件中白名单原生库齐全的 ABI.
 */
class NativeLibraryInventoryTest {

    private fun entries(vararg abis: String): List<String> =
        listOf("AndroidManifest.xml", "classes.dex") + abis.flatMap { abi -> ThreePrismCameraContract.NATIVE_LIBRARIES.map { "lib/$abi/$it" } }

    @Test
    fun `a single-ABI APK reports its ABI`() {
        ThreePrismCameraContract.NATIVE_ABIS.forEach { abi ->
            assertEquals(listOf(abi), NativeLibraryInventory.supportedAbis(entries(abi)))
        }
    }

    @Test
    fun `the universal APK reports the four ABIs in the contract order`() {
        assertEquals(ThreePrismCameraContract.NATIVE_ABIS, NativeLibraryInventory.supportedAbis(entries("x86", "x86_64", "armeabi-v7a", "arm64-v8a")))
    }

    @Test
    fun `an ABI with a missing whitelisted library or outside the whitelist is not reported`() {
        val partial = entries("arm64-v8a") + "lib/x86_64/libimage_processing_util_jni.so" + ThreePrismCameraContract.NATIVE_LIBRARIES.map { "lib/riscv64/$it" }
        assertEquals(listOf("arm64-v8a"), NativeLibraryInventory.supportedAbis(partial))
        assertEquals(emptyList<String>(), NativeLibraryInventory.supportedAbis(listOf("AndroidManifest.xml")))
    }

    @Test
    fun `the inventory lists equal the release tool whitelists`() {
        val tool = RepositoryFiles.text(".python/release_sums.py")
        val abis = ThreePrismCameraContract.NATIVE_ABIS.joinToString(", ") { "\"$it\"" }
        val libraries = ThreePrismCameraContract.NATIVE_LIBRARIES.joinToString(", ") { "\"$it\"" }
        assertTrue(tool.contains("NATIVE_ABIS = [$abis]"))
        assertTrue(tool.contains("NATIVE_LIBRARIES = [$libraries]"))
    }

}
