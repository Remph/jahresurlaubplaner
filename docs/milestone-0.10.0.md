# Milestone 0.10.0

The plan for the first minor release after 0.9.x, written 2026-10-26 for the threads that build it.
Scope: parked items 18 to 23 (the "Minor" section of the parked list, all planned for 0.10.0 by the
maintainer on 2026-10-26). Nothing else goes in unless the maintainer asks. The rules of `CLAUDE.md`
apply unchanged; this file only adds how the integration branch works and in which order the items
are built.

## The integration branch

`release/0.10.0` collects the 0.10.0 features. It started from `main` at `d76c078` (0.9.1) and is
merged into `main` only when 0.10.0 is ready. Treat it like `main`:

- Nobody pushes to it directly. Every change reaches it through a PR that the maintainer merges.
- Each item gets its own branch `feature/0-10-<topic>`, made from `release/0.10.0`:

  ```
  git fetch origin release/0.10.0
  git checkout -b feature/0-10-<topic> origin/release/0.10.0
  ```

- The PR's base is **`release/0.10.0`**, not `main` (GitHub suggests `main`; change it). The PR
  description says "Part of 0.10.0" and names the parked item.
- Every code PR gets its independent review thread before the maintainer merges, as always. The
  review checks out the feature branch and runs the standard check there.
- PRs into `release/0.10.0` are merged with a merge commit (not squash, not rebase), like on `main`.
- To bring the latest `release/0.10.0` into a feature branch, merge it in; never rebase.
- After a merge into `release/0.10.0`, new work goes on a new branch, as on `main`.

### Patch releases (0.9.x) keep going from main

Fixes that ship as 0.9.x (the "Patch" section of the parked list) branch from `main` and go into
`main`, as before. After each 0.9.x is merged into `main`, `main` is merged into `release/0.10.0`:

```
git fetch origin main release/0.10.0
git checkout -b feature/0-10-sync-main-<n> origin/release/0.10.0
git merge origin/main        # a merge commit, never a rebase
./gradlew lint test assembleDebug spotlessCheck   # plus -ProbolectricRepoUrl=... in the cloud
git push -u origin feature/0-10-sync-main-<n>
```

Then a PR into `release/0.10.0`. A clean merge needs no review (like an ops change); a merge where
conflicts were resolved by hand gets a review thread for the resolved files only. Do the sync soon
after each patch release, so conflicts stay small.

Two rules keep the branches from fighting:

- **No schema or backup format change on `main` while `release/0.10.0` is open.** A database
  migration is a minor change by the version rule anyway. If one is ever needed on `main`, stop and
  ask the maintainer first: both branches would claim the same Room version.
- **The version number changes only at the end.** `versionCode` and `versionName` stay as `main`
  has them on `release/0.10.0` (901 / 0.9.1 today, or whatever the last sync brought) until the
  release PR sets 1000 / 0.10.0. Then the patch bumps on `main` (902, 903, ...) and their
  changelog files merge in without conflicts.

## The items

Size in the code, checked 2026-10-26 on `main` at `d76c078`:

| Item | What | Where in the code | Data change | Design question first |
| --- | --- | --- | --- | --- |
| 18 | Clear the marked days of one year | `settings/VacationPage.kt` or the calendar, `SettingsViewModel`, `UserDataRepository` / `MarkedDayDao` (a `DELETE ... BETWEEN`) | none | yes, one |
| 19 | School holidays and taken days more distinct | `planner/DayColors.kt`, `Legend` in `DayCells.kt`, `YearGrid` | none | yes, mock-ups |
| 20 | "Nächster Feiertag" opens the calendar with a highlight | `OverviewScreen.NextHoliday`, `AppRoot` (the `onOpenMonth` path exists), `PlannerViewModel`, `DayCells` | none | yes, animation options |
| 21 | Granted future days shown apart from planned ones | `:core` (mark status), `data/Entities.kt`, Room 3 to 4, `BackupFile` 1 to 2, `IcsExport`, `DaySheet`, `DayCells`, `YearGrid`, overview | Room + backup | yes, several |
| 22 | Slide animation when the month changes | `CalendarScreen` (`MonthGrid` is not animated yet), `ui/Motion.kt` | none | yes, one |
| 23 | Year rows expand only as far as the screen allows | `YearGrid` (`YearRowsCompact` 28 dp, `YearRowsExpanded` 48 dp), `CalendarScreen` | none | yes, one |

What the code says about each:

