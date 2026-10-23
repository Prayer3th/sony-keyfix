# Sony Keyfix

Repair four Bluetooth buttons on **Sony X9500H + RMF-TX910C** without root, paid software or an always-on computer.

[Download](https://github.com/Prayer3th/sony-keyfix/releases/latest) · [中文说明](README.md) · [Build instructions](docs/BUILDING.md) · [Security](SECURITY.md)

An independent community project, not affiliated with Sony.

| Button | Action |
| --- | --- |
| Input | Input selection |
| Gear | Sony quick settings |
| Options / Three-line menu | Standard menu in the current app |

Menu actions require an app that supports them; the home screen may do nothing. Navigation, volume and voice retain their normal system behavior. The TV button is not remapped.

## Compatibility and setup

The tested combination is X9500H, Android 9, **32-bit ARM userspace**, and RMF-TX910C (`054c:0eae`, Bluetooth). Other firmware and models are unverified. The service refuses other Android versions and userspace formats.

The TV must already expose authorized ADB on TCP port 5555, permit loopback connections, and allow ADB shell to read its input device. The app cannot enable debugging or bypass authorization. Keep network debugging on a trusted network; never forward it to the internet.

Install a signed APK, open Sony Keyfix, and select **Start**. Accept the TV debugging prompt with **Always allow**. Optionally select **Resume after TV restart**. You can close the app screen; the service continues on the TV. **Stop** persists across reboots and upgrades. Uninstalling removes the service and its private keys; the TV may retain the public authorization record.

The baseline installation was tested with the computer disconnected: all four buttons and voice worked. Normal reboot recovery was confirmed for Input and Gear. See [validation details](docs/COMPATIBILITY.md) for release checks and remaining limits.

## Design and build

The app reads 16-byte Linux input events over its authorized loopback ADB connection, then injects only three fixed key codes: 178, 189 and 82. It does not grab the input device or modify system files. It uses no cloud, analytics or accessibility service.

With Python 3.9+, JDK 17, Android Platform 28 and Build Tools 35.0.1:

```sh
python3 build.py --test-only
python3 build.py
```

The default APK is unsigned. Signing requires an explicit private keystore and environment credentials; no signing key is distributed. See [BUILDING](docs/BUILDING.md). CI tests and builds an unsigned artifact; it does not publish a release.

## License

Project: [MIT](LICENSE.txt). Vendored [AdbLib](https://github.com/cgutman/AdbLib): [BSD-3-Clause](AdbLib-LICENSE.txt). Pinned revision and local changes: [NOTICE](NOTICE.txt). Both license texts are included in the APK under **About → Open source licenses**. No Sony firmware or proprietary binaries are included.
