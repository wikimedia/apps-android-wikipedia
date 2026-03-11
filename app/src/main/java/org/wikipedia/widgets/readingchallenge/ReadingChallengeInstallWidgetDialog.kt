package org.wikipedia.widgets.readingchallenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.components.TwoButtonBottomBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.theme.Theme

class ReadingChallengeInstallWidgetDialog : ExtendedBottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    InstallWidgetScreen(
                        modifier = Modifier.fillMaxSize(),
                        onCloseClick = {
                            dismiss()
                        },
                        onGotItClick = {
                            dismiss()
                        },
                        onAddClick = {
                            // TODO: add "Add widget" functionality here
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun InstallWidgetScreen(
        modifier: Modifier = Modifier,
        onCloseClick: () -> Unit,
        onGotItClick: () -> Unit,
        onAddClick: () -> Unit
    ) {
        Scaffold(
            modifier = modifier
                .safeDrawingPadding(),
            containerColor = WikipediaTheme.colors.paperColor,
            bottomBar = {
                TwoButtonBottomBar(
                    primaryButtonText = stringResource(R.string.reading_challenge_install_prompt_add),
                    secondaryButtonText = stringResource(R.string.reading_challenge_install_prompt_got_it),
                    onPrimaryOnClick = onAddClick,
                    onSecondaryOnClick = onGotItClick
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 16.dp),
                        text = stringResource(R.string.reading_challenge_install_prompt_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                        color = WikipediaTheme.colors.primaryColor
                    )

                    IconButton(
                        onClick = {
                            onCloseClick()
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_close_black_24dp),
                            contentDescription = stringResource(R.string.dialog_close_description),
                            tint = WikipediaTheme.colors.primaryColor
                        )
                    }
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.reading_challenge_install_prompt_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.secondaryColor
                )

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.yir_puzzle_pinch) // TODO: add the correct image here
                        .allowHardware(false)
                        .build(),
                    success = { SubcomposeAsyncImageContent() },
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @Preview
    @Composable
    private fun InstallWidgetScreenPreview() {
        BaseTheme(
            currentTheme = Theme.LIGHT
        ) {
            InstallWidgetScreen(
                onCloseClick = {},
                onAddClick = {},
                onGotItClick = {}
            )
        }
    }

    companion object {
        fun newInstance(): ReadingChallengeInstallWidgetDialog {
            return ReadingChallengeInstallWidgetDialog()
        }
    }
}
