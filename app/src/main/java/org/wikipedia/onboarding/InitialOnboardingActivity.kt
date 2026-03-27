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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.AppTextButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.onboarding.personalization.PersonalizationScreen
import org.wikipedia.settings.Prefs

class InitialOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                InitialOnboardingScreen(
                    isNewUser = Prefs.isInitialOnboardingEnabled,
                    onFinish = {
                        // Prefs.isInitialOnboardingEnabled = false
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
    isNewUser: Boolean,
    onFinish: () -> Unit
) {
    var showInterestOnboarding by remember { mutableStateOf(false) }
    var showIntroScreen by remember { mutableStateOf(isNewUser) }
    if (showIntroScreen) {
        IntroScreen(onClick = {
            showInterestOnboarding = true
        })
    }

    if (showInterestOnboarding) {
        // Personalization Screen (interest selection + content preference + language)
        PersonalizationScreen()
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
            Text("Next", color = WikipediaTheme.colors.progressiveColor)
        }
    }
}
