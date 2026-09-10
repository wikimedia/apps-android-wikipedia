package org.wikipedia.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.InstallWidgetScreen
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.UriUtil

class SemanticSearchInfoDialog: ExtendedBottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SemanticSearchInfoDialogContent(
                    onCloseClick = { dismiss() },
                    onLearnMoreClick = {
                        UriUtil.visitInExternalBrowser(requireContext(), getString(R.string.hybrid_search_info_link_phase2).toUri())
                        dismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SemanticSearchInfoDialogContent(
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit
) {
    BaseTheme {
        InstallWidgetScreen(
            title = stringResource(id = R.string.hybrid_search_info_dialog_title),
            message = stringResource(id = R.string.hybrid_search_info_dialog_message),
            onCloseClick = onCloseClick,
            bottomContent = {
                Spacer(modifier = Modifier.height(24.dp))
                AppButton(
                    onClick = onLearnMoreClick,
                    backgroundColor = WikipediaTheme.colors.backgroundColor,
                    modifier = Modifier.fillMaxWidth(),

                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.hybrid_search_info_dialog_button_text),
                            color = WikipediaTheme.colors.progressiveColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_in_new_black_24px),
                            tint = WikipediaTheme.colors.progressiveColor,
                            contentDescription = null
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SemanticSearchInfoDialogPreview() {
    SemanticSearchInfoDialogContent(
        onCloseClick = {},
        onLearnMoreClick = { }
    )
}
