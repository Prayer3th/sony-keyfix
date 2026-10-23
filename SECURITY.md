# Security and privacy

The app connects only to `127.0.0.1:5555`. It has no cloud, telemetry, advertising
or uploaded event log. It generates an RSA ADB identity on the TV and keeps it in
private app storage; Android app backup is disabled. Same-signature updates keep
the identity. Clearing data or uninstalling removes it; the TV may retain its
public authorization record.

ADB authorization grants shell privileges. Current code uses them only for CPU
ABI detection, device discovery, raw input reads and three fixed key codes.
Device identity and paths are validated. There is no arbitrary-command UI or
exported shell service. The activity is exported for the launcher; it does not
automatically start repair or accept commands from incoming intents. The service
and startup receiver are not exported.

Permissions: INTERNET for loopback ADB, FOREGROUND_SERVICE for background work,
and RECEIVE_BOOT_COMPLETED for opt-in startup. Enabling TCP ADB on the TV can
expose its daemon to the LAN even though this app only uses loopback. Use trusted
networks and never forward ADB to the internet. The app does not listen on a
network socket or change debugging settings.

Android 9 and target SDK 28 are intentional constraints for this firmware. This
is a sideloaded utility, not a claim of current Play Store eligibility or support
for modern wireless-debugging pairing.

For vulnerabilities, use GitHub private vulnerability reporting if the owner has
enabled it. Otherwise ask for a private reporting channel without exposing details
in public. No security audit certification or response-time commitment is implied.
