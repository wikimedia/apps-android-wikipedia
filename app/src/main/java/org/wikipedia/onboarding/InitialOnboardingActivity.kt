package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.AppTextButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs

class InitialOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                InitialOnboardingScreen(
                    startDestination = if (Prefs.isInitialOnboardingEnabled) Intro else PersonalizationSetup,
                    onFinish = {
                        Prefs.isInitialOnboardingEnabled = false
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val RESULT_LANGUAGE_CHANGED = 1
        fun newIntent(context: Context): Intent {
            return Intent(context, InitialOnboardingActivity::class.java)
        }
    }
}

@Composable
fun InitialOnboardingScreen(
    modifier: Modifier = Modifier,
    navHostController: NavHostController = rememberNavController(),
    startDestination: OnboardingRoute,
    onFinish: () -> Unit
) {
    NavHost(
        navController = navHostController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Intro> {
            // TODO: replace with actual screen
            IntroScreen(onClick = onFinish)
            // on click to navigate to next screen, for now just finish the onboarding flow
            // navHostController.navigate(PersonalizationSetup)
        }

        composable<PersonalizationSetup> {
            // TODO: replace with actual screen
            // we can create a viewModel for this screen here and pass it down which can be shared
        }
    }
}

// TODO: remove later with actual screen
@Composable
fun IntroScreen(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppTextButton (
            onClick = onClick
        ) {
            Text("Skip", color = WikipediaTheme.colors.progressiveColor)
        }
    }
}

// TODO: we can also separate intro into world knowledge intro and data privacy
sealed interface OnboardingRoute
@Serializable object Intro : OnboardingRoute
@Serializable object PersonalizationSetup : OnboardingRoute
