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
    onExit: () -> Unit
) {
    val context = navController.context
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
                onBack = { navController.navigateUp() }
            )
        }

        composable<HomeFeedSettingsDestination.ForYouModuleScreen> {
            ForYouModulesScreen(
                onBack = { navController.navigateUp() },
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
