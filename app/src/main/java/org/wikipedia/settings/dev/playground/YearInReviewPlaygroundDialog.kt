package org.wikipedia.settings.dev.playground

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.presentation.YearInReviewActivity
import org.wikipedia.yearinreview.presentation.YearInReviewOnboardingActivity
import org.wikipedia.yearinreview.presentation.YearInReviewPage
import org.wikipedia.yearinreview.presentation.YearInReviewViewModel

class YearInReviewPlaygroundDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    var selectedData by remember { mutableStateOf(Prefs.yearInReviewPlaygroundData) }
                    var entryPoint by remember { mutableStateOf(Prefs.yearInReviewPlaygroundEntryPoint) }
                    var previewPageId by remember {
                        YearInReviewPlayground.previewPageId = null
                        mutableStateOf<String?>(null)
                    }
                    // Bumped each time the playground comes back, since the screens it opens can change login and history
                    var refreshCount by remember { mutableIntStateOf(0) }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshCount++ }
                    val userName = remember(refreshCount) { AccountUtil.userName.takeIf { AccountUtil.isLoggedIn } }
                    val isTemporaryAccount = remember(refreshCount) { AccountUtil.isTemporaryAccount }
                    val isYearInReviewEnabled = remember(refreshCount) { Prefs.isYearInReviewEnabled }
                    val hiddenCountryCodes = remember { YearInReviewPlayground.hiddenCountryCodes }
                    var realDataSummary by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(refreshCount) { realDataSummary = YearInReviewPlaygroundSummary.realData() }
                    YearInReviewPlaygroundScreen(
                        userName = userName,
                        isTemporaryAccount = isTemporaryAccount,
                        selectedData = selectedData,
                        onDataSelected = {
                            selectedData = it
                            Prefs.yearInReviewPlaygroundData = it
                            previewPageId = null
                            YearInReviewPlayground.previewPageId = null
                        },
                        previewPageId = previewPageId,
                        onPreviewPageSelected = {
                            previewPageId = it
                            YearInReviewPlayground.previewPageId = it
                        },
                        entryPoint = entryPoint,
                        canShowEntryPoint = remember(entryPoint, refreshCount) { YearInReviewViewModel.canShowEntryPoint },
                        hiddenReasons = remember(entryPoint, isYearInReviewEnabled) { entryPoint.hiddenReasons(isYearInReviewEnabled, hiddenCountryCodes) },
                        hiddenCountryCodes = hiddenCountryCodes,
                        onEntryPointChange = {
                            entryPoint = it
                            Prefs.yearInReviewPlaygroundEntryPoint = it
                        },
                        realDataSummary = realDataSummary,
                        onAnnouncementClick = { startActivity(YearInReviewOnboardingActivity.newIntent(requireContext())) },
                        // Clears the task, so going back from the Activity tab exits the app as it normally would
                        onActivityTabClick = {
                            startActivity(MainActivity.newIntent(requireContext())
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
                                .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EDITS.code()))
                        },
                        onLaunchClick = { startActivity(YearInReviewActivity.newIntent(requireContext())) },
                        onBackClick = { dismiss() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            BottomSheetBehavior.from(it).isDraggable = false
        }
    }
}

@Composable
fun YearInReviewPlaygroundScreen(
    userName: String?,
    isTemporaryAccount: Boolean,
    selectedData: YearInReviewPlaygroundData,
    onDataSelected: (YearInReviewPlaygroundData) -> Unit,
    previewPageId: String?,
    onPreviewPageSelected: (String?) -> Unit,
    entryPoint: YearInReviewPlaygroundEntryPoint,
    canShowEntryPoint: Boolean,
    hiddenReasons: List<String>,
    hiddenCountryCodes: List<String>,
    onEntryPointChange: (YearInReviewPlaygroundEntryPoint) -> Unit,
    realDataSummary: String?,
    onAnnouncementClick: () -> Unit,
    onActivityTabClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                    contentDescription = stringResource(R.string.nav_item_back),
                    tint = WikipediaTheme.colors.primaryColor
                )
            }
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Year in Review Playground",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = WikipediaTheme.colors.primaryColor
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReadingDataCard(
                userName = userName,
                isTemporaryAccount = isTemporaryAccount,
                selectedData = selectedData,
                onDataSelected = onDataSelected,
                previewPageId = previewPageId,
                onPreviewPageSelected = onPreviewPageSelected,
                realDataSummary = realDataSummary
            )
            EntryPointCard(
                entryPoint = entryPoint,
                canShowEntryPoint = canShowEntryPoint,
                hiddenReasons = hiddenReasons,
                hiddenCountryCodes = hiddenCountryCodes,
                onEntryPointChange = onEntryPointChange
            )
        }
        LaunchButtons(
            onAnnouncementClick = onAnnouncementClick,
            onActivityTabClick = onActivityTabClick,
            onLaunchClick = onLaunchClick
        )
    }
}

