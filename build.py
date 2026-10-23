#!/usr/bin/env python3
"""Offline build/test driver. Requires Python 3.9+, Java 17 and Android SDK.

No downloads, installation or device access. Unsigned by default; --sign requires
an explicit keystore and environment-based credentials. See docs/BUILDING.md.
"""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent
BUILD = ROOT / "build"
DIST = ROOT / "dist"
NS = "{http://schemas.android.com/apk/res/android}"


def run(*args):
    subprocess.run([str(arg) for arg in args], check=True, cwd=ROOT, timeout=180)


def required(path, label):
    if not path.is_file():
        raise SystemExit(f"Missing {label}: {path}. See docs/BUILDING.md.")
    return path


def java_tools():
    home = os.environ.get("KEYFIX_JAVA_HOME") or os.environ.get("JAVA_HOME")
    suffix = ".exe" if os.name == "nt" else ""
    java = Path(home) / "bin" / ("java" + suffix) if home else Path(shutil.which("java") or "missing-java")
    required(java, "Java 17")
    ecj = os.environ.get("KEYFIX_ECJ_JAR")
    if ecj:
        compiler = [java, "-jar", required(Path(ecj), "ECJ compiler"), "-8", "-encoding", "UTF-8"]
    else:
        javac = Path(home) / "bin" / ("javac" + suffix) if home else Path(shutil.which("javac") or "missing-javac")
        compiler = [required(javac, "JDK javac"), "-source", "8", "-target", "8", "-encoding", "UTF-8"]
    return java, compiler


def test(java, compiler):
    target = BUILD / "tests"
    target.mkdir(parents=True)
    library = sorted((ROOT / "src/com/cgutman/adblib").glob("*.java"))
    pure = [ROOT / f"src/local/sony/keyfix/{name}.java" for name in
            ("EventDecoder", "KeyMapping", "RemoteDevice", "StartupPolicy", "ShellResult")]
    run(*compiler, "-d", target, *library, *pure, *sorted((ROOT / "tests").glob("*.java")))
    for main in ("EventDecoderTest", "CoreTest", "com.cgutman.adblib.AdbProtocolTest", "com.cgutman.adblib.AdbSmokeTest"):
        run(java, "-cp", target, main)


def archive_classes():
    with zipfile.ZipFile(BUILD / "classes.jar", "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((BUILD / "classes").rglob("*.class")):
            archive.write(path, path.relative_to(BUILD / "classes").as_posix())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--test-only", action="store_true", help="Run JVM tests; no Android SDK needed")
    parser.add_argument("--sign", action="store_true", help="Sign with explicit KEYFIX_KEYSTORE settings")
    args = parser.parse_args()
    if args.test_only and args.sign:
        parser.error("--test-only cannot be combined with --sign")
    java, compiler = java_tools()
    # Only generated directories inside this repository are ever removed.
    if BUILD.is_symlink() or DIST.is_symlink():
        raise SystemExit("build/ and dist/ must not be symbolic links")
    if BUILD.exists():
        shutil.rmtree(BUILD)
    BUILD.mkdir()
    test(java, compiler)
    if args.test_only:
        return

    sdk_value = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    sdk = Path(sdk_value) if sdk_value else ROOT / "missing-android-sdk"
    sdk_tools = Path(os.environ.get("KEYFIX_BUILD_TOOLS", str(sdk / "build-tools/35.0.1")))
    android = required(Path(os.environ.get("KEYFIX_ANDROID_JAR", str(sdk / "platforms/android-28/android.jar"))), "Android API 28")
    suffix = ".exe" if os.name == "nt" else ""
    aapt = required(sdk_tools / ("aapt2" + suffix), "aapt2")
    align = required(sdk_tools / ("zipalign" + suffix), "zipalign")
    d8 = required(sdk_tools / "lib/d8.jar", "D8")
    signer = required(sdk_tools / "lib/apksigner.jar", "apksigner")
    manifest = ET.parse(ROOT / "AndroidManifest.xml").getroot()
    version = manifest.attrib[NS + "versionName"]
    if not all(c in "0123456789." for c in version):
        raise SystemExit("Release version must contain only digits and dots")
    for name in ("generated", "classes", "dex"):
        (BUILD / name).mkdir()
    run(aapt, "compile", "--dir", ROOT / "res", "-o", BUILD / "resources.zip")
    run(aapt, "link", "-I", android, "--manifest", ROOT / "AndroidManifest.xml",
        "--java", BUILD / "generated", "-o", BUILD / "resources.apk", BUILD / "resources.zip")
    sources = sorted((ROOT / "src").rglob("*.java")) + sorted((BUILD / "generated").rglob("*.java"))
    run(*compiler, "-classpath", android, "-d", BUILD / "classes", *sources)
    archive_classes()
    run(java, "-cp", d8, "com.android.tools.r8.D8", "--min-api", "26", "--lib", android,
        "--output", BUILD / "dex", BUILD / "classes.jar")
    shutil.copyfile(BUILD / "resources.apk", BUILD / "unsigned.apk")
    with zipfile.ZipFile(BUILD / "unsigned.apk", "a", zipfile.ZIP_DEFLATED) as archive:
        for dex in sorted((BUILD / "dex").glob("*.dex")):
            archive.write(dex, dex.name)
        for name in ("LICENSE.txt", "AdbLib-LICENSE.txt", "NOTICE.txt"):
            archive.write(ROOT / name, "assets/" + name)
    run(align, "-f", "4", BUILD / "unsigned.apk", BUILD / "aligned.apk")
    DIST.mkdir(exist_ok=True)
    artifact = DIST / f"Sony-Keyfix-{version}{'' if args.sign else '-unsigned'}.apk"
    if args.sign:
        for env in ("KEYFIX_KEYSTORE", "KEYFIX_KEY_ALIAS", "KEYFIX_STORE_PASSWORD", "KEYFIX_KEY_PASSWORD"):
            if not os.environ.get(env):
                raise SystemExit(f"--sign requires {env}; unsigned result remains in build/aligned.apk")
        keystore = required(Path(os.environ["KEYFIX_KEYSTORE"]), "release signing keystore")
        run(java, "-jar", signer, "sign", "--ks", keystore, "--ks-key-alias", os.environ["KEYFIX_KEY_ALIAS"],
            "--ks-pass", "env:KEYFIX_STORE_PASSWORD", "--key-pass", "env:KEYFIX_KEY_PASSWORD",
            "--v4-signing-enabled", "false", "--out", artifact, BUILD / "aligned.apk")
        run(java, "-jar", signer, "verify", "--verbose", "--print-certs", artifact)
    else:
        shutil.copyfile(BUILD / "aligned.apk", artifact)
    run(align, "-c", "4", artifact)
    digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
    artifact.with_suffix(".apk.sha256").write_text(f"{digest}  {artifact.name}\n", encoding="utf-8")
    print(f"Built: {artifact}")


if __name__ == "__main__":
    try:
        main()
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
        print(f"Build failed: {error}", file=sys.stderr)
        sys.exit(1)
