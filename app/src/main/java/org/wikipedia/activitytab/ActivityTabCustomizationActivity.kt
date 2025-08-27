package org.wikipedia.activitytab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.ActivityTabEvent
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil

class ActivityTabCustomizationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            BaseTheme {
                CustomizationScreen(
                    onBackButtonClick = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (Prefs.activityTabModules.areAllModulesDisabled())
            ActivityTabEvent.submit(activeInterface = "activity_tab_cutomize", "customize_click", all = "off")

        if (Prefs.activityTabModules.areAllModulesEnabled())
            ActivityTabEvent.submit(activeInterface = "activity_tab_cutomize", "customize_click", all = "on")
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ActivityTabCustomizationActivity::class.java)
        }
    }
}

@Composable
fun CustomizationScreen(
    modifier: Modifier = Modifier,
    onBackButtonClick: () -> Unit
) {
    var currentModules by remember { mutableStateOf(Prefs.activityTabModules) }

    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.activity_tab_menu_customize),
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.backgroundColor,
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(vertical = 16.dp),
            ) {
                item {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        text = stringResource(R.string.activity_tab_menu_customize_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
                itemsIndexed(ModuleType.entries) { index, moduleType ->
                    CustomizationScreenSwitch(
                        isChecked = currentModules.isModuleEnabled(moduleType),
                        title = stringResource(moduleType.displayName),
                        onCheckedChange = { isChecked ->
                            submitModuleToggleEvent(moduleType, isChecked)
                            currentModules = currentModules.setModuleEnabled(moduleType, isChecked)
                            Prefs.activityTabModules = currentModules
                        }
                    )
                    if (index < ModuleType.entries.size - 1) {
                        HorizontalDivider(
                            color = WikipediaTheme.colors.borderColor
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CustomizationScreenSwitch(
    isChecked: Boolean,
    title: String,
    onCheckedChange: ((Boolean) -> Unit),
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.paperColor
        ),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    onCheckedChange(it)
                },
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = WikipediaTheme.colors.paperColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                    checkedThumbColor = WikipediaTheme.colors.paperColor
                )
            )
        }
    )
}

fun ActivityTabModules.isModuleEnabled(moduleType: ModuleType): Boolean = when (moduleType) {
    ModuleType.TIME_SPENT -> isTimeSpentEnabled
    ModuleType.READING_INSIGHTS -> isReadingInsightsEnabled
    ModuleType.EDITING_INSIGHTS -> isEditingInsightsEnabled
    ModuleType.IMPACT -> isImpactEnabled
    ModuleType.GAMES -> isGamesEnabled
    ModuleType.DONATIONS -> isDonationsEnabled
    ModuleType.TIMELINE -> isTimelineEnabled
}

fun ActivityTabModules.setModuleEnabled(moduleType: ModuleType, enabled: Boolean) = when (moduleType) {
    ModuleType.TIME_SPENT -> copy(isTimeSpentEnabled = enabled)
    ModuleType.READING_INSIGHTS -> copy(isReadingInsightsEnabled = enabled)
    ModuleType.EDITING_INSIGHTS -> copy(isEditingInsightsEnabled = enabled)
    ModuleType.IMPACT -> copy(isImpactEnabled = enabled)
    ModuleType.GAMES -> copy(isGamesEnabled = enabled)
    ModuleType.DONATIONS -> copy(isDonationsEnabled = enabled)
    ModuleType.TIMELINE -> copy(isTimelineEnabled = enabled)
}

fun ActivityTabModules.areAllModulesEnabled(): Boolean {
    return ModuleType.entries.all { this.isModuleEnabled(it) }
}

fun ActivityTabModules.areAllModulesDisabled(): Boolean {
    return ModuleType.entries.none { this.isModuleEnabled(it) }
}

private fun submitModuleToggleEvent(moduleType: ModuleType, isEnabled: Boolean, activeInterface: String = "activity_tab_cutomize", action: String = "customize_click") {
    val data = if (isEnabled) "on" else "off"
    when (moduleType) {
        ModuleType.TIME_SPENT -> ActivityTabEvent.submit(activeInterface = activeInterface, action = action, timeSpent = data)
        ModuleType.READING_INSIGHTS -> ActivityTabEvent.submit(activeInterface = activeInterface, action = action, readingInsight = data)
        ModuleType.EDITING_INSIGHTS -> ActivityTabEvent.submit(activeInterface = activeInterface, action = action, editingInsight = data)
        ModuleType.IMPACT -> ActivityTabEvent.submit(activeInterface = activeInterface, action = action, impact = data)
        ModuleType.GAMES -> ActivityTabEvent.submit(activeInterface = activeInterface, action = action, games = data)
        ModuleType.DONATIONS -> ActivityTabEvent.submit(activeInterface = activeInterface, action = action, donations = data)
        ModuleType.TIMELINE -> ActivityTabEvent.submit(activeInterface = activeInterface, action = action, timeline = data)
    }
}

@Serializable
data class ActivityTabModules(
    val isTimeSpentEnabled: Boolean = true,
    val isReadingInsightsEnabled: Boolean = true,
    val isEditingInsightsEnabled: Boolean = true,
    val isImpactEnabled: Boolean = true,
    val isGamesEnabled: Boolean = true,
    val isDonationsEnabled: Boolean = false,
    val isTimelineEnabled: Boolean = true,
)

enum class ModuleType(val displayName: Int) {
    TIME_SPENT(R.string.activity_tab_customize_screen_time_spent_switch_title),
    READING_INSIGHTS(R.string.activity_tab_customize_screen_reading_insights_switch_title),
    EDITING_INSIGHTS(R.string.activity_tab_customize_screen_editing_insights_switch_title),
    IMPACT(R.string.activity_tab_customize_screen_impact_switch_title),
    GAMES(R.string.activity_tab_customize_screen_games_switch_title),
    DONATIONS(R.string.activity_tab_customize_screen_donations_switch_title),
    TIMELINE(R.string.activity_tab_customize_screen_timeline_switch_title)
}

@Preview
@Composable
private fun CustomizationScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        CustomizationScreen(
            onBackButtonClick = {}
        )
    }
}
