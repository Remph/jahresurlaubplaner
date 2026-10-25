package de.remph.urlaubsplaner.core.settings

import de.remph.urlaubsplaner.core.balance.WorkSchedule
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.holidays.RegionalExtra
import de.remph.urlaubsplaner.core.holidays.RegionalOptOut

/**
 * Everything the rules need to know about the user, apart from the vacation itself. It lives in
 * `:core` so the backup file can use the same type as the database.
 *
 * [land] is null until the user has chosen one: a default would show the holidays of the wrong
 * Land without anyone noticing. The app language is not here; the system remembers it.
 */
data class UserSettings(
    val land: Bundesland? = null,
    val extras: Set<RegionalExtra> = emptySet(),
    val optOuts: Set<RegionalOptOut> = emptySet(),
    val schedule: WorkSchedule = WorkSchedule(),
)
