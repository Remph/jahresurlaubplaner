# Holiday data sources

Checked 2026-10-25 by web search only. The cloud container could not open openholidaysapi.org,
date.nager.at or feiertage-api.de (network policy), so every licence note below comes from
secondary sources and is **not confirmed**. Confirm on the official pages (for example from
Nick's PC) before any data file is bundled.

## Decision

- Public holidays (gesetzliche Feiertage): computed by rules in `:core` (Easter rule plus a table
  of Länder rules), covered by unit tests. No data licence involved.
- School holidays (Schulferien): a bundled, versioned data file per year, built from an
  OpenHolidays export and cross-checked with the KMK and Länder ministries each year. Never fetched
  at runtime.
- Nager.Date and feiertage-api.de: only as a cross-check for the public holiday tests, not
  bundled.

## What was found (unconfirmed)

| Source | Covers | Licence found |
| --- | --- | --- |
| OpenHolidays API | DE/AT/CH public and school holidays, subdivisions, JSON and iCal | Open data, free also for commercial use; processed data under ODbL (attribution, share-alike), per a third-party site |
| Nager.Date | Public holidays only | Licence of the data unclear; the NuGet package lists MIT (third-party site); offline use needs a sponsor key |
| feiertage-api.de | German public holidays | No terms found |

ODbL needs attribution and share-alike for a derived database. The data file would be a database
published under ODbL next to the GPL-3.0 code, with credit in the app's about screen.
