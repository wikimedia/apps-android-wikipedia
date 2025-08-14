package org.wikipedia.activitytab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState

class ActivityTabFragment : Fragment() {

    private val viewModel: ActivityTabViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Prefs.activityTabRedDotShown = true

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    ActivityTabScreen(
                        userName = AccountUtil.userName,
                        timeSpentState = viewModel.timeSpentState.collectAsState().value,
                    )
                }
            }
        }
    }

    @Composable
    fun ActivityTabScreen(
        userName: String,
        timeSpentState: UiState<Long>
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WikipediaTheme.colors.paperColor,
                                WikipediaTheme.colors.additionColor
                            )
                        )
                    )
            ) {
                Text(
                    text = stringResource(R.string.activity_tab_user_reading, userName),
                    modifier = Modifier.padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = WikipediaTheme.colors.primaryColor
                )
                // All module will have their own state management
                // TimeSpentModule
                TimeSpentModule(
                    timeSpentState = timeSpentState,
                    wikiErrorClickEvents = WikiErrorClickEvents(
                        retryClickListener = {
                            viewModel.loadTimeSpent()
                        }
                    )
                )
                // Monthly insights

                // Categories module

                // impact module

                // Game module

                // other module
            }
        }
    }

    @Composable
    fun ActivityTabScreenPreview() {
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                userName = "User",
                timeSpentState = UiState.Success(123456L)
            )
        }
    }

    // @TODO: error view and handling
    @Composable
    fun ColumnScope.TimeSpentModule(
        modifier: Modifier = Modifier,
        timeSpentState: UiState<Long>,
        wikiErrorClickEvents: WikiErrorClickEvents? = null
    ) {
        Text(
            text = stringResource(R.string.activity_tab_user_reading, AccountUtil.userName),
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(
                    color = WikipediaTheme.colors.additionColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(R.string.activity_tab_on_wikipedia_android).uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = TextUnit(0.8f, TextUnitType.Sp),
                textAlign = TextAlign.Center,
                color = WikipediaTheme.colors.primaryColor
            )
        }
        if (timeSpentState is UiState.Success) {
            Text(
                text = stringResource(R.string.activity_tab_weekly_time_spent_hm, (timeSpentState.data / 3600), (timeSpentState.data % 60)),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally),
                fontSize = 32.sp,
                fontWeight = FontWeight.W500,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ComposeColors.Red700,
                            ComposeColors.Orange500,
                            ComposeColors.Yellow500,
                            ComposeColors.Blue300
                        )
                    )
                ),
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = "Time spent reading this week",
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.W500,
                textAlign = TextAlign.Center,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    }

    companion object {
        fun newInstance(): ActivityTabFragment {
            return ActivityTabFragment().apply {
                arguments = Bundle().apply {
                    // TODO
                }
            }
        }
    }
}
