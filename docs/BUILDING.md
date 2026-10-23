# Building Sony Keyfix

A small offline Python driver builds the app without Gradle. AdbLib source is
vendored; there are no Maven application dependencies. Run commands from the
repository root. The build never downloads tools, accesses a TV or installs an APK.

## Toolchain

- Python 3.9+.
- JDK 17, with `JAVA_HOME` set or `java` and `javac` on PATH.
- Android SDK Platform **28**, Build Tools **35.0.1**; set `ANDROID_HOME`.

With the official Android command-line tools installed:

```sh
sdkmanager "platforms;android-28" "build-tools;35.0.1"
python3 build.py --test-only
python3 build.py
```

Review SDK license prompts when installing tools. The output is
`dist/Sony-Keyfix-1.0.0-unsigned.apk` with a SHA-256 file. It must be signed before
installation. Each run cleans `build/`; `dist/` may retain earlier artifacts, so
use the exact output path printed by the successful build.

The release was built locally on macOS Apple Silicon with Java 17 + ECJ 3.33.0.
The Linux JDK 17 build also passed on GitHub Actions; see the
[verified run](https://github.com/Prayer3th/sony-keyfix/actions/runs/40000312491).
Windows paths are handled but Windows builds are unverified.

## Alternate locations / JRE + ECJ

| Environment variable | Override |
| --- | --- |
| `KEYFIX_JAVA_HOME` | Java 17 home, ahead of `JAVA_HOME` |
| `KEYFIX_BUILD_TOOLS` | Directory containing aapt2, zipalign and lib/ |
| `KEYFIX_ANDROID_JAR` | API 28 android.jar |
| `KEYFIX_ECJ_JAR` | Optional ECJ 3.33.0 JAR, permitting a JRE without javac |

The optional compiler is `org.eclipse.jdt:ecj:3.33.0` from
[Maven Central](https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.33.0/).
SDKs and downloaded tools are not redistributed with the source.

## Signing

Keep the signing keystore outside the repository and back it up privately.
Updates require the same key. Never commit private keys or passwords.

For your own new distribution, generate a key once (keytool prompts for identity
and credentials):

```sh
keytool -genkeypair -keystore /private/location/sony-keyfix.p12 \
  -storetype PKCS12 -alias release -keyalg RSA -keysize 3072 -validity 10000
```

Set `KEYFIX_KEYSTORE`, `KEYFIX_KEY_ALIAS`, `KEYFIX_STORE_PASSWORD` and
`KEYFIX_KEY_PASSWORD` in your local environment or secret manager, then run:

```sh
python3 build.py --sign
```

The script uses password environment references, aligns before signing, verifies
the APK and prints its certificate fingerprint. Output:
`dist/Sony-Keyfix-1.0.0.apk`. No shared debug key or release credentials are shipped.
Reference: [Android apksigner](https://developer.android.com/tools/apksigner).

## Publication

1. Set `versionName` and increment `versionCode` in AndroidManifest.xml.
2. Update CHANGELOG, filenames in the docs and the compatibility record.
3. Build and sign with the intended release identity.
4. Verify upgrade, four keys, voice, stop/start, reboot and operation without the
   computer on supported hardware. Record untested items honestly.
5. Run `python3 tools/package_source.py` and inspect the archive listing. It uses
   an explicit allowlist that excludes build outputs and secrets.
6. Create the Git tag and GitHub Release manually, attaching the signed APK,
   source ZIP and SHA-256 files.

CI has read-only repository permissions and no signing or publication step.
Its artifact is **unsigned**. Build success does not imply hardware compatibility
or byte-for-byte reproducibility.
