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
    onExit: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = HomeFeedSettingsDestination.Root,
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
                onWhatsDrivingFeedClick = { navController.navigate(HomeFeedSettingsDestination.FeedConfiguration) }
            )
        }

        composable<HomeFeedSettingsDestination.CommunityModuleScreen> {
            CommunityModulesScreen(
                onBack = { navController.navigateUp() }
            )
        }

        composable<HomeFeedSettingsDestination.ForYouModuleScreen> {
            ForYouModulesScreen(
                onBack = { navController.navigateUp() },
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
