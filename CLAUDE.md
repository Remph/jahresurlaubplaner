# Urlaubsplaner (repo: jahresurlaubplaner): notes for Claude

Personal, non-profit Android app for planning the year's vacation in Germany: holidays, school
holidays, vacation balance and bridge days, status first. Nick's own phone, used on the go.
Local-first, no backend for the MVP. The skeleton is in (Gradle with `:core` and `:app`, no features
yet); the plan and milestones are in `docs/plan.md`.

## Decided
- Android first; no web app for now. Distribution: F-Droid preferred, Google Play optional.
- Local-first; sync starts as a backup file chosen through the Android file picker.
- Holidays come from a versioned data file bundled in the app and updated yearly; no holiday API
  at runtime (works offline, reveals no region).
- Germany first (Bundesland-specific); Austria and Switzerland later.
- MVP scope: Feiertage and Schulferien per Bundesland; vacation balance (annual entitlement,
  Resturlaub with a configurable expiry date, days taken and planned, half days); marking single
  days and showing Brückentage; year/month view with status first (remaining days, next trip);
  ICS export; backup export/import; home screen widget (next vacation day, remaining days).
- Not MVP, only when Nick asks: categories and trips, AT/CH, multi-person overlap, cloud sync.

## Decided (design decisions, 2026-10-25)
- Stack: Kotlin, Jetpack Compose with Material 3, Room.
- Licence of the app source: GPL-3.0.
- Package name (application ID): `de.remph.urlaubsplaner`. Permanent, do not change it.
- App name (label): Urlaubsplaner. Only a string resource, easy to change.
- Holiday data: public holidays are computed by rules in `:core` (Easter rule plus Länder tables,
  unit-tested). Only school holidays come from a bundled, versioned yearly data file, built from
  an OpenHolidays export and cross-checked with KMK. Details and licence notes:
  `docs/data-sources.md`.
- minSdk 30 (Android 11), compileSdk and targetSdk 37; details in `docs/plan.md`. Decided 2026-10-25.
- Google Play: no developer account for now. Decide before a public release and re-check
  Google's rules and fee on their own pages first.

## Open decisions (ask Nick, don't assume)
- OpenHolidays licence is confirmed (ODbL, 2026-10-25). When the first school holiday data file is
  bundled: keep it under ODbL in its own directory with the licence text, and add the credit to
  the about screen and the ICS export (`docs/data-sources.md`).
- Before the first release: Google Play account (see above) and the F-Droid metadata.

## What to read
- `README.md`: what the app does and how to build it.
- `docs/`: `plan.md` (layout, build settings, milestones), `data-sources.md`.
- Check facts that change over time (store rules, data sources, API terms, Google's developer
  verification) with search and write the date next to what you found.

## Commands
Run from the repo root (JDK 17 or newer; the Gradle wrapper is committed). All of these were run
in the cloud container on 2026-10-25:

- `./gradlew lint test assembleDebug spotlessCheck`: lint, unit tests, debug APK, format check.
  The standard check.
- `./gradlew spotlessApply`: fix formatting (ktlint through Spotless).
- `./gradlew test --tests '*SomeTest'`: one class while working.
- `./gradlew -PcoreOnly :core:test`: only the rules module, no Android SDK needed.
- `./gradlew connectedDebugAndroidTest`: instrumented tests, need an emulator or device. Not
  possible in the cloud container: say "not run" in the PR and ask Nick to run it on his PC.

### Cloud containers
Since 2026-10-25 `dl.google.com` is reachable, so the app module builds in the cloud. The
environment's setup script `/mnt/project-files/ops/cloud-setup-script.sh` installs the Android SDK
(platform 37.0, build-tools 37.0.0) into `/opt/android-sdk`; in a resumed thread without
`$ANDROID_HOME` run it once by hand and `export ANDROID_HOME=/opt/android-sdk`. If
`$ANDROID_HOME/platforms` stays empty, `dl.google.com` is missing from the network policy; tell
Nick (Project settings > Cloud environment > Network access) instead of working around it.

- Keep the app's rules (holidays, vacation balance, bridge days, ICS export, backup format) in
  the plain Kotlin/JVM module `:core` with no Android imports; it also builds without the SDK.
- `settings.gradle.kts` skips `:app` with `-PcoreOnly`. Do not apply Android plugins in the root
  build file, or `-PcoreOnly` cannot skip them.
- Versions live in `gradle/libs.versions.toml`; the plan and milestones in `docs/plan.md`.
- openholidaysapi.org and similar sites are blocked unless Nick adds them to the trusted sites.

## Git
- Every change on its own `feature/<topic>` branch with a PR into `main`; Nick reviews and
  merges. Never commit or push to `main`, never force-push, never rebase (merge `main` in).
  `git fetch origin main` before branching: a fresh clone's `origin/main` can be stale.
- After a merge, new work goes on a NEW branch and PR; never reuse a merged branch. If a PR was
  merged before its review finished, the fixes go on a new branch and PR.
- One thread pushes to a branch at a time: the review thread during the review, the building
  thread again afterwards.
- Never add two files whose names differ only in case (Nick builds on Windows).
- Never commit keystores, `local.properties`, tokens or Google service files.
- No tags or releases unless Nick asks. The repo has no deploy.

## Checks (no CI on pull requests)
GitHub does not check pull requests (the free Actions minutes are used up). Before reporting
done, run the standard check (and the format check) and list each command with its result in the
PR description (`.github/pull_request_template.md`); say plainly which checks could not run and
why. A review runs them again. A docs-only PR says "no code, no checks".

## Reviews
- Every code feature or fix gets an independent review in a new thread before Nick merges
  (docs-only, text-only and ops changes don't). A thread never reviews its own work: it reports
  "ready for review" and the coordinator starts the review thread on the same branch.
- The review fixes what it judges worth fixing on that branch without asking and asks Nick only
  about design choices, one question at a time.
- A small fix round (several small items) is one build thread, one branch, one PR and one review
  thread. Bigger features get a thread each.

## Code rules
- Design first and Nick's OK before building; no features beyond the agreed scope.
- Minimal UI, phone and touch first, nothing hover-only, touch targets of at least 48dp,
  TalkBack labels. A competing app felt cluttered to Nick: status first, few controls.
- German is the first language of the app and its texts, English second: every visible text is a
  string resource in both. Use everyday German words; ask when unsure.
- Every rule of the app (holiday dates, balance and carry-over expiry, half days, bridge days)
  gets a unit test. Use an injected clock, never `now()` in logic. Dates are `java.time` types.
- Holidays and school holidays come from the bundled, versioned data file, never from the
  network at runtime. Verify each year against official sources (KMK, Länder ministries).
- Room: schema changes need a migration and a migration test; schemas are exported
  and committed.
- Comment the "why", not the "what".
- Personal notes and real device or account details stay out of the repo if it is public.

## Dependencies
Well-known, maintained libraries may be added when they make development easier (name them in the
PR; ask Nick first only when unsure). No paid services, no premium features, nothing from Palantir
or tied in a major positive way to Peter Thiel. For F-Droid: no proprietary dependencies (no
Google Play Services, no tracking).
