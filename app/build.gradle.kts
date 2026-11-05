import com.android.apksig.ApkVerifier
import com.android.build.api.variant.FilterConfiguration
import com.android.build.api.variant.VariantOutputConfiguration
import org.gradle.api.provider.Property
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.prism.camera"

// Minimum host build (roadmap D39, appendix D Q9 = (a)): 6641, the first host build after the public 6.8.0 (6640) and the
// value of CameraPluginIds.REQUIRED_HOST_VERSION_CODE in the host contract, until roadmap P5.4 writes the build that ships
// the camera contract. The host versionCode is its VERSION_BUILD. Keep it in sync with
// ThreePrismCameraContract.REQUIRED_HOST_VERSION and .readme/common.json.
// zh-CN: 最低宿主构建号 (路线图 D39, 附录 D Q9 = (a)): 6641, 即已公开的 6.8.0 (6640) 之后的第一个宿主构建, 与宿主契约中的
// CameraPluginIds.REQUIRED_HOST_VERSION_CODE 相同, 路线图 P5.4 回填实际发布相机契约的构建. 宿主 versionCode 即其 VERSION_BUILD.
// 与 ThreePrismCameraContract.REQUIRED_HOST_VERSION 及 .readme/common.json 保持一致.
val requiredHostVersion = 6641

val buildTypeDebug = "debug"
val buildTypeRelease = "release"

fun File.sha256(): String = inputStream().buffered().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

// Lock files (new repository specification 5.5): `format=1` first, unique keys, comments and blank lines skipped.
// zh-CN: 锁文件 (新仓库规范 5.5): 首行 `format=1`, 键唯一, 跳过注释与空行.
fun readLock(path: String): Map<String, String> {
    val lockFile = rootProject.file(path)
    check(lockFile.isFile) { "Missing $path" }
    val entries = LinkedHashMap<String, String>()
    lockFile.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
        val key = line.substringBefore('=').trim()
        check(line.contains('=') && entries.put(key, line.substringAfter('=').trim()) == null) { "Malformed or duplicate entry in $path: $line" }
    }
    check(entries["format"] == "1") { "Unsupported lock format in $path" }
    return entries
}

