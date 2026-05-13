package org.wikipedia.settings.homefeed

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun HomeFeedSettingsNavHost(
    navController: NavHostController,
    startDestination: HomeFeedSettingsStartDestination,
    onExit: () -> Unit
) {
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
                onCommunityModulesClick = { navController.navigate(HomeFeedSettingsDestination.CommunityModuleScreen) },
                onForYouModulesClick = { navController.navigate(HomeFeedSettingsDestination.ForYouModuleScreen) },
                onFeedConfigurationClick = { navController.navigate(HomeFeedSettingsDestination.FeedConfiguration) }
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
