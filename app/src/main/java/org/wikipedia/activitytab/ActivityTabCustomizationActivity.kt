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
    ModuleType.READING_HISTORY -> isReadingHistoryEnabled
    ModuleType.IMPACT -> isImpactEnabled
    ModuleType.GAMES -> isGamesEnabled
    ModuleType.DONATIONS -> isDonationsEnabled
    ModuleType.TIMELINE -> isTimelineEnabled
}

fun ActivityTabModules.setModuleEnabled(moduleType: ModuleType, enabled: Boolean) = when (moduleType) {
    ModuleType.READING_HISTORY -> copy(isReadingHistoryEnabled = enabled)
    ModuleType.IMPACT -> copy(isImpactEnabled = enabled)
    ModuleType.GAMES -> copy(isGamesEnabled = enabled)
    ModuleType.DONATIONS -> copy(isDonationsEnabled = enabled)
    ModuleType.TIMELINE -> copy(isTimelineEnabled = enabled)
}

@Serializable
data class ActivityTabModules(
    val isReadingHistoryEnabled: Boolean = true,
    val isImpactEnabled: Boolean = true,
    val isGamesEnabled: Boolean = true,
    val isDonationsEnabled: Boolean = false,
    val isTimelineEnabled: Boolean = true,
)

enum class ModuleType(val displayName: Int) {
    READING_HISTORY(R.string.activity_tab_customize_screen_reading_history_switch_title),
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
