# Plan: skeleton and milestones

Agreed with Nick on 2026-10-25. Scope is the MVP in `CLAUDE.md`; nothing beyond it unless Nick asks.

## Layout

```
settings.gradle.kts         includes :core always, :app unless -PcoreOnly
build.gradle.kts            Spotless only; no Android plugins (so -PcoreOnly never resolves them)
gradle/libs.versions.toml   the only place with versions
core/                       plain Kotlin/JVM, no Android imports: all rules
app/                        Android: Compose, Room, widget; depends on :core
data/school-holidays/       later: yearly ODbL file with the licence text, own directory
```

Planned packages in `:core` (`de.remph.urlaubsplaner.core`): `holidays` (Easter rule, Länder
tables), `balance` (entitlement, Resturlaub expiry, half days), `bridge` (Brückentage), `backup`
(versioned JSON format), `ics`.

Rules:

- `:core` takes a `java.time.Clock` wherever it needs "today". No `now()` in logic.
- `:app` holds screens, Room and glue. Every rule lives in `:core` and has a unit test there.
- Room schemas are exported to `app/schemas/` and committed; every change gets a migration test.
- Every visible text is a string resource in `values/` (German) and `values-en/` (English).

## Build settings

- minSdk 30 (Nick, 2026-10-25), compileSdk 37 and targetSdk 37. compileSdk 37 is not a choice:
  the current AndroidX releases require it.
- Bytecode level 17, so any JDK from 17 up builds it. The cloud container has JDK 21. JDK 25 is
  not used: no statement found that the Android Gradle Plugin supports it (2026-10-25).
- Versions (latest stable on 2026-10-25): Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21, Compose BOM
  2026.09.00, activity-compose 1.13.0, JUnit 6.1.3, Spotless 8.10.4 (ktlint).
- Well-known, maintained libraries may be added when they make development easier; name them in
  the PR and ask first when unsure.
- Widget later with Jetpack Glance (AndroidX, no Google Play Services, fine for F-Droid).

## Milestones

| # | Milestone | Contents | PRs (each with a review) |
| --- | --- | --- | --- |
| M0 | Skeleton | `LICENSE`, Gradle setup, `:core` with a smoke test, `:app` with an empty screen in German and English, Spotless, README, this plan | 1 |
| M1 | Rules in `:core` | Public holidays for all 16 Länder (Easter rule plus tables, regional cases noted); vacation balance (entitlement, Resturlaub expiry, half days); Brückentage | 3 |
| M2 | Data and storage | Room (settings, marked days, entitlement); school holiday file and loader (ODbL); backup export/import | 3 |
| M3 | Screens | Status home (remaining days, next trip), year and month view, marking single and half days, settings (Bundesland, language), Brückentage in the view | 2 to 3 |
| M4 | Export and widget | ICS export (with the ODbL credit), home screen widget; both follow the chosen app language | 2 |
| M5 | Release prep | About screen with credits, icon, F-Droid metadata, signing; the Google Play decision comes up here | 1 to 2 |

M1 comes first after M0 because it is testable in the cloud and everything else depends on it.

Language (Nick, 2026-10-25): the app language is a setting, not only the system language: System
(default), Deutsch, English. If the system language is neither, German applies (the default
resources). Plan: `androidx.appcompat` `AppCompatDelegate.setApplicationLocales()` (works back to
Android 11), `autoStoreLocales` so it is remembered below Android 13, and a `localeConfig` so
Android 13+ lists the app in the system's per-app language settings. To check when building:
whether the activity must extend `AppCompatActivity`, and how the widget and ICS export get the
chosen language below Android 13 (they do not run inside the activity).

Known snags:

- School holiday data (M2): the cloud cannot reach openholidaysapi.org unless Nick adds it to the
  trusted sites; otherwise Nick exports the JSON once on his PC.
- Instrumented tests and the widget (M3, M4) need an emulator or Nick's phone.
- M5: the app has no launcher icon yet (lint `MissingApplicationIcon`).

## Status

- [x] M0 Skeleton (PR #3, merged 2026-10-25)
- [ ] M1 Rules in `:core`
- [ ] M2 Data and storage
- [ ] M3 Screens
- [ ] M4 Export and widget
- [ ] M5 Release prep
