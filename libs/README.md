# Staged binary artifacts

This directory holds the exact, hash-locked binary artifacts the plugin consumes. Gradle never resolves
them from sibling repositories or from `mavenLocal()`: `app/build.gradle.kts` reads `../locks/host-api-aars.lock`
and fails configuration when a staged file is missing, listed with an invalid digest or different from its
locked SHA-256.

## Host contract AARs (lock `../locks/host-api-aars.lock`)

Release artifacts of the AutoJs6 host, consumed with `implementation` because the plugin runs in its own
process and needs the AIDL stubs and constants at run time.

| File | Host module | Content | SHA-256 |
| --- | --- | --- | --- |
| `camera-api.aar` | `plugin-api/camera-api` | `ICameraPlugin`, `ICameraSession` and the three callback AIDL interfaces, `CameraContract`, `CameraOptionKeys`, `CameraResultKeys`, `CameraFrameKeys`, `CameraLimits`, `CameraErrorCodes`, `CameraCapabilityKeys`, `CameraPluginActions`, `CameraPluginIds` | `bd3d074a958de5a3a5f188136174951c94c764e984e693b3622c573987ff3a63` |
| `common-plugin-api.aar` | `plugin-api/common-plugin-api` | `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`, `AutoJs6HostSettingsContract` | `9849b15091dcb255cd1c47140790081eebc074aec2e3e9b28c18f12bf9ef4a6a` |

Provenance (2026-11-05, roadmap P2.1): both AARs are `assembleRelease` outputs of one host commit, master
`2a5bafc67a`, built in the isolated clone `E:/.cache/autojs6-prism-host` (its `version.properties` set to
`VERSION_BUILD=6641`, not committed; appendix D Q9). `camera-api.aar` is the camera contract module of roadmap P1.1
(the only host commit that touches `plugin-api/camera-api` is `4a10154c44`); `common-plugin-api.aar` is restaged from
the same commit because `camera-api` refers to its `PluginInfo` parcelable. The P0 copy (13220 bytes, host 6.8.0
build 5697) is superseded.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.

To update: assemble `:plugin-api:<module>:assembleRelease` in the host, copy the AAR here, record its
lowercase SHA-256 in the lock and in this table, and list it in `THIRD_PARTY_NOTICES.md`.
