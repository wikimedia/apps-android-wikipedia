package org.wikipedia.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.InstallWidgetScreen
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs

class SearchWidgetInstallDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    private val instrument = TestKitchenAdapter.client.getInstrument("apps-widgetsearch")
        .setDefaultActionSource("widget_search_install")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        instrument.submitInteraction(action = "impression")
        Prefs.searchWidgetInstallPromptShown = true

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    InstallWidgetScreen(
                        title = stringResource(R.string.search_widget_install_prompt_title),
                        message = stringResource(R.string.search_widget_install_prompt_message),
                        onCloseClick = {
                            instrument.submitInteraction(action = "click", elementId = "install_close")
                            dismiss()
                        },
                        previewContent = {
                            Image(
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillWidth,
                                painter = painterResource(id = R.drawable.reading_challenge_blur_background),
                                contentDescription = null
                            )
                            Image(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(vertical = 24.dp),
                                contentScale = ContentScale.FillWidth,
                                painter = painterResource(id = R.drawable.widget_search),
                                contentDescription = null
                            )
                        },
                        bottomContent = {
                            val pinSupported = pinWidgetSupported()
                            AppButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (pinSupported) {
                                        instrument.submitInteraction(action = "click", elementId = "install_add")
                                        requestToPinWidget(requireContext())
                                    }
                                    dismiss()
                                }
                            ) {
                                Text(
                                    text = stringResource(
                                        if (pinSupported) R.string.search_widget_install_prompt_add
                                        else R.string.search_widget_install_prompt_got_it
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun pinWidgetSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                AppWidgetManager.getInstance(requireContext()).isRequestPinAppWidgetSupported
    }

    private fun requestToPinWidget(context: Context) {
        if (pinWidgetSupported()) {
            AppWidgetManager.getInstance(context).requestPinAppWidget(
                ComponentName(context, WidgetProviderSearch::class.java), null, null
            )
        }
    }

    companion object {
        fun isWidgetInstalled(): Boolean {
            val context = WikipediaApp.instance
            return AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, WidgetProviderSearch::class.java)
            ).isNotEmpty()
        }
    }
}
