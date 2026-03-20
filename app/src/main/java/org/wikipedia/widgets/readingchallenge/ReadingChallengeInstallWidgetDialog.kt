package org.wikipedia.widgets.readingchallenge

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.R
import org.wikipedia.compose.components.TwoButtonBottomBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme

class ReadingChallengeInstallWidgetDialog : ExtendedBottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    InstallWidgetScreen(
                        pinToWidgetSupported = false,
                        onCloseClick = {
                            dismissDialog()
                        },
                        onGotItClick = {
                            dismissDialog()
                        },
                        onAddClick = {
                            requestToPinWidget(requireContext())
                            dismissDialog()
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

    private fun pinWidgetSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                AppWidgetManager.getInstance(requireContext()).isRequestPinAppWidgetSupported
    }

    private fun requestToPinWidget(context: Context) {
        if (pinWidgetSupported()) {
            val successCallback = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, ReadingChallengeWidgetReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, ReadingChallengeWidgetReceiver::class.java), null, successCallback)
        }
    }

    private fun dismissDialog() {
        Prefs.readingChallengeInstallPromptShown = true
        dismiss()
    }

    @Composable
    fun InstallWidgetScreen(
        pinToWidgetSupported: Boolean,
        onCloseClick: () -> Unit,
        onGotItClick: () -> Unit,
        onAddClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    text = stringResource(R.string.reading_challenge_install_prompt_title),
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

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.reading_challenge_install_prompt_message),
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.secondaryColor
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .height(190.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.FillWidth,
                    painter = painterResource(id = R.drawable.reading_challenge_blur_background),
                    contentDescription = null
                )
                Image(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 24.dp)
                        .dropShadow(
                            shape = RoundedCornerShape(12.dp),
                            shadow = Shadow(
                                color = WikipediaTheme.colors.overlayColor,
                                radius = 12.dp,
                                offset = DpOffset(0.dp, 12.dp)
                            )
                        ),
                    painter = painterResource(id = R.drawable.reading_challenge_widget_example),
                    contentDescription = null
                )
            }

            if (pinToWidgetSupported) {
                TwoButtonBottomBar(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 16.dp),
                    primaryButtonText = stringResource(R.string.reading_challenge_install_prompt_add),
                    secondaryButtonText = stringResource(R.string.reading_challenge_install_prompt_got_it),
                    onPrimaryOnClick = onAddClick,
                    onSecondaryOnClick = onGotItClick
                )
            } else {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor
                    ),
                    onClick = onGotItClick
                ) {
                    AnimatedContent(
                        targetState = stringResource(R.string.reading_challenge_install_prompt_got_it),
                    ) { targetText ->
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.labelLarge,
                            color = WikipediaTheme.colors.paperColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
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
                pinToWidgetSupported = false,
                onCloseClick = {},
                onAddClick = {},
                onGotItClick = {}
            )
        }
    }
}
