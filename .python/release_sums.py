# -*- coding: utf-8 -*-
"""Check the release APKs of 3-Prism Camera and write their SHA256SUMS (roadmap D22, R4, appendix D Q8 = (b)).

A release is one APK per ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus the universal APK. Every APK must carry exactly
the whitelisted CameraX and Compose native helpers of its ABIs (three for a single-ABI APK, four ABIs x three for the
universal one), stored uncompressed, with the zip data offset and every ELF PT_LOAD alignment at 16 KB or more; the
same rule appendDigestToReleasedFiles enforces locally. CI uses --artifacts on the unsigned assembleRelease outputs.

`.\\gradlew.bat :app:appendDigestToReleasedFiles` copies the signed APKs to releases/ with a CRC32 suffix
(`autojs6-plugin-three-prism-camera-v<version>-<abi>-<crc32>.apk`, `universal` for the universal APK); several builds of
one version may sit there side by side. The default mode takes, for each split, the newest APK of the version in
version.properties, checks that the CRC32 suffix matches the file and the native libraries follow the rule above, and
writes releases/SHA256SUMS in the format of `sha256sum` (two spaces, file names sorted). Nothing is signed and no secret
is read.

Usage:
  py .python/release_sums.py                     check the newest release set and write releases/SHA256SUMS
  py .python/release_sums.py --check             compare releases/SHA256SUMS with the files (writes nothing)
  py .python/release_sums.py --artifacts <dir>   check the five APKs of a build output folder (no CRC32 suffix, no sums)

Exit protocol: prints RELEASE_SUMS_OK (or RELEASE_OK with --artifacts) on success, RELEASE_SUMS_ERROR <reason> on
failure (exit code 1).
"""
from __future__ import annotations

import argparse
import hashlib
import re
import struct
import sys
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RELEASES = ROOT / "releases"
PREFIX = "autojs6-plugin-three-prism-camera"
# Roadmap D22: kept equal to nativeAbis / allowedNativeLibraries of app/build.gradle.kts by check_repository.py.
NATIVE_ABIS = ["arm64-v8a", "armeabi-v7a", "x86_64", "x86"]
NATIVE_LIBRARIES = ["libandroidx.graphics.path.so", "libimage_processing_util_jni.so", "libsurface_util_jni.so"]
SPLITS = NATIVE_ABIS + ["universal"]
PAGE = 16384


class SumsError(Exception):
    pass


def version_name():
    match = re.search(r"^VERSION_NAME=(.+)$", (ROOT / "version.properties").read_text(encoding="utf-8"), re.M)
    if not match:
        raise SumsError("VERSION_NAME is missing from version.properties")
    return match.group(1).strip()


def crc32(path):
    value = 0
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            value = zlib.crc32(chunk, value)
    return f"{value:08x}"


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def load_alignments(data):
    """The p_align values of the PT_LOAD segments of an ELF file (32- or 64-bit, little endian)."""
    if data[:4] != b"\x7fELF":
        return []
    is64 = data[4] == 2
    phoff = struct.unpack_from("<Q" if is64 else "<I", data, 0x20 if is64 else 0x1C)[0]
    size, count = struct.unpack_from("<HH", data, 0x36 if is64 else 0x2A)
    return [struct.unpack_from("<Q" if is64 else "<I", data, phoff + i * size + (48 if is64 else 28))[0]
            for i in range(count) if struct.unpack_from("<I", data, phoff + i * size)[0] == 1]


def check_native(apk, split):
    """Problems of one APK against roadmap D22; an empty list means the APK passes. Returns (problems, library count)."""
    abis = NATIVE_ABIS if split == "universal" else [split]
    expected = {f"lib/{abi}/{name}" for abi in abis for name in NATIVE_LIBRARIES}
    problems = []
    with zipfile.ZipFile(apk) as archive, open(apk, "rb") as raw:
        natives = [info for info in archive.infolist() if info.filename.startswith("lib/") or info.filename.endswith(".so")]
        found = {info.filename for info in natives}
        if found != expected:
            problems.append(f"native set: missing {sorted(expected - found)}, unexpected {sorted(found - expected)}")
        for info in natives:
            raw.seek(info.header_offset)
            header = raw.read(30)
            offset = info.header_offset + 30 + struct.unpack_from("<H", header, 26)[0] + struct.unpack_from("<H", header, 28)[0]
            aligns = load_alignments(archive.read(info))
            if info.compress_type != zipfile.ZIP_STORED or offset % PAGE or not aligns or min(aligns) < PAGE:
                problems.append(f"{info.filename}: method {info.compress_type}, offset {offset}, PT_LOAD {aligns}")
    return problems, len(natives)


def newest_apks(version):
    chosen = {}
    for split in SPLITS:
        pattern = re.compile(rf"{re.escape(PREFIX)}-v{re.escape(version)}-{re.escape(split)}-([0-9a-f]{{8}})\.apk")
        candidates = [p for p in RELEASES.glob("*.apk") if pattern.fullmatch(p.name)]
        if not candidates:
            raise SumsError(f"No {split} APK of v{version} in releases/ (run appendDigestToReleasedFiles)")
        chosen[split] = max(candidates, key=lambda p: p.stat().st_mtime)
    return chosen


def artifact_apks(folder):
    apks = sorted(Path(folder).glob("*.apk"))
    chosen = {}
    for split in SPLITS:
        matches = [p for p in apks if p.name.endswith(f"-{split}.apk")]
        if len(matches) != 1:
            raise SumsError(f"expected one {split} APK in {folder}, found {[p.name for p in matches]}")
        chosen[split] = matches[0]
    if len(apks) != len(SPLITS):
        raise SumsError(f"expected exactly {len(SPLITS)} APKs in {folder}, found {[p.name for p in apks]}")
    return chosen


def verify(apks, with_crc):
    for split, apk in apks.items():
        if with_crc and crc32(apk) != apk.stem.rsplit("-", 1)[1]:
            raise SumsError(f"{apk.name}: the CRC32 suffix does not match the file")
        problems, count = check_native(apk, split)
        if problems:
            raise SumsError(f"{apk.name}: " + "; ".join(problems))
        print(f"  {apk.name}: {apk.stat().st_size} bytes, {count} native libraries stored and 16 KB aligned")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true", help="compare releases/SHA256SUMS instead of writing it")
    mode.add_argument("--artifacts", metavar="DIR", help="check the five APKs of a build output folder")
    arguments = parser.parse_args(argv)
    try:
        if arguments.artifacts:
            verify(artifact_apks(arguments.artifacts), with_crc=False)
            print("RELEASE_OK")
            return 0
        apks = newest_apks(version_name())
        verify(apks, with_crc=True)
        lines = "".join(f"{sha256(p)}  {p.name}\n" for p in sorted(apks.values(), key=lambda p: p.name))
        target = RELEASES / "SHA256SUMS"
        if arguments.check:
            if not target.is_file() or target.read_text(encoding="ascii") != lines:
                raise SumsError("releases/SHA256SUMS differs from the release files")
        else:
            target.write_text(lines, encoding="ascii", newline="\n")
    except (SumsError, OSError, zipfile.BadZipFile) as error:
        print(f"RELEASE_SUMS_ERROR {error}")
        return 1
    print("RELEASE_SUMS_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
