package org.wikipedia.widgets.readingchallenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.theme.Theme

class ReadingChallengeRewardDialog : ExtendedBottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    RewardScreen(
                        modifier = Modifier.height(500.dp),
                        onCloseClick = {
                            dismiss()
                        },
                        onNavigateClick = {
                            // TODO
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    @Composable
    fun RewardScreen(
        modifier: Modifier = Modifier,
        onCloseClick: () -> Unit,
        onNavigateClick: () -> Unit
    ) {
        Scaffold(
            modifier = modifier.safeDrawingPadding(),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 16.dp),
                        text = stringResource(R.string.reading_challenge_widget_collect_your_prize_button),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = WikipediaTheme.colors.primaryColor
                    )
                    IconButton(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-6).dp),
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

                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .height(202.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.FillWidth,
                        painter = painterResource(id = R.drawable.reading_challenge_reward),
                        contentDescription = null
                    )
                }

                Text(
                    modifier = Modifier
                        .padding(bottom = 16.dp),
                    text = stringResource(R.string.reading_challenge_widget_reward_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = WikipediaTheme.colors.primaryColor
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.reading_challenge_widget_reward_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.secondaryColor
                )
                AppButton(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    onClick = onNavigateClick
                ) {
                    Text(
                        stringResource(R.string.reading_challenge_widget_reward_button_label)
                    )
                }
            }
        }
    }

    @Preview
    @Composable
    private fun RewardScreenPreview() {
        BaseTheme(
            currentTheme = Theme.LIGHT
        ) {
            RewardScreen(
                modifier = Modifier.height(450.dp),
                onCloseClick = {},
                onNavigateClick = {}
            )
        }
    }

    companion object {
        fun newInstance(): ReadingChallengeInstallWidgetDialog {
            return ReadingChallengeInstallWidgetDialog()
        }
    }
}