@Composable
private fun LaunchButtons(
    onAnnouncementClick: () -> Unit,
    onActivityTabClick: () -> Unit,
    onLaunchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WikipediaTheme.colors.paperColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "The announcement opens even when the entry point checks would hide it. The Activity tab only shows the card when they pass.",
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onAnnouncementClick
            ) {
                Text("Announcement", color = WikipediaTheme.colors.progressiveColor)
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onActivityTabClick
            ) {
                Text("Activity tab", color = WikipediaTheme.colors.progressiveColor)
            }
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLaunchClick
        ) {
            Text("Launch Year in Review")
        }
    }
}

@Composable
private fun ReadingDataCard(
    userName: String?,
    isTemporaryAccount: Boolean,
    selectedData: YearInReviewPlaygroundData,
    onDataSelected: (YearInReviewPlaygroundData) -> Unit,
    previewPageId: String?,
    onPreviewPageSelected: (String?) -> Unit,
    realDataSummary: String?
) {
    PlaygroundCard(title = "Year in Review data") {
        Text(
            text = "The data used by the Activity tab card, the announcement and the slides. Takes effect the next time each one loads.",
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.secondaryColor
        )
        // Temporary accounts count as logged in, the same as in the slides
        Text(
            text = when {
                userName == null -> "You're not logged in. Personalized only shows the personalized slides when you're logged in, so log in first."
                isTemporaryAccount -> "Logged in with the temporary account $userName."
                else -> "Logged in as $userName."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (userName != null) WikipediaTheme.colors.successColor else WikipediaTheme.colors.destructiveColor
        )
        PlaygroundOptions(
            options = YearInReviewPlaygroundData.entries.filter { it.previewPages == null },
            selectedOption = selectedData,
            label = { it.label },
            description = { it.description },
            onOptionSelected = onDataSelected
        )
        if (selectedData == YearInReviewPlaygroundData.REAL) {
            realDataSummary?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = WikipediaTheme.colors.primaryColor)
            }
        }
        PlaygroundSectionTitle(title = "Preview", enabled = true)
        Text(
            text = "Fixed slides with sample data, for checking how every slide looks. Skips the flow logic, so login and data don't matter.",
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.secondaryColor
        )
        PlaygroundOptions(
            options = YearInReviewPlaygroundData.entries.filter { it.previewPages != null },
            selectedOption = selectedData,
            label = { it.label },
            description = { it.description },
            onOptionSelected = onDataSelected
        )
        selectedData.previewPages?.let { previewPages ->
            PreviewSlidePicker(
                pages = remember(selectedData) { previewPages() },
                selectedPageId = previewPageId,
                onPageSelected = onPreviewPageSelected
            )
        }
    }
}

@Composable
private fun PreviewSlidePicker(
    pages: List<YearInReviewPage>,
    selectedPageId: String?,
    onPageSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = pages.indexOfFirst { it.id == selectedPageId }
    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Text(
                text = if (selectedIndex >= 0) "Slide: ${slideLabel(selectedIndex, pages[selectedIndex])}" else "Slides: all ${pages.size}",
                color = WikipediaTheme.colors.progressiveColor
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All ${pages.size} slides") },
                onClick = {
                    expanded = false
                    onPageSelected(null)
                }
            )
            pages.forEachIndexed { index, page ->
                DropdownMenuItem(
                    text = { Text(slideLabel(index, page)) },
                    onClick = {
                        expanded = false
                        onPageSelected(page.id)
                    }
                )
            }
        }
    }
}

private fun slideLabel(index: Int, page: YearInReviewPage) = "${index + 1}. ${page.id}"

