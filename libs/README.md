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
| `camera-api.aar` | `plugin-api/camera-api` | `ICameraPlugin`, `ICameraSession` and the three callback AIDL interfaces, `CameraContract`, `CameraOptionKeys`, `CameraResultKeys`, `CameraFrameKeys`, `CameraLimits`, `CameraErrorCodes`, `CameraCapabilityKeys`, `CameraPluginActions`, `CameraPluginIds` | `9063b4a9019b896119f5688d5d2b0142d240737869fa534102079706cf1a1200` |
| `common-plugin-api.aar` | `plugin-api/common-plugin-api` | `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`, `AutoJs6HostSettingsContract` | `9849b15091dcb255cd1c47140790081eebc074aec2e3e9b28c18f12bf9ef4a6a` |

Provenance (roadmap P2.1): both AARs are `assembleRelease` outputs of the host. `camera-api.aar` is the camera
contract module of roadmap P1.1, restaged on 2026-11-06 from host master `c82c5b27f9`, which adds
`CameraResultKeys.STATE_PREVIEW_MIRRORED` (appendix D Q11 = (b)); the host commits that touch `plugin-api/camera-api`
are `4a10154c44` and `c82c5b27f9`. `common-plugin-api.aar` is the 2026-11-05 build of host master `2a5bafc67a` in the
isolated clone `E:/.cache/autojs6-prism-host` (its `version.properties` set to `VERSION_BUILD=6641`, not committed;
appendix D Q9), restaged because `camera-api` refers to its `PluginInfo` parcelable; nothing in it changed since. The
P0 copy (13220 bytes, host 6.8.0 build 5697) is superseded.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.

To update: assemble `:plugin-api:<module>:assembleRelease` in the host, copy the AAR here, record its
lowercase SHA-256 in the lock and in this table, and list it in `THIRD_PARTY_NOTICES.md`.
