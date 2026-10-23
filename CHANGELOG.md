# Changelog

## 1.0.0 — 2026-10-24

First public-release source layout, following the working local installation.

- Retain the four verified mappings: Input, Quick settings and two standard menus.
- Replace development wording with a Chinese/English TV UI, clear state, help
  and bundled open-source license viewer.
- Preserve package ID, preferences and ADB identity for same-key upgrades;
  resume enabled repair after package replacement.
- Bound pending key actions and ADB buffers; prevent older sessions affecting
  replacement connections; validate packet bounds and shell completion.
- Handle lost input synchronization conservatively.
- Add portable offline builds, explicit signing, regression tests, CI, source
  packaging, setup instructions and third-party attribution.

Hardware coverage and remaining test gaps: docs/COMPATIBILITY.md.
