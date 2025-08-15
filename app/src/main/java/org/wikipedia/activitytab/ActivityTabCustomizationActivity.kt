package org.wikipedia.activitytab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
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
                title = "Customize",
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.backgroundColor,
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(vertical = 24.dp)
            ) {
                items(ModuleType.entries) { moduleType ->
                    CustomizationScreenSwitch(
                        isChecked = currentModules.isModuleEnabled(moduleType),
                        title = moduleType.displayName,
                        onCheckedChange = { isChecked ->
                            currentModules = currentModules.setModuleEnabled(moduleType, isChecked)
                            Prefs.activityTabModules = currentModules
                        }
                    )
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
    HorizontalDivider(
        color = WikipediaTheme.colors.borderColor
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

enum class ModuleType(val displayName: String) {
    READING_HISTORY("Reading History"),
    IMPACT("Impact"),
    GAMES("Games"),
    DONATIONS("Donations"),
    TIMELINE("Timeline")
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
