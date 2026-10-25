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
| M3 | Screens | Status home (remaining days, next trip), year and month view, marking single and half days, settings (Bundesland, language, regional holiday opt-ins and the Mariä Himmelfahrt opt-out for Bayern), Brückentage in the view | 2 to 3 |
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

Public holidays (M1, PR #5): `PublicHolidays.of(land, year, extras)` supports 2019 and later (earlier
years differ). Mariä Himmelfahrt counts for all of Bayern (Nick, 2026-10-25); Augsburger
Friedensfest and Fronleichnam in parts of Sachsen/Thüringen are opt-in `RegionalExtra`s. Berlin
one-offs (8 May 2020 and 2025, 17 June 2028) are in the table. Each year: check the Länder laws for
new or changed holidays and extend `HolidayRules`.

Vacation balance (M1, second piece): `VacationCalculator(clock, schedule, isPublicHoliday)` in
`core.balance`. Decided with Nick on 2026-10-25:

- Amounts are `VacationDays`, counted in half days. A marked day is a full or a half day
  (`DayMark`); it costs vacation only on a working day that is not a public holiday. Working
  weekdays are a setting (Monday to Friday by default).
- Heiligabend and Silvester are full working days by default. `WorkSchedule.specialDays` can set
  either (or any other date) to a half working day or a day off like a holiday. A half working day
  caps the cost at half a day, even for a full mark.
- Public holidays come in as a function `(LocalDate) -> Boolean`, so regional extras and the
  Bayern opt-out reach the balance without changing it.
- Resturlaub is typed in per year (the employer's figure), not computed. It is used first by days
  up to and including the expiry date (31 March by default, configurable) and lost the day after.
- Marked days up to and including today (from the `Clock`) are taken, later ones are planned.
  Remaining days can go negative.
- Not built: pro-rata entitlement in the first year, and the rule that Resturlaub only lapses when
  the employer has told the employee about it.

Brückentage (M1, third piece): `BridgeFinder(clock, schedule, isPublicHoliday)` in `core.bridge`.
Decided with Nick on 2026-10-25:

- A bridge is a whole gap of days that cost vacation between two free stretches (weekend, holiday,
  day off in the schedule, marked day) that costs at most 2 days by default (`maxCost`, half days
  counted). The joined stretch must hold a public holiday or special day off on a weekday the user
  works, so plain weekends and holidays on never-worked days give no bridge. Works with a 4-day week.
- Days already marked count as free time and cost nothing more; a half marked day still leaves
  half a day to take.
- `find(year, marks, maxCost)` lists the year's bridges by date (a bridge belongs to the year of its
  first day to take, the stretch may cross New Year); `best(year, 3, ...)` returns the three with
  the most days off per vacation day from tomorrow on.
- Use years after 2019 with `PublicHolidays`: a stretch at the start of January looks at the
  last days of the year before, which that class does not know for 2018.
- Not built: combining several gaps, ICS, screens.

Opt-out for Mariä Himmelfahrt in Bayern (Nick, 2026-10-25): the holiday is counted for all of
Bayern by default, but some communities do not have it, so there is a setting to switch it off.
It sits in the M3 settings next to the regional opt-ins. Built in `:core` (M1, last piece):
`RegionalOptOut.MARIAE_HIMMELFAHRT_BAYERN`, passed as `optOuts` next to `extras` to
`PublicHolidays.of` and `isHoliday`. It only affects Bayern (Saarland has the holiday everywhere)
and the default is unchanged. `VacationCalculator` and `BridgeFinder` needed no change; they read
the holidays through the function the caller builds, and a test runs both through it with the
opt-out. M3 stores the set like the extras and builds that function from Land, extras and opt-outs
for marked days, the balance and Brückentage alike. Not built: Room, screen.

Storage (M2, first piece): Room in `:app`, package `de.remph.urlaubsplaner.data`, database
`UrlaubsplanerDatabase` (version 1). Design approved by Nick on 2026-10-25:

- Three tables: `settings` (one row: Land code, working weekdays, regional extras, opt-outs, and the
  special days like Heiligabend/Silvester as `day:halves` text), `year_entitlement` (year, half
  days, Resturlaub, expiry day) and `marked_day` (ISO date, `FULL` or `HALF`). Entities hold plain
  text and numbers; `Mappers.kt` turns them into the `:core` types and skips a value it does not
  understand instead of crashing. The enum names are stored, so renaming one needs a migration (a
  test pins them).
- `UserSettings` (Land or null, extras, opt-outs, `WorkSchedule`) is in `:core.settings`, so the
  backup format can use it too. No Land is chosen until the user picks one; no default.
- `UserDataRepository` is what screens and the widget use: flows of `UserSettings`,
  `YearEntitlement?` and a date range of marks, plus the writes.
- The app language is not stored here: `AppCompatDelegate` remembers it (`autoStoreLocales`). The
  backup file leaves it out.
- Schemas go to `app/schemas/` (committed). `UrlaubsplanerDatabase.MIGRATIONS` has one entry per
  version step and a test fails if one is missing. No destructive fallback: it is the user's data.
- Tests run on the JVM with Robolectric (JUnit 4 in `:app`, Jupiter stays in `:core`). Robolectric
  uses Android 15 (`robolectric.properties`). In the cloud pass
  `-ProbolectricRepoUrl=https://maven-central.storage-download.googleapis.com/maven2`, because it
  downloads its Android image from Maven Central, which answers 429 there.
- The schema directory is a `debug` asset directory because Robolectric reads assets from the
  debug build; the release APK does not contain the schemas.
- Not built: school holiday file, backup, screens, a place that creates the database
  (`UrlaubsplanerDatabase.create(context)` exists; M3 calls it from an `Application`).

Known snags:

- School holiday data (M2): the cloud cannot reach openholidaysapi.org unless Nick adds it to the
  trusted sites; otherwise Nick exports the JSON once on his PC.
- Instrumented tests and the widget (M3, M4) need an emulator or Nick's phone.
- M5: the app has no launcher icon yet (lint `MissingApplicationIcon`).

## Status

- [x] M0 Skeleton (PR #3, merged 2026-10-25)
- [x] M1 Rules in `:core`: public holidays (PR #5), vacation balance (PR #7), Brückentage (PR #8, merged 2026-10-25), Bayern Mariä Himmelfahrt opt-out (PR #9, merged 2026-10-25)
- [ ] M2 Data and storage: Room (in review), school holiday file, backup
- [ ] M3 Screens
- [ ] M4 Export and widget
- [ ] M5 Release prep