fun verifiedFile(path: String, expected: String?, label: String): File {
    val file = rootProject.file(path)
    check(file.isFile) { "Missing $path ($label)" }
    check(expected != null && expected.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256 for $label" }
    val actual = file.sha256()
    check(actual == expected) { "SHA-256 mismatch for $path: expected $expected, actual $actual" }
    return file
}

// The host contract AARs (roadmap 5.5): common-plugin-api and camera-api (roadmap P2.1, both built from one recorded host
// commit that contains the contract module of P1.1). Release AARs only, staged under libs/ and pinned by locks/host-api-aars.lock.
// zh-CN: 宿主契约 AAR (路线图 5.5): common-plugin-api 与 camera-api (路线图 P2.1, 均由同一个记录在案且含 P1.1 契约模块的宿主提交
// 构建). 只接受 release AAR, 暂存于 libs/ 并由 locks/host-api-aars.lock 固定.
val hostApiAars: List<File> = run {
    val entries = readLock("locks/host-api-aars.lock")
    val names = entries.keys.filter { it.endsWith(".file") }.map { it.removeSuffix(".file") }.sorted()
    check(entries.keys == setOf("format") + names.flatMap { listOf("$it.file", "$it.sha256") }) { "Unexpected keys in locks/host-api-aars.lock" }
    names.map { name ->
        val fileName = entries.getValue("$name.file")
        check(fileName == File(fileName).name && fileName.endsWith(".aar") && !fileName.endsWith("-debug.aar")) { "Invalid $name.file" }
        verifiedFile("libs/$fileName", entries["$name.sha256"], name)
    }
}

// Native code inventory (roadmap D22, amended by the P0.2 evidence): the plugin has no native code of its own, but
// camera-core bundles libimage_processing_util_jni.so and libsurface_util_jni.so, and Compose ui-graphics depends on
// androidx.graphics:graphics-path with libandroidx.graphics.path.so, each for the four Android ABIs (202140 bytes in
// CameraX 1.6.2 / Compose BOM 2026.09.00). Appendix D Q8 = (b): one APK per ABI plus the universal APK (new repository
// specification 5.5); the release gate verifies that every package holds exactly these libraries for its ABIs, stored
// uncompressed and 16 KB page-aligned (ELF PT_LOAD and zip data offset), so that 16 KB page devices map them straight from
// the APK. A dependency update that changes the set changes this list, D22 and the README together.
// zh-CN: 原生代码清单 (路线图 D22, 按 P0.2 证据修订): 插件自身没有原生代码, 但 camera-core 自带
// libimage_processing_util_jni.so 与 libsurface_util_jni.so, Compose ui-graphics 依赖的 androidx.graphics:graphics-path 自带
// libandroidx.graphics.path.so, 各含四个 Android ABI (CameraX 1.6.2 / Compose BOM 2026.09.00 共 202140 字节). 附录 D Q8 = (b):
// 每个 ABI 一个 APK 并附 Universal APK (新仓库规范 5.5); release 门禁逐包校验其 ABI 恰好是这些库, 不压缩存储, 且按 16 KB 页
// 对齐 (ELF PT_LOAD 与 zip 数据偏移), 以便 16 KB 页设备直接从 APK 映射. 依赖升级改变库集合时, 同时修改此清单, D22 与 README.
val nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val allowedNativeLibraries = listOf("libandroidx.graphics.path.so", "libimage_processing_util_jni.so", "libsurface_util_jni.so")
val nativePageAlignment = 16384L

fun elfMinimumLoadAlignment(bytes: ByteArray): Long {
    check(bytes.size > 0x40 && bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
        "Not an ELF file"
    }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val is64 = bytes[4].toInt() == 2
    val programHeaderOffset = if (is64) buffer.getLong(0x20) else (buffer.getInt(0x1C).toLong() and 0xFFFFFFFFL)
    val entrySize = buffer.getShort(if (is64) 0x36 else 0x2A).toInt() and 0xFFFF
    val entryCount = buffer.getShort(if (is64) 0x38 else 0x2C).toInt() and 0xFFFF
    var minimum = Long.MAX_VALUE
    repeat(entryCount) { index ->
        val offset = (programHeaderOffset + index.toLong() * entrySize).toInt()
        if (buffer.getInt(offset) == 1) { // PT_LOAD
            val alignment = if (is64) buffer.getLong(offset + 48) else (buffer.getInt(offset + 28).toLong() and 0xFFFFFFFFL)
            minimum = minOf(minimum, alignment)
        }
    }
    check(minimum != Long.MAX_VALUE) { "No PT_LOAD segment" }
    return minimum
}

/** Data offsets of the `lib/` entries inside [apk], read from the central directory and the local headers. */
fun nativeEntryDataOffsets(apk: File): Map<String, Long> = RandomAccessFile(apk, "r").use { file ->
    val length = file.length()
    val tailLength = minOf(length, 22L + 65535L).toInt()
    val tail = ByteArray(tailLength).also { file.seek(length - tailLength); file.readFully(it) }
    val tailBuffer = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
    val eocd = (tail.size - 22 downTo 0).firstOrNull { tailBuffer.getInt(it) == 0x06054b50 }
    checkNotNull(eocd) { "End of central directory not found in ${apk.name}" }
    val entryCount = tailBuffer.getShort(eocd + 10).toInt() and 0xFFFF
    val directorySize = tailBuffer.getInt(eocd + 12).toLong() and 0xFFFFFFFFL
    val directoryOffset = tailBuffer.getInt(eocd + 16).toLong() and 0xFFFFFFFFL
    val directory = ByteArray(directorySize.toInt()).also { file.seek(directoryOffset); file.readFully(it) }
    val directoryBuffer = ByteBuffer.wrap(directory).order(ByteOrder.LITTLE_ENDIAN)
    val offsets = LinkedHashMap<String, Long>()
    var position = 0
    repeat(entryCount) {
        check(directoryBuffer.getInt(position) == 0x02014b50) { "Corrupt central directory in ${apk.name}" }
        val nameLength = directoryBuffer.getShort(position + 28).toInt() and 0xFFFF
        val extraLength = directoryBuffer.getShort(position + 30).toInt() and 0xFFFF
        val commentLength = directoryBuffer.getShort(position + 32).toInt() and 0xFFFF
        val localHeaderOffset = directoryBuffer.getInt(position + 42).toLong() and 0xFFFFFFFFL
        val name = String(directory, position + 46, nameLength, Charsets.UTF_8)
        if (name.startsWith("lib/")) {
            val local = ByteArray(30).also { file.seek(localHeaderOffset); file.readFully(it) }
            val localBuffer = ByteBuffer.wrap(local).order(ByteOrder.LITTLE_ENDIAN)
            check(localBuffer.getInt(0) == 0x04034b50) { "Corrupt local header for $name" }
            val localNameLength = localBuffer.getShort(26).toInt() and 0xFFFF
            val localExtraLength = localBuffer.getShort(28).toInt() and 0xFFFF
            offsets[name] = localHeaderOffset + 30 + localNameLength + localExtraLength
        }
        position += 46 + nameLength + extraLength + commentLength
    }
    offsets
}

/** Verifies that [apk] holds exactly the whitelisted libraries of [abis], stored and 16 KB aligned (roadmap D22). */
fun verifyNativeLibraries(apk: File, abis: List<String>) {
    val expected = abis.flatMap { abi -> allowedNativeLibraries.map { "lib/$abi/$it" } }.toSet()
    ZipFile(apk).use { zip ->
        val entries = zip.entries().asSequence().filter { it.name.startsWith("lib/") || it.name.endsWith(".so") }.toList()
        check(entries.map { it.name }.toSet() == expected) {
            "Unexpected native library set in ${apk.name}: expected ${expected.sorted()}, actual ${entries.map { it.name }.sorted()}"
        }
        entries.forEach { entry ->
            check(entry.method == ZipEntry.STORED) { "${entry.name} must be stored uncompressed so it can be mapped from the APK" }
            val alignment = elfMinimumLoadAlignment(zip.getInputStream(entry).use { it.readBytes() })
            check(alignment >= nativePageAlignment) { "${entry.name} PT_LOAD alignment $alignment is below $nativePageAlignment" }
        }
    }
    nativeEntryDataOffsets(apk).forEach { (name, offset) ->
        check(offset % nativePageAlignment == 0L) { "$name data offset $offset is not $nativePageAlignment-byte aligned in the APK" }
    }
}

android {
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Identity strings come from the build and are never translated (new repository specification 11).
        // zh-CN: 身份字符串来自构建, 不翻译 (新仓库规范第 11 节).
        resValue("string", "app_name", "3-Prism Camera")
        // Brand name answered as PluginInfo.name; never translated (roadmap P0.1). zh-CN: PluginInfo.name 使用的品牌名, 不翻译 (路线图 P0.1).
        resValue("string", "plugin_name", "3-Prism Camera")
        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_id", "three-prism-camera")
        resValue("string", "plugin_engine", "camera")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_requires_host_version", "$requiredHostVersion")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    lint {
        abortOnError = true
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        resValues = true
    }

    // One APK per ABI plus the universal APK (roadmap D22, appendix D Q8 = (b), new repository specification 5.5). BuildConfig
    // is shared by the splits, so getInfo() derives supportedAbis at run time from the lib/<abi>/ entries of the installed APK.
    // zh-CN: 每个 ABI 一个 APK 并附 Universal APK (路线图 D22, 附录 D Q8 = (b), 新仓库规范 5.5). 各分包共用 BuildConfig, 因此
    // getInfo() 在运行时根据已安装 APK 的 lib/<abi>/ 条目得出 supportedAbis.
    splits {
        abi {
            isEnable = true
            reset()
            include(*nativeAbis.toTypedArray())
            isUniversalApk = true
        }
    }

    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }

    bundle {
        language.enableSplit = false
        density.enableSplit = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val architecture = output.filters.find {
                it.filterType == FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal".takeIf { output.outputType == VariantOutputConfiguration.OutputType.UNIVERSAL }
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    val abiSuffix = architecture?.let { "-$it" }.orEmpty()
                    "${rootProject.name}-v$version$abiSuffix.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    implementation(files(hostApiAars))

    implementation(libs.core.ktx)

    // Jetpack CameraX 1.6.2 (roadmap D21): the use cases, the Camera2 back end, the lifecycle binding and CameraXViewfinder
    // for the Compose viewfinder (maintainer decision Q3 = (b), 2026-11-05). camera-view is not used.
    // zh-CN: Jetpack CameraX 1.6.2 (路线图 D21): use case, Camera2 后端, 生命周期绑定, 以及 Compose 取景页用的 CameraXViewfinder
    // (维护者 2026-11-05 拍板 Q3 = (b)). 不使用 camera-view.
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.compose)

    // Jetpack Compose (roadmap D41): the BOM is the single version source; activity-compose follows it.
    // zh-CN: Jetpack Compose (路线图 D41): BOM 是唯一的版本来源, activity-compose 随之.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    // Signed release APKs (roadmap D22, appendix D Q8 = (b)): exactly one APK per ABI plus the universal APK, each with a valid
    // signature and exactly the whitelisted native libraries of its ABIs, stored and 16 KB aligned, then
    // `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{ABI}-{CRC32}.apk` in releases/ (`universal` for the universal APK).
    // zh-CN: 已签名 release APK (路线图 D22, 附录 D Q8 = (b)): 每个 ABI 恰好一个 APK 并附 Universal APK, 每个签名有效, 原生库恰为
    // 其 ABI 的白名单且不压缩, 按 16 KB 对齐, 然后以 `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{ABI}-{CRC32}.apk` 放入 releases/
    // (Universal APK 的 ABI 为 `universal`).
    register("appendDigestToReleasedFiles") {
        group = "distribution"
        description = "Builds and verifies the signed per-ABI and universal release APKs of 3-Prism Camera, then appends their CRC32 digests"
        dependsOn("assembleRelease")
        doLast {
            check(signs.isValid) { "Release signing is not configured (sign.properties); refusing to collect an unsigned APK" }
            val extension = utils.FILE_EXTENSION_APK
            val prefix = "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}".lowercase()
            val packages = (nativeAbis + "universal").associateBy { "$prefix-$it.$extension" }
            val source = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease").get().asFile
            val apks = source.listFiles { candidate -> candidate.extension == extension }.orEmpty().associateBy { it.name }
            check(apks.keys == packages.keys) {
                "Unexpected release APK set in $source: expected ${packages.keys.sorted()}, actual ${apks.keys.sorted()}"
            }
            val destination = file("$rootDir/${buildTypeRelease}s")
            check(destination.isDirectory || destination.mkdirs()) { "Cannot create $destination" }
            val collected = packages.map { (name, abi) ->
                val apk = apks.getValue(name)
                val verification = ApkVerifier.Builder(apk).build().verify()
                check(verification.isVerified) { "Invalid or unsigned release APK ${apk.name}: ${verification.errors}" }
                verifyNativeLibraries(apk, if (abi == "universal") nativeAbis else listOf(abi))
                "$prefix-$abi-${utils.digestCRC32(apk)}.$extension".also { apk.copyTo(destination.resolve(it), overwrite = true) }
            }.toSet()
            destination.listFiles { file -> file.name.startsWith("$prefix-") && file.extension == extension }
                .orEmpty()
                .filter { it.name !in collected }
                .forEach { stale -> check(stale.delete()) { "Cannot remove superseded artifact: $stale" } }
            println("Destination: $destination")
            collected.sorted().forEach(::println)
        }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}
