# Contributing

Small fixes and hardware evidence are welcome. Open an issue before changing the
compatibility gate, input record width or mappings: they depend on firmware.

Include TV model, Android version, remote model, app version, Bluetooth/IR mode,
steps and expected/actual behavior. For menu tests, name the app where the original
remote works. Redact device addresses, serial numbers and account information.
Never upload ADB identities or signing keys.

Run `python3 build.py --test-only` and `python3 build.py`. Changes to input parsing,
service recovery or ADB need focused regression tests. Distinguish JVM testing
from physical-TV verification. Preserve package identity and preferences for upgrades.

Keep Chinese and English strings in sync. Use normal release wording in the UI
and document actual compatibility limits and gaps. Project contributions follow
the MIT license; retain BSD-3-Clause notices for vendored AdbLib changes.
