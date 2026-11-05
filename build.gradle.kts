// @Hint: declared here, applied by the modules.
//  ! The platform-versions plugin publishes the AGP and Kotlin versions as system properties before this script runs, so the
//  ! plugin versions below come from one central decision and are never hardcoded.
//  ! zh-CN: platform-versions 插件在本脚本执行前以系统属性发布 AGP 与 Kotlin 版本, 因此下列插件版本来自同一个中央决策, 不做硬编码.
plugins {
    id("com.android.application") version System.getProperty("gradle.agp.version") apply false
    id("org.jetbrains.kotlin.plugin.compose") version System.getProperty("gradle.kotlin.version") apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

tasks {
    register<Delete>("clean").configure {
        delete(rootProject.layout.buildDirectory)
    }
}