@Composable
private fun EntryPointCard(
    entryPoint: YearInReviewPlaygroundEntryPoint,
    canShowEntryPoint: Boolean,
    hiddenReasons: List<String>,
    hiddenCountryCodes: List<String>,
    onEntryPointChange: (YearInReviewPlaygroundEntryPoint) -> Unit
) {
    val isDateAndCountryChecked = entryPoint.useTestValues && entryPoint.hasRemoteConfig
    PlaygroundCard(title = "Entry point checks") {
        Text(
            text = "Controls whether the Year in Review entry points show. " +
                    "The checks combine: each one below can hide the entry point on its own. The Year in Review setting always stays real.",
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.secondaryColor
        )
        Text(
            text = when {
                !entryPoint.useTestValues -> "Entry point: ${if (canShowEntryPoint) "shown" else "hidden"} (real checks)"
                canShowEntryPoint -> "Entry point: shown"
                else -> "Entry point: hidden, because ${hiddenReasons.joinToString(", and ")}"
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (canShowEntryPoint) WikipediaTheme.colors.successColor else WikipediaTheme.colors.destructiveColor
        )
        PlaygroundSwitch(
            title = "Use test values",
            description = if (entryPoint.useTestValues) {
                "The remote config, date and country below are used instead of the real ones."
            } else {
                "Off: the real remote config, country and date are used. While developer settings are on, the entry point always shows."
            },
            checked = entryPoint.useTestValues,
            onCheckedChange = { onEntryPointChange(entryPoint.copy(useTestValues = it)) }
        )
        PlaygroundSectionTitle(title = "Remote config for ${YearInReviewConfig.YEAR}", enabled = entryPoint.useTestValues)
        PlaygroundOptions(
            options = listOf(true, false),
            selectedOption = entryPoint.hasRemoteConfig,
            label = { hasRemoteConfig -> if (hasRemoteConfig) "Published" else "Not published" },
            description = { hasRemoteConfig ->
                if (hasRemoteConfig) "Active from $testActiveStartDate to $testActiveEndDate." else "The entry point is hidden, and the date and country aren't checked."
            },
            enabled = entryPoint.useTestValues,
            onOptionSelected = { onEntryPointChange(entryPoint.copy(hasRemoteConfig = it)) }
        )
        PlaygroundSectionTitle(
            title = "Date",
            enabled = isDateAndCountryChecked,
            note = if (entryPoint.useTestValues && !entryPoint.hasRemoteConfig) "Not checked: remote config isn't published" else null
        )
        PlaygroundOptions(
            options = YearInReviewPlaygroundDate.entries,
            selectedOption = entryPoint.date,
            label = { it.label },
            description = { it.description },
            enabled = isDateAndCountryChecked,
            onOptionSelected = { onEntryPointChange(entryPoint.copy(date = it)) }
        )
        PlaygroundSectionTitle(
            title = "Country",
            enabled = isDateAndCountryChecked,
            note = if (entryPoint.useTestValues && !entryPoint.hasRemoteConfig) "Not checked: remote config isn't published" else null
        )
        CountryCodeField(
            value = entryPoint.countryCode,
            enabled = isDateAndCountryChecked,
            onValueChange = { onEntryPointChange(entryPoint.copy(countryCode = it)) }
        )
        Text(
            text = "Hidden in (from the latest live remote config): ${hiddenCountryCodes.joinToString()}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDateAndCountryChecked) WikipediaTheme.colors.secondaryColor else WikipediaTheme.colors.inactiveColor
        )
    }
}

@Composable
private fun PlaygroundCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .background(WikipediaTheme.colors.backgroundColor)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = WikipediaTheme.colors.primaryColor
            )
            content()
        }
    }
}

@Composable
private fun PlaygroundSectionTitle(
    title: String,
    enabled: Boolean,
    note: String? = null
) {
    Text(
        modifier = Modifier.padding(top = 8.dp),
        text = if (note != null) "$title · $note" else title,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) WikipediaTheme.colors.primaryColor else WikipediaTheme.colors.inactiveColor
    )
}

@Composable
private fun PlaygroundSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) WikipediaTheme.colors.primaryColor else WikipediaTheme.colors.inactiveColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) WikipediaTheme.colors.secondaryColor else WikipediaTheme.colors.inactiveColor
            )
        }
        Switch(
            modifier = Modifier.padding(start = 12.dp),
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}

// Only saves complete two-letter codes, so the check never runs against a partly typed one
@Composable
private fun CountryCodeField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            if (newText.length <= 2 && newText.all { it.isLetter() }) {
                text = newText.uppercase()
                if (text.length == 2) {
                    onValueChange(text)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text("Country code") },
        isError = enabled && text.length != 2,
        singleLine = true
    )
}

@Composable
private fun <T> PlaygroundOptions(
    options: List<T>,
    selectedOption: T,
    label: (T) -> String,
    description: (T) -> String,
    onOptionSelected: (T) -> Unit,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option == selectedOption,
                        enabled = enabled,
                        onClick = { onOptionSelected(option) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = null,
                    enabled = enabled,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WikipediaTheme.colors.progressiveColor,
                        unselectedColor = WikipediaTheme.colors.primaryColor
                    )
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) WikipediaTheme.colors.primaryColor else WikipediaTheme.colors.inactiveColor
                    )
                    Text(
                        text = description(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) WikipediaTheme.colors.secondaryColor else WikipediaTheme.colors.inactiveColor
                    )
                }
            }
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun YearInReviewPlaygroundScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewPlaygroundScreen(
            userName = null,
            isTemporaryAccount = false,
            selectedData = YearInReviewPlaygroundData.REAL,
            onDataSelected = {},
            previewPageId = null,
            onPreviewPageSelected = {},
            entryPoint = YearInReviewPlaygroundEntryPoint(useTestValues = true, date = YearInReviewPlaygroundDate.ACTIVE, countryCode = "RU"),
            canShowEntryPoint = false,
            hiddenReasons = listOf("RU is a hidden country"),
            hiddenCountryCodes = listOf("RU", "IR", "CN"),
            onEntryPointChange = {},
            realDataSummary = "Entry card and announcement: Data-rich\nSlides: Personalized\nReading history from 2026-01-01 to 2026-11-30\n" +
                    "Articles read: 3\nVisited days: 2\nVisited days in the peak month: 2",
            onAnnouncementClick = {},
            onActivityTabClick = {},
            onLaunchClick = {},
            onBackClick = {}
        )
    }
}
