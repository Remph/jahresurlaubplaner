# Urlaubsplaner

A personal, non-profit Android app for planning the year's vacation in Germany: public holidays,
school holidays, vacation balance and bridge days, status first. Local-first: the data stays on
the phone. German first, English second.

Status: the rules, storage, screens (Übersicht, Kalender, Einstellungen), ICS export, backup and the
home screen widget are built; the first public release is 0.9.0 (release preparation is under way). The plan is in
[docs/plan.md](docs/plan.md), the project rules in [CLAUDE.md](CLAUDE.md).

## Build

You need JDK 17 or newer and the Android SDK (platform 37; set `ANDROID_HOME` or
`local.properties`). The Gradle wrapper is committed.

```
./gradlew lint test assembleDebug spotlessCheck   # the standard check
./gradlew spotlessApply                           # fix formatting
./gradlew -PcoreOnly :core:test                   # only the rules module, no Android SDK needed
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Install

- **F-Droid:** planned; the app is not in the F-Droid repository yet.
- **GitHub:** a signed APK is attached to a release when there is one (releases only when the
  maintainer decides). Check the SHA-256 fingerprint of the signing key against
  [docs/release.md](docs/release.md) before installing. The F-Droid and GitHub builds are signed
  with different keys, so switching from one to the other means uninstalling first (back up
  through Einstellungen > Datensicherung).
- Bugs and wishes: [GitHub issues](https://github.com/Remph/jahresurlaubplaner/issues). There is no
  contact e-mail.

How a release is made, the signing key and the F-Droid steps are in [docs/release.md](docs/release.md).
The F-Droid texts, icon and screenshots are in `fastlane/metadata/android/`.

## Layout

- `core/`: plain Kotlin/JVM, all rules (holidays, school holidays, balance, bridge days, backup and
  ICS format, widget status), unit-tested.
- `app/`: the Android app (Jetpack Compose, Material 3, Room, Glance widget), depends on `core`.
- `fastlane/metadata/android/`: store texts, 512 px icon and screenshots (German and English).
- `tools/`: helper scripts (school holiday export, store icon).

## Licence

GPL-3.0, see [LICENSE](LICENSE). The bundled school holiday data comes from OpenHolidays under
the ODbL and stays in its own directory (`data/school-holidays/`) with its own licence text, see
[docs/data-sources.md](docs/data-sources.md).
