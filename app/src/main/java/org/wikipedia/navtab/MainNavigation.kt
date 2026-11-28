package org.wikipedia.navtab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

class MainNavigationState(initialTheme: Theme = WikipediaApp.instance.currentTheme) {
    var selectedTab by mutableStateOf(NavTab.HOME)
    var theme by mutableStateOf(initialTheme)
    val tabsWithDot = mutableStateMapOf<NavTab, Boolean>()
    var tooltip by mutableStateOf<NavTabTooltip?>(null)

    fun setDot(tab: NavTab, enabled: Boolean) { tabsWithDot[tab] = enabled }
}

data class NavTabTooltip(val tab: NavTab, val text: String, val showDismissButton: Boolean)

enum class MainNavigationType { BAR, RAIL }

@Composable
fun rememberMainNavigationType(): MainNavigationType {
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return if (windowWidth >= 600.dp) MainNavigationType.RAIL else MainNavigationType.BAR
}

@Composable
fun MainNavigation(
    type: MainNavigationType,
    state: MainNavigationState,
    onTabClick: (NavTab) -> Unit
) {
    if (rememberMainNavigationType() != type) return
    BaseTheme(currentTheme = state.theme) {
        when (type) {
            MainNavigationType.BAR -> MainNavigationBar(state, onTabClick)
            MainNavigationType.RAIL -> MainNavigationRail(state, onTabClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavigationBar(
    state: MainNavigationState,
    onTabClick: (NavTab) -> Unit
) {
    val colors = WikipediaTheme.colors
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = colors.borderColor)
        NavigationBar(
            containerColor = colors.paperColor,
            windowInsets = WindowInsets(0)
        ) {
            NavTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = state.selectedTab == tab,
                    onClick = { onTabClick(tab) },
                    icon = {
                        NavTabTooltipBox(tab = tab, state = state, position = TooltipAnchorPosition.Above) {
                            NavTabIcon(tab = tab, state = state)
                        }
                    },
                    label = { NavTabLabel(tab = tab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.progressiveColor,
                        selectedTextColor = colors.progressiveColor,
                        unselectedIconColor = colors.placeholderColor,
                        unselectedTextColor = colors.placeholderColor,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavigationRail(
   state: MainNavigationState,
   onTabClick: (NavTab) -> Unit
) {
    val colors = WikipediaTheme.colors
    Row {
        NavigationRail(
            containerColor = colors.paperColor,
            windowInsets = WindowInsets(0)
        ) {
            NavTab.entries.forEach { tab ->
                NavTabTooltipBox(tab = tab, state = state, position = TooltipAnchorPosition.End) {
                    NavigationRailItem(
                        selected = state.selectedTab == tab,
                        onClick = { onTabClick(tab) },
                        icon = { NavTabIcon(tab = tab, state = state) },
                        label = { NavTabLabel(tab = tab) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = colors.progressiveColor,
                            selectedTextColor = colors.progressiveColor,
                            unselectedIconColor = colors.placeholderColor,
                            unselectedTextColor = colors.placeholderColor,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
        VerticalDivider(thickness = 0.5.dp, color = colors.borderColor)
    }
}

@Composable
private fun NavTabIcon(
    tab: NavTab,
    state: MainNavigationState
) {
    BadgedBox(
        badge = {
            if (state.tabsWithDot[tab] == true) {
                Badge(modifier = Modifier.size(6.dp), containerColor = WikipediaTheme.colors.destructiveColor)
            }
        }
    ) {
        Icon(painterResource(if (state.selectedTab == tab) tab.selectedIcon else tab.icon), contentDescription = null)
    }
}

@Composable
private fun NavTabLabel(tab: NavTab) {
    Text(
        text = stringResource(tab.text),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = with(LocalDensity.current) { LocalResources.current.getDimension(R.dimen.bottom_nav_label_text_size).toSp() },
            lineHeight = 12.sp
        ),
        maxLines = 2,
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavTabTooltipBox(
    tab: NavTab,
    state: MainNavigationState,
    position: TooltipAnchorPosition,
    content: @Composable () -> Unit
) {
    val tooltip = state.tooltip?.takeIf { it.tab == tab }
    val tooltipState = rememberTooltipState(isPersistent = true)
    // Keeps the text on screen while the tooltip fades out after state.tooltip is cleared.
    var shownTooltip by remember { mutableStateOf<NavTabTooltip?>(null) }
    LaunchedEffect(tooltip) {
        if (tooltip != null) {
            shownTooltip = tooltip
            // Suspends until state.tooltip changes, which cancels this effect and hides the tooltip.
            tooltipState.show()
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
        tooltip = {
            shownTooltip?.let {
                NavTabTooltipContent(tooltip = it, onDismiss = { state.tooltip = null })
            }
        },
        state = tooltipState,
        onDismissRequest = { state.tooltip = null },
        enableUserInput = false,
        hasAction = shownTooltip?.showDismissButton == true,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipScope.NavTabTooltipContent(
    tooltip: NavTabTooltip,
    onDismiss: () -> Unit
) {
    RichTooltip(
        caretShape = TooltipDefaults.caretShape(),
        shape = RoundedCornerShape(5.dp),
        colors = TooltipDefaults.richTooltipColors(
            containerColor = WikipediaTheme.colors.progressiveColor,
            contentColor = ComposeColors.White
        ),
        shadowElevation = 2.dp,
        action = if (tooltip.showDismissButton) {
            {
                AppButton(
                    onClick = onDismiss,
                    backgroundColor = WikipediaTheme.colors.backgroundColor,
                    contentColor = WikipediaTheme.colors.progressiveColor
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_got_it),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            null
        }
    ) {
        Text(
            text = tooltip.text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Preview(widthDp = 400)
@Composable
private fun MainNavigationBarPreview() {
    val state = MainNavigationState(Theme.LIGHT).apply {
        selectedTab = NavTab.SEARCH
        setDot(NavTab.READING_LISTS, true)
    }
    BaseTheme(currentTheme = state.theme) { MainNavigationBar(state, onTabClick = {}) }
}

@Preview(heightDp = 600)
@Composable
private fun MainNavigationRailPreview() {
    val state = MainNavigationState(Theme.DARK).apply { setDot(NavTab.EDITS, true) }
    BaseTheme(currentTheme = state.theme) { MainNavigationRail(state, onTabClick = {}) }
}
