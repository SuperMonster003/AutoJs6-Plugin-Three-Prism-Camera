# Third-party notices

3-Prism Camera is licensed under the Mozilla Public License 2.0 (see `LICENSE`). The APK bundles the
components below; their licenses apply to those components only. Versions are the ones resolved for the release
build (`:app:dependencies --configuration releaseRuntimeClasspath`); R8 removes the classes the plugin does not use.

## AutoJs6 host contract libraries

| Artifact | Source | License |
| --- | --- | --- |
| `libs/common-plugin-api.aar` | AutoJs6 host module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6) | MPL-2.0 |

Provenance and SHA-256 digests are recorded in `locks/host-api-aars.lock` and `libs/README.md`. The camera contract
(`libs/camera-api.aar`) joins in roadmap P2.1.

## Jetpack CameraX (Maven)

| Library | Version | License |
| --- | --- | --- |
| CameraX core, Camera2 back end, Camera2 pipe, lifecycle and Compose (`androidx.camera:camera-core`, `camera-camera2`, `camera-camera2-pipe`, `camera-lifecycle`, `camera-compose`) | 1.6.2 | Apache-2.0 |
| CameraX viewfinder (`androidx.camera.viewfinder:viewfinder-core`, `viewfinder-compose`) | 1.6.2 | Apache-2.0 |
| CameraX feature combination query (`androidx.camera.featurecombinationquery:featurecombinationquery`) | 1.6.2 | Apache-2.0 |
| AndroidX ExifInterface (`androidx.exifinterface:exifinterface`) | 1.4.2 | Apache-2.0 |
| AndroidX Concurrent futures (`androidx.concurrent:concurrent-futures`, `concurrent-futures-ktx`) | 1.1.0 | Apache-2.0 |
| Guava ListenableFuture (`com.google.guava:listenablefuture`) | 1.0 | Apache-2.0 |
| Dagger (`com.google.dagger:dagger`, used by the Camera2 pipe) | 2.59 | Apache-2.0 |
| JSR 330 and Jakarta Inject annotations (`javax.inject:javax.inject` 1, `jakarta.inject:jakarta.inject-api` 2.0.1) | see column | Apache-2.0 |
| AutoValue annotations (`com.google.auto.value:auto-value-annotations`) | 1.6.3 | Apache-2.0 |

## Jetpack Compose and AndroidX (Maven)

| Library | Version | License |
| --- | --- | --- |
| Compose BOM (`androidx.compose:compose-bom`): runtime, ui, ui-graphics, ui-text, ui-tooling-preview, foundation, animation, material-ripple | 2026.09.00 (Compose 1.12.1) | Apache-2.0 |
| Compose Material 3 (`androidx.compose.material3:material3`) | 1.4.0 | Apache-2.0 |
| AndroidX Graphics Path (`androidx.graphics:graphics-path`, used by Compose ui-graphics) | 1.0.1 | Apache-2.0 |
| AndroidX Activity (`androidx.activity:activity`, `activity-ktx`, `activity-compose`) | 1.12.2 | Apache-2.0 |
| AndroidX Core (`androidx.core:core`, `core-ktx`) | 1.16.0 | Apache-2.0 |
| AndroidX Lifecycle (`androidx.lifecycle:*`) | 2.9.4 | Apache-2.0 |
| AndroidX SavedState (`androidx.savedstate:*`) | 1.3.2 | Apache-2.0 |
| AndroidX NavigationEvent (`androidx.navigationevent:*`) | 1.0.1 | Apache-2.0 |
| AndroidX Window (`androidx.window:window`, `window-core`) | 1.5.0 | Apache-2.0 |
| AndroidX Emoji2, ProfileInstaller, Startup, Tracing, Collection, Annotation, Arch Core, Autofill, CustomView PoolingContainer, Interpolator, VersionedParcelable, Core ViewTree, Core Backported Fixes | as resolved by the BOM | Apache-2.0 |

## Kotlin libraries (Maven)

| Library | Version | License |
| --- | --- | --- |
| Kotlin standard library (`org.jetbrains.kotlin:kotlin-stdlib`, added by the Android Gradle Plugin) | follows the build's Kotlin version (2.3.20) | Apache-2.0 |
| kotlinx.coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-core`, `kotlinx-coroutines-android`) | 1.9.0 | Apache-2.0 |
| kotlinx.serialization core (`org.jetbrains.kotlinx:kotlinx-serialization-core`) | 1.7.3 | Apache-2.0 |
| atomicfu (`org.jetbrains.kotlinx:atomicfu`) | 0.28.0 | Apache-2.0 |
| JetBrains annotations (`org.jetbrains:annotations`), JSpecify (`org.jspecify:jspecify`) | 23.0.0, 1.0.0 | Apache-2.0 |

## Native libraries

The plugin has no native code of its own. The APK carries the native helpers of the libraries above for the four
Android ABIs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`), stored uncompressed and 16 KB page-aligned (roadmap D22):

| Library file | From | License |
| --- | --- | --- |
| `libimage_processing_util_jni.so`, `libsurface_util_jni.so` | CameraX `camera-core` 1.6.2 | Apache-2.0 |
| `libandroidx.graphics.path.so` | AndroidX `graphics-path` 1.0.1 | Apache-2.0 |

The full text of the Apache License 2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.

Libraries used only by the JVM and device tests (JUnit, AndroidX Test) are not part of the APK.
