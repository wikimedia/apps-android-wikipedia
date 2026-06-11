package org.wikipedia.settings.homefeed

import android.app.Activity
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.wikipedia.Constants
import org.wikipedia.extensions.instrument
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.places.PlacesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.FeedbackUtil

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
    val customizeInterestsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK || it.resultCode == PersonalizationActivity.RESULT_INTERESTS_UPDATED) {
            val context = context as Activity
            if (it.resultCode == PersonalizationActivity.RESULT_INTERESTS_UPDATED) {
                FeedbackUtil.showMessage(context, org.wikipedia.R.string.home_feed_settings_feed_configuration_interests_updated_message)
            }
        }
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
            FeedConfigurationScreen (
                onBack = { if (!navController.navigateUp()) onExit() },
                onInterestsClick = {
                    context.instrument?.submitInteraction("click", elementId = "customize_feed")
                    customizeInterestsLauncher.launch(PersonalizationActivity.newIntent(context, showInterestsOnly = true))
                },
                onLocationClick = {
                    context.instrument?.submitInteraction("click", elementId = "update_location")
                    context.startActivity(PlacesActivity.newIntent(context))
                },
                onReadingHistoryClick = {
                    context.instrument?.submitInteraction("click", elementId = "reading_history")
                    context.startActivity(
                        MainActivity.newIntent(context)
                            .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.SEARCH.code())
                    )
                },
                onLanguagesClick = {
                    context.instrument?.submitInteraction("click", elementId = "languages")
                    context.startActivity(WikipediaLanguagesActivity.newIntent(context, Constants.InvokeSource.SETTINGS))
                },
            )
        }
    }
}
