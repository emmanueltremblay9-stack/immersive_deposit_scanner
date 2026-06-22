# Changelog

## 1.0.20 - 2026-06-22

- Hardened saved deposit loading so malformed keys, sources, kinds, partial sample positions, wrong-typed amounts, and invalid percentages cannot become bogus deposits.
- Preserved live client upserts and removals that arrive while a full sync is in progress.
- Guarded JourneyMap marker, overlay, waypoint, and cleanup calls so API failures do not break local cache cleanup.
- Ignored faulty Immersive Petroleum reservoir survey results with empty fluid data.
- Strengthened `install-mod.ps1` to verify jar identity from the primary mod metadata block, required dependency declarations, installed dependency jars, SHA-256 equality, and exact one-jar replacement.

Verification for this release candidate:

- `.\gradlew.bat qaDataModel`
- `.\gradlew.bat check`
- `.\gradlew.bat clean build`
- `.\gradlew.bat runGameTestServer`
- `.\install-mod.ps1 -ModsDir 'C:\Users\Emmanuel Tremblay\AppData\Roaming\PrismLauncher\instances\1.21.1 TesT LaB\minecraft\mods'`
- Post-install `.\gradlew.bat runGameTestServer`
