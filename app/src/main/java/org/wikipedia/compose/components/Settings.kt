package org.wikipedia.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.homefeed.CommunityModuleType
import org.wikipedia.settings.homefeed.ForYouModuleType
import org.wikipedia.theme.Theme

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onSubtitleLinkClick: ((String) -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
            if (subtitle != null) {
                HtmlText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.padding(top = 4.dp),
                    linkInteractionListener = { link ->
                        val url = (link as LinkAnnotation.Url).url
                        onSubtitleLinkClick?.invoke(url)
                    },
                    linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = WikipediaTheme.colors.progressiveColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.25.sp
                        )
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = WikipediaTheme.colors.progressiveColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun ToggleListScreen(
    title: String,
    description: String,
    modules: List<ToggleSettingItem>,
    hiddenModules: Set<String>,
    onToggle: (key: String, isVisible: Boolean) -> Unit,
    onSubtitleLinkClick: ((href: String) -> Unit)? = null,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = title,
                onNavigationClick = onBack,
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.primaryColor
            )

            modules.forEach { module ->
                val isVisible = module.key !in hiddenModules
                SettingsRow(
                    title = stringResource(module.title),
                    subtitle = stringResource(module.subtitle),
                    trailingContent = {
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { newChecked ->
                                onToggle(module.key, newChecked)
                            },
                            colors = SwitchDefaults.colors(
                                uncheckedTrackColor = WikipediaTheme.colors.paperColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                                checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                                checkedThumbColor = WikipediaTheme.colors.paperColor,
                            ),
                        )
                    },
                    onSubtitleLinkClick = { href ->
                        onSubtitleLinkClick?.invoke(href)
                    }
                )
            }
        }
    }
}

data class ToggleSettingItem(
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
    val key: String
)

@Preview
@Composable
private fun SettingsSectionPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SettingsSection(
            title = "For you"
        ) {
            Column {
                SettingsRow(
                    title = "Modules",
                    subtitle = "Turn on or off For You sections"
                )
                SettingsRow(
                    title = "Notifications",
                    subtitle = "Manage your notification preferences for home feed updates",
                    trailingContent = {
                        Switch(
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SettingsRow(
            title = "Modules",
            subtitle = "Manage the modules that appear on your home feed"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowWithSwitchPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SettingsRow(
            title = "Modules",
            subtitle = "Manage the modules that appear on your home feed",
            trailingContent = {
                Switch(
                    checked = true,
                    onCheckedChange = { /* Handle switch state change */ }
                )
            }
        )
    }
}

@Preview
@Composable
private fun ToggleListForYouScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ToggleListScreen(
            title = "For you",
            description = stringResource(R.string.home_feed_settings_for_you_modules_description),
            modules = ForYouModuleType.entries(),
            hiddenModules = setOf(ForYouModuleType.BECAUSE_YOU_READ.name),
            onToggle = { _, _ -> },
            onBack = { },
        )
    }
}

@Preview
@Composable
private fun ToggleListCommunityScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ToggleListScreen(
            title = "Community",
            description = stringResource(R.string.home_feed_settings_community_modules_description),
            modules = CommunityModuleType.entries(),
            hiddenModules = setOf(CommunityModuleType.FEATURED_IMAGE.name, CommunityModuleType.ON_THIS_DAY.name),
            onToggle = { _, _ -> },
            onBack = { },
        )
    }
}
