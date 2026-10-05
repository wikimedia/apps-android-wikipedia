package org.wikipedia.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import java.io.IOException

/**
 * Shown behind the browser in which the user logs in, and while we finish logging in afterwards.
 */
@Composable
fun LoginScreen(
    error: Throwable?,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (error == null) {
            CircularProgressIndicator(color = WikipediaTheme.colors.progressiveColor)
        } else {
            WikiErrorView(
                caught = error,
                errorClickEvents = WikiErrorClickEvents(retryClickListener = onRetryClick, backClickListener = onBackClick),
                retryForGenericError = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenInProgressPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LoginScreen(error = null, onRetryClick = {}, onBackClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        LoginScreen(error = IOException("The authorization server is unavailable."), onRetryClick = {}, onBackClick = {})
    }
}