- **18.** There is no bulk delete yet: `MarkedDayDao` has `delete(date)`, `deleteDates(list)` and
  `deleteAll()`. Add one query for a date range and a repository call, wrapped in a confirmation
  dialog that names the year and the number of days and points to Datensicherung. The widget must
  redraw afterwards (check that `WidgetRefresher` sees the change). Tests: ViewModel test with Room
  in memory (only that year's marks go, other years and the entitlement stay), Compose test of the
  dialog in German and English.
- **19.** Taken and planned days use the theme's `primary` (filled and outlined). With system colours
  on, `primary` follows the wallpaper, so a blue wallpaper makes taken days and the fixed light blue
  school holidays (`0xFFD3E8F8` light, `0xFF244A68` dark) nearly the same. A fix has to hold for
  any wallpaper, in light, dark and OLED, with system colours on and off, and keep the weekend text
  contrast rule (`weekendTextColor`, 4.5:1) and the dashed Brückentage outline readable.
- **20.** The overview already opens a month in the calendar (`onOpenMonth`, used by the year grid on
  the overview). The new parts: the "Nächster Feiertag" block becomes a button (48 dp, a TalkBack
  label), the calendar gets a one-shot "highlight this date" from `PlannerViewModel`, and the tile
  plays the chosen effect once. With animations turned off in Android (`rememberAnimationsRemoved`)
  it shows a still outline for a moment instead.
- **21.** Today a marked day is only `FULL` or `HALF` (`DayMark`); "taken" and "planned" are not
  stored but follow from the date (up to today = taken). Granted is a new stored fact per marked
  day, so it touches the table, the backup file and the ICS export. Details below.
- **22.** The swipe (PR #28) and the arrows both call `PlannerViewModel.step(±1)`; the month content
  is drawn directly, without `AnimatedContent`. The tabs already slide with `rememberSharedAxis`
  (M5), so the month can reuse it with the direction of the step.
- **23.** Expanded rows are a fixed 48 dp; on a small phone the expanded grid is taller than the
  screen and the page scrolls (design review M3, question 4). The expanded height becomes
  "what fits": the visible height minus everything else on the page, divided by 12, between a
  minimum and a maximum. Font size 130 % and landscape must still work.

## Order and threads

Five build threads plus a release thread. Each thread asks its design question first (one at a
time, with a recommendation, keeps working on the recommended option meanwhile), then builds, runs
the standard check, opens its PR into `release/0.10.0` and reports "ready for review".

```
Phase 1 (in parallel)   T1 item 18            T2 items 19 + 21 design, build 19
Phase 2 (in parallel)   T3 item 21 build      T4 items 22 + 23
Phase 3                 T5 item 20
Phase 4                 T6 release 0.10.0
```

| Thread | Items | Branch | Depends on | Why this place |
| --- | --- | --- | --- | --- |
| T1 Clear a year's days | 18 | `feature/0-10-clear-year` | nothing | Small, settings only, no overlap with the calendar work. |
| T2 Colours and granted look | 19, design of 21 | `feature/0-10-colours` | nothing | 19 and 21 both decide what a day tile looks like; designing them together avoids choosing a colour twice. T2 builds only 19. |
| T3 Granted days | 21 | `feature/0-10-granted-days` | T2 merged (its decisions and `DayColors`) | The one data change: Room, backup, ICS. Its own thread because it is the biggest item. |
| T4 Calendar motion and year rows | 22, 23 | `feature/0-10-calendar-motion` | nothing hard; start after T2 is merged to avoid `YearGrid` conflicts | Both change `CalendarScreen` and `YearGrid`; one small round, one PR, one review. |
| T5 Next holiday highlight | 20 | `feature/0-10-holiday-highlight` | T3 and T4 merged (the tile drawing and the month animation it plays after) | The highlight draws on the tile that T2/T3 change and runs after the month slide from T4. Its mock-up videos can be made earlier. |
| T6 Release | all | `feature/0-10-release` | T1 to T5 merged | See "Release steps". |

T1 and T2 can start right away. If the maintainer wants fewer threads at a time, run them in the
order T1, T2, T4, T3, T5.

## Design questions, per thread

Asked by the thread that builds the item, one at a time. Recommendations are a starting point.

- **T1 (18).** Where the action lives and what it clears. Recommended: a row "Urlaubstage von 2027
  löschen" at the bottom of the year page in Einstellungen > Urlaub (that page already has the year
  stepper), clearing only the marked days of that year; the year's Urlaubstage and Resturlaub stay.
  Confirmation dialog with the number of days and the hint to make a backup first.
- **T2 (19).** Colour options as mock-ups (HTML plus Playwright screenshots, like
  `design/mockups-r1/`), every option in light, dark and OLED, with system colours on (a blue
  wallpaper as the worst case) and off. Options to compare: a school holiday colour that is not blue
  (for example a soft green or lavender), keeping blue but drawing school holidays only as the thin
  bar, or a fixed taken colour that ignores the wallpaper. Then, for 21: how a granted day looks next
  to planned (outline) and taken (filled), for example a filled tonal tile or the outline with a
  small check, in the month and the year grid and the legend.
- **T3 (21).** The data questions, one at a time: (a) is the distinction always there or behind a
  setting (recommended: always there, a new mark is "geplant" and the day sheet has a switch
  "genehmigt"; a setting only if the maintainer wants to hide it); (b) does the overview show
  "genehmigt" as its own number next to taken and planned (recommended: yes, one line, the balance
  itself does not change, granted days still count as planned); (c) the ICS export (recommended:
  `STATUS:CONFIRMED` for granted and `STATUS:TENTATIVE` for planned, a trip splits where the status
  changes); (d) the Brückentage "Eintragen" sheet and the "Bis" range mark as planned.
- **T4 (22, 23).** (22) A slide after the swipe or arrow, like the tabs (recommended, light), or a
  pager that follows the finger (bigger rework); and whether the year view slides the same way
  between years (recommended: yes, one animation). (23) The minimum and maximum row height when
  expanded (recommended: at least 36 dp, at most 64 dp, otherwise what fits).
- **T5 (20).** The highlight, compared in short videos like `design/m5-animations/`: a soft glow
  around the tile, a ring that grows and fades, a brief scale bounce, a fill that flashes and fades
  back. The maintainer wants to see something better than a plain glow, so show at least three.

## Migration and backup version steps (T3 only)

Only item 21 changes stored data. 0.9.0 is released, so old files and old databases must keep
working.

1. **Room 3 to 4.** One new column on `marked_day`, for example
   `granted INTEGER NOT NULL DEFAULT 0` (existing marks become "planned"). `MIGRATION_3_4` in
   `UrlaubsplanerDatabase`, `VERSION = 4`, the migration added to `MIGRATIONS`, `app/schemas/.../4.json`
   exported and committed, a migration test that opens version 3 with marks and reads them as not
   granted. A text column with an enum name is the alternative if the design brings more than two
   states; then a test pins the stored names, as for `mark`.
2. **Backup format 1 to 2.** `BackupFile.VERSION = 2`; each entry of `markedDays` gets
   `"granted": true|false`. An old app reading the new key would drop it silently, which the format
   rule forbids, so the version goes up: 0.9.x then refuses a 0.10.0 file with "newer version"
   instead of losing data. Reading: version 1 files still restore (no key, all planned); in version 2
   the key is required. Tests: the hand-written version 1 file still reads, a hand-written version 2
   file reads, a version 3 file is refused. Update the format comment in `BackupFile` and
   `docs/plan.md`.
3. **ICS.** No stored format, but the export changes as decided in T3 (c); the ODbL credit stays.
4. The changelog of 0.10.0 says that a backup made with 0.10.0 cannot be restored in 0.9.x.

If another item turns out to need a schema change after all, it takes the next version in merge
order (5 after 4) and the thread that merges second renumbers its migration and schema file.

Phone testing during the milestone: a debug APK with Room version 4 cannot go back to a 0.9.x build
(Room has no downgrade, and a version 2 backup does not restore in 0.9.x). Before the first T3 APK
goes on the phone, export a backup with the app that is installed; going back means uninstall,
install 0.9.x, restore that backup.

## Release steps (T6)

1. **Sync.** Merge the latest `main` into `release/0.10.0` (sync PR above), so the release contains
   every 0.9.x fix.
2. **Release PR into `release/0.10.0`** on `feature/0-10-release`: `versionCode = 1000`,
   `versionName = "0.10.0"` (0 * 10000 + 10 * 100 + 0); `fastlane/metadata/android/de-DE/changelogs/1000.txt`
   and `en-US/changelogs/1000.txt` (at most 500 characters, German first; include the backup note);
   new F-Droid screenshots where the colours or screens changed (real emulator screenshots, as in
   PR #39); README feature list and `docs/plan.md` up to date; the parked list's items 18 to 23 are
   moved to Done by the parked-items thread (through the coordinator). Standard check, then an
   independent review of the release PR.
3. **Full test on the maintainer's PC** (Remote Control session, after the release PR is merged into
   `release/0.10.0`): standard check, signed release APK 0.10.0 / 1000 (`docs/release.md`), the
   checklist "Before a release: test on the phone" on **both emulators** (API 30 and API 37) and on
   the **phone**. In addition for 0.10.0: install over 0.9.x with data (the Room 3 to 4 migration on
   a real device), restore a 0.9.x backup, export a new backup and see 0.9.x refuse it, clear a
   year, granted days in month, year, overview and ICS, the month slide, the holiday highlight, the
   expanded year rows on the small API 30 emulator, colours in light, dark and OLED with system
   colours on and off. `adb logcat` without FATAL or NoSuchMethodError. Instrumented tests
   (`connectedDebugAndroidTest`) only if parked item 10 has added some by then.
4. **Merge into `main`.** A PR from `release/0.10.0` into `main`; the maintainer merges it with a
   merge commit (not squash or rebase, so the history of both branches stays joined).
5. **Tag from the maintainer's PC** (the cloud gets 403 on tag pushes): tag the merge commit on `main`
   `v0.10.0` and push it. GitHub release with the signed APK only when the maintainer asks.
6. **Close the branch.** `release/0.10.0` is not reused; the next minor gets `release/0.11.0` from
   `main`. Deleting the old branch is the maintainer's call.
