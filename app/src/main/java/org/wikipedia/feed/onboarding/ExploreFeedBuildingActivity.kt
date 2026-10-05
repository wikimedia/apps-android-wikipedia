package org.wikipedia.feed.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFileSource
import app.rive.RivePointerInputMode
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import kotlinx.coroutines.delay
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import app.rive.runtime.kotlin.core.Rive as RiveRuntime

class ExploreFeedBuildingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                ExploreFeedBuildingScreen(
                    onFinished = {
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ExploreFeedBuildingActivity::class.java)
        }
    }
}

@Composable
fun ExploreFeedBuildingScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onFinished()
    }

    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.explore_feed_building_text),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                color = WikipediaTheme.colors.primaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.height(185.dp)
            ) {
                HomeFeedAnimation(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HomeFeedAnimation(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isRiveRuntimeLoaded = remember { runCatching { RiveRuntime.init(context) }.isSuccess }
    if (!isRiveRuntimeLoaded) {
        return
    }
    val riveWorker = rememberRiveWorkerOrNull() ?: return
    val riveFile = rememberRiveFile(RiveFileSource.RawRes.from(R.raw.home_feed_animation), riveWorker)
    if (riveFile is Result.Success) {
        Rive(
            file = riveFile.value,
            modifier = modifier,
            pointerInputMode = RivePointerInputMode.Observe
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreFeedBuildingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ExploreFeedBuildingScreen(
            onFinished = { }
        )
    }
}
