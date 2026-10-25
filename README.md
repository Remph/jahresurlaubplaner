# Urlaubsplaner

A personal, non-profit Android app for planning the year's vacation in Germany: public holidays,
school holidays, vacation balance and bridge days, status first. Local-first: the data stays on
the phone. German first, English second.

Status: skeleton only, the app shows an empty screen. The plan is in [docs/plan.md](docs/plan.md),
the project rules in [CLAUDE.md](CLAUDE.md).

## Build

You need JDK 17 or newer and the Android SDK (platform 37; set `ANDROID_HOME` or
`local.properties`). The Gradle wrapper is committed.

```
./gradlew lint test assembleDebug spotlessCheck   # the standard check
./gradlew spotlessApply                           # fix formatting
./gradlew -PcoreOnly :core:test                   # only the rules module, no Android SDK needed
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Layout

- `core/`: plain Kotlin/JVM, all rules (holidays, balance, bridge days), unit-tested.
- `app/`: the Android app (Jetpack Compose, Material 3), depends on `core`.

## Licence

GPL-3.0, see [LICENSE](LICENSE). The bundled school holiday data comes from OpenHolidays under
the ODbL and stays in its own directory (`data/school-holidays/`) with its own licence text, see
[docs/data-sources.md](docs/data-sources.md).
