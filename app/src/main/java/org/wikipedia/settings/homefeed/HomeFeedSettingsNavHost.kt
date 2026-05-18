package org.wikipedia.settings.homefeed

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.wikipedia.extensions.instrument

@Composable
fun HomeFeedSettingsNavHost(
    navController: NavHostController,
    startDestination: HomeFeedSettingsStartDestination,
    onExit: () -> Unit
) {
    val context = navController.context
    val startRoute = when (startDestination) {
        HomeFeedSettingsStartDestination.ROOT -> HomeFeedSettingsDestination.Root
        HomeFeedSettingsStartDestination.COMMUNITY_MODULES -> HomeFeedSettingsDestination.CommunityModuleScreen
        HomeFeedSettingsStartDestination.FOR_YOU_MODULES -> HomeFeedSettingsDestination.ForYouModuleScreen
    }
    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<HomeFeedSettingsDestination.Root> {
            HomeFeedSettingsScreen(
                onBackClick = {
                    onExit()
                },
                onCommunityModulesClick = {
                    context.instrument?.submitInteraction("click", elementId = "feed_modules_community")
                    navController.navigate(HomeFeedSettingsDestination.CommunityModuleScreen)
                },
                onForYouModulesClick = {
                    context.instrument?.submitInteraction("click", elementId = "feed_modules_for_you")
                    navController.navigate(HomeFeedSettingsDestination.ForYouModuleScreen)
                },
                onFeedConfigurationClick = {
                    context.instrument?.submitInteraction("click", elementId = "feed_data_info")
                    navController.navigate(HomeFeedSettingsDestination.FeedConfiguration)
                }
            )
        }

        composable<HomeFeedSettingsDestination.CommunityModuleScreen> {
            CommunityModulesScreen(
                onBack = { if (!navController.navigateUp()) onExit() }
            )
        }

        composable<HomeFeedSettingsDestination.ForYouModuleScreen> {
            ForYouModulesScreen(
                onBack = { if (!navController.navigateUp()) onExit() },
                navigateToFeedConfigurationScreen = {
                    context.instrument?.submitInteraction("click", actionSubtype = "feed_for_you", elementId = "feed_data_info")
                    navController.navigate(HomeFeedSettingsDestination.FeedConfiguration)
                }
            )
        }

        composable<HomeFeedSettingsDestination.FeedConfiguration> {
            FeedConfigurationScreen(
                onBack = { navController.navigateUp() }
            )
        }
    }
}
