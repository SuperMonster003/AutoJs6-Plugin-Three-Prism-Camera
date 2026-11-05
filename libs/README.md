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
| `common-plugin-api.aar` | `plugin-api/common-plugin-api` | `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`, `AutoJs6HostSettingsContract` | `9eaba785db6128f9a0ded272ab976f37835e9fa17a49cc250a6a75ddf76cf769` |

Provenance (2026-11-05): byte-identical to the artifact 3-Chime Alarm and 3-Stamp Mail stage, a release
build of the host 6.8.0 (build 5697). P0 only needs `PluginInfo`, `IPluginInfoProvider` and the capability
keys for the INFO service. The camera contract `camera-api.aar` joins in roadmap P2.1, built from the host
commit that introduces `plugin-api/camera-api` (roadmap P1.1); `common-plugin-api.aar` is then restaged from
the same commit because `camera-api` refers to its `PluginInfo` parcelable.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.

To update: assemble `:plugin-api:<module>:assembleRelease` in the host, copy the AAR here, record its
lowercase SHA-256 in the lock and in this table, and list it in `THIRD_PARTY_NOTICES.md`.
