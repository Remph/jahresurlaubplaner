package de.remph.urlaubsplaner.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.core.backup.BackupFile
import de.remph.urlaubsplaner.core.backup.BackupSummary
import de.remph.urlaubsplaner.core.balance.AmountText
import de.remph.urlaubsplaner.core.balance.VacationDays
import de.remph.urlaubsplaner.core.balance.WorkSchedule
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.holidays.RegionalExtra
import de.remph.urlaubsplaner.core.holidays.RegionalOptOut
import java.time.DayOfWeek
import java.time.MonthDay
import java.time.format.TextStyle
import java.util.Locale

/** The settings page: one scrolling column; on a fresh start only the first steps show. */
@Composable
fun SettingsRoute(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ui = state ?: return
    val snackbar = remember { SnackbarHostState() }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BackupFile.MIME_TYPE),
        ) { uri: Uri? -> uri?.let(viewModel::export) }
    // Any type: a file saved by hand or moved through a cloud drive does not always keep its type.
    // A file that is not a backup is refused with a message before anything is changed.
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let(viewModel::import)
        }

    val notice = ui.notice
    val noticeText = notice?.let { noticeText(it) }
    LaunchedEffect(notice) {
        if (notice != null && noticeText != null && !notice.needsDialog()) {
            snackbar.showSnackbar(noticeText)
            viewModel.dismissNotice()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            LandSection(ui, viewModel)
            if (ui.settings.land != null) {
                VacationSection(ui, viewModel)
                WorkDaysSection(ui, viewModel)
                RegionalSection(ui, viewModel)
            }
            LanguageSection()
            BackupSection(
                canExport = ui.settings.land != null,
                onExport = { exportLauncher.launch(viewModel.suggestedBackupName()) },
                onImport = { importLauncher.launch(arrayOf("*/*")) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    ui.pendingImport?.let { ImportDialog(it, viewModel) }
    if (notice != null && noticeText != null && notice.needsDialog()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            text = { Text(noticeText) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

// Sections.

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun LandSection(
    ui: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    var open by remember { mutableStateOf(false) }
    val land = ui.settings.land
    Section(stringResource(R.string.section_land)) {
        if (land == null) Text(stringResource(R.string.land_first_start))
        val name =
            land?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.land_choose)
        val description =
            land?.let { stringResource(R.string.land_button_description, name) } ?: name
        // First start: a filled button, it is the one thing to do. Afterwards a quiet outline.
        if (land == null) {
            Button(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
            ) { Text(name) }
        } else {
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
            ) { Text(name) }
        }
    }
    if (open) {
        LandDialog(
            selected = land,
            onSelect = {
                viewModel.chooseLand(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun LandDialog(
    selected: Bundesland?,
    onSelect: (Bundesland) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.land_choose)) },
        text = {
            LazyColumn {
                items(Bundesland.entries) { land ->
                    ChoiceRow(stringResource(land.labelRes()), land == selected) { onSelect(land) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun VacationSection(
    ui: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val locale = currentLocale()
    val form = ui.form
    Section(stringResource(R.string.section_vacation)) {
        val previous = stringResource(R.string.year_previous)
        val next = stringResource(R.string.year_next)
        val yearDescription = stringResource(R.string.year_description, ui.year)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { viewModel.selectYear(ui.year - 1) },
                enabled = ui.canGoBack,
                modifier = Modifier.size(48.dp).semantics { contentDescription = previous },
            ) { Text("‹", style = MaterialTheme.typography.titleLarge) }
            Text(
                text = ui.year.toString(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { contentDescription = yearDescription },
            )
            TextButton(
                onClick = { viewModel.selectYear(ui.year + 1) },
                enabled = ui.canGoForward,
                modifier = Modifier.size(48.dp).semantics { contentDescription = next },
            ) { Text("›", style = MaterialTheme.typography.titleLarge) }
        }
        form.suggestedFrom?.let { from ->
            val carry = form.base?.carryOver
            val withCarryOver =
                ui.settings.suggestCarryOver && carry != null && carry > VacationDays.ZERO
            Text(
                text =
                    if (withCarryOver) {
                        stringResource(R.string.suggestion_from_year_with_carry_over, from)
                    } else {
                        stringResource(R.string.suggestion_from_year, from)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountField(
            label = stringResource(R.string.field_days),
            value =
                form.typedDays ?: form.base
                    ?.days
                    ?.let { AmountText.format(it, locale) }
                    .orEmpty(),
            isError = form.daysError,
            error = stringResource(R.string.error_days),
            onChange = viewModel::editDays,
        )
        AmountField(
            label = stringResource(R.string.field_carry_over),
            value =
                form.typedCarryOver
                    ?: AmountText.format(form.base?.carryOver ?: VacationDays.ZERO, locale),
            isError = form.carryOverError,
            error = stringResource(R.string.error_days),
            onChange = viewModel::editCarryOver,
        )
        AmountField(
            label = stringResource(R.string.field_carry_over_expiry),
            value =
                form.typedExpiry
                    ?: AmountText.formatDayMonth(form.base?.expiry ?: MonthDay.of(3, 31)),
            isError = form.expiryError,
            error = stringResource(R.string.error_expiry),
            placeholder = stringResource(R.string.field_expiry_format),
            onChange = viewModel::editExpiry,
        )
        Button(
            onClick = viewModel::saveEntitlement,
            enabled = form.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.save)) }
        SwitchRow(
            label = stringResource(R.string.suggest_carry_over),
            support = stringResource(R.string.suggest_carry_over_support),
            checked = ui.settings.suggestCarryOver,
            onChange = viewModel::setSuggestCarryOver,
        )
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    isError: Boolean,
    error: String,
    onChange: (String) -> Unit,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = if (isError) ({ Text(error) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkDaysSection(
    ui: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val locale = currentLocale()
    val schedule = ui.settings.schedule
    Section(stringResource(R.string.section_work_days)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (day in DayOfWeek.entries) {
                val works = day in schedule.workingDays
                FilterChip(
                    selected = works,
                    onClick = { viewModel.setWorkingDay(day, !works) },
                    label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    modifier =
                        Modifier
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = day.getDisplayName(TextStyle.FULL, locale)
                            },
                )
            }
        }
        val christmasEve = stringResource(R.string.special_christmas_eve)
        SpecialDay(WorkSchedule.HEILIGABEND, christmasEve, schedule, viewModel)
        val newYearsEve = stringResource(R.string.special_new_years_eve)
        SpecialDay(WorkSchedule.SILVESTER, newYearsEve, schedule, viewModel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialDay(
    day: MonthDay,
    label: String,
    schedule: WorkSchedule,
    viewModel: SettingsViewModel,
) {
    val value = schedule.specialDays[day] ?: VacationDays.ONE
    val options =
        listOf(
            VacationDays.ONE to stringResource(R.string.special_full),
            VacationDays.HALF to stringResource(R.string.special_half),
            VacationDays.ZERO to stringResource(R.string.special_off),
        )
    Text(label, style = MaterialTheme.typography.bodyLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((amount, text) in options) {
            val description = stringResource(R.string.special_options_description, label, text)
            FilterChip(
                selected = value == amount,
                onClick = { viewModel.setSpecialDay(day, amount) },
                label = { Text(text) },
                modifier =
                    Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = description },
            )
        }
    }
}

@Composable
private fun RegionalSection(
    ui: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val land = ui.settings.land ?: return
    val extras = RegionalExtra.entries.filter { land in it.lands }
    val optOuts = RegionalOptOut.entries.filter { land in it.lands }
    if (extras.isEmpty() && optOuts.isEmpty()) return
    Section(stringResource(R.string.section_regional)) {
        for (extra in extras) {
            val (label, support) =
                when (extra) {
                    RegionalExtra.AUGSBURGER_FRIEDENSFEST -> {
                        R.string.extra_augsburg to R.string.extra_augsburg_support
                    }

                    RegionalExtra.FRONLEICHNAM_IN_TEILEN -> {
                        R.string.extra_fronleichnam to R.string.extra_fronleichnam_support
                    }
                }
            SwitchRow(
                label = stringResource(label),
                support = stringResource(support),
                checked = extra in ui.settings.extras,
                onChange = { viewModel.setExtra(extra, it) },
            )
        }
        for (optOut in optOuts) {
            val (label, support) =
                when (optOut) {
                    RegionalOptOut.MARIAE_HIMMELFAHRT_BAYERN -> {
                        R.string.mariae_himmelfahrt_bayern to
                            R.string.mariae_himmelfahrt_bayern_support
                    }
                }
            // The switch says "holiday", the stored opt-out the opposite: on = not opted out.
            SwitchRow(
                label = stringResource(label),
                support = stringResource(support),
                checked = optOut !in ui.settings.optOuts,
                onChange = { viewModel.setHolidayCounts(optOut, it) },
            )
        }
    }
}

@Composable
private fun LanguageSection() {
    var choice by remember { mutableStateOf(LanguageChoice.current()) }
    Section(stringResource(R.string.section_language)) {
        for (
        (option, label) in
        listOf(
            LanguageChoice.SYSTEM to R.string.language_system,
            LanguageChoice.GERMAN to R.string.language_german,
            LanguageChoice.ENGLISH to R.string.language_english,
        )
        ) {
            ChoiceRow(stringResource(label), choice == option) {
                choice = option
                // Restarts the activity in the new language.
                LanguageChoice.apply(option)
            }
        }
    }
}

@Composable
private fun BackupSection(
    canExport: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Section(stringResource(R.string.section_backup)) {
        Text(
            stringResource(R.string.backup_explain),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canExport) {
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_export))
            }
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.backup_import))
        }
    }
}

@Composable
private fun ImportDialog(
    plan: ImportPlan,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val body =
        stringResource(
            R.string.backup_import_body,
            summaryText(context, plan.current),
            summaryText(context, plan.file),
        )
    val noLand =
        if (plan.current.landChosen && !plan.file.landChosen) {
            "\n\n" + stringResource(R.string.backup_import_no_land)
        } else {
            ""
        }
    AlertDialog(
        onDismissRequest = viewModel::cancelImport,
        title = { Text(stringResource(R.string.backup_import_title)) },
        text = { Text(body + noLand) },
        confirmButton = {
            TextButton(onClick = viewModel::confirmImport) {
                Text(stringResource(R.string.backup_import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** "12 markierte Tage und 2 Jahre mit Urlaubsanspruch", or "keine Einträge" for nothing at all. */
private fun summaryText(
    context: Context,
    summary: BackupSummary,
): String {
    val resources = context.resources
    val marked = summary.markedDays
    val days = resources.getQuantityString(R.plurals.backup_marked_days, marked, marked)
    val years = resources.getQuantityString(R.plurals.backup_years, summary.years, summary.years)
    return when {
        marked == 0 && summary.years == 0 -> context.getString(R.string.backup_summary_empty)
        summary.years == 0 -> days
        marked == 0 -> years
        else -> context.getString(R.string.backup_summary_join, days, years)
    }
}

// Messages.

private fun Notice.needsDialog() =
    when (this) {
        Notice.ImportNotABackup,
        is Notice.ImportNewerVersion,
        is Notice.ImportDamaged,
        Notice.ImportUnreadable,
        -> true

        else -> false
    }

@Composable
private fun noticeText(notice: Notice): String =
    when (notice) {
        Notice.EntitlementSaved -> {
            stringResource(R.string.notice_saved)
        }

        Notice.Exported -> {
            stringResource(R.string.notice_exported)
        }

        Notice.Imported -> {
            stringResource(R.string.notice_imported)
        }

        Notice.ExportFailed -> {
            stringResource(R.string.notice_export_failed)
        }

        Notice.ImportUnreadable -> {
            stringResource(R.string.notice_import_unreadable)
        }

        Notice.ImportNotABackup -> {
            stringResource(R.string.notice_not_a_backup)
        }

        is Notice.ImportNewerVersion -> {
            stringResource(R.string.notice_newer_version, notice.found, notice.supported)
        }

        is Notice.ImportDamaged -> {
            stringResource(R.string.notice_damaged, notice.detail)
        }
    }

// Small building blocks.

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun SwitchRow(
    label: String,
    support: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = checked, onValueChange = onChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                support,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
