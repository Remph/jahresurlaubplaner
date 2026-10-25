package de.remph.urlaubsplaner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * The tables store plain text and numbers, not Room type converters: the mapping to the `:core`
 * types is in `Mappers.kt`, where a bad value can be skipped instead of crashing the app.
 * Amounts are half days, dates are ISO text (`2026-12-24`), a day of the year is `--12-24`.
 * Sets are comma separated names (the Land is its two-letter code).
 */

/** The single row of settings (always [id] 1). */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SETTINGS_ID,
    /** Two-letter Land code, or null until the user has chosen. */
    val land: String?,
    val workingDays: String,
    val regionalExtras: String,
    val optOuts: String,
    /** Comma separated `day:halves` pairs like `--12-24:1`; a day not listed is a normal day. */
    val specialDays: String,
    /** Added in version 2; rows from version 1 get 0 (off). */
    @ColumnInfo(defaultValue = "0") val suggestCarryOver: Boolean = false,
) {
    companion object {
        const val SETTINGS_ID = 1
    }
}

@Entity(tableName = "year_entitlement")
data class YearEntitlementEntity(
    @PrimaryKey val year: Int,
    val halves: Int,
    val carryOverHalves: Int,
    val carryOverExpiry: String,
)

/** A day the user marked as vacation; [mark] is the name of a `DayMark`. */
@Entity(tableName = "marked_day")
data class MarkedDayEntity(
    @PrimaryKey val date: String,
    val mark: String,
)
