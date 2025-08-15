package org.wikipedia.activitytab

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                        donationUiState = viewModel.donationUiState.collectAsState().value,
                        readingHistoryState = viewModel.readingHistoryState.collectAsState().value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReadingHistory()
    }

    @Composable
    fun ActivityTabScreen(
        userName: String,
        donationUiState: UiState<String?>,
        readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(WikipediaTheme.colors.paperColor)
            ) {
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
                    ReadingHistoryModule(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        userName = userName,
                        readingHistoryState = readingHistoryState,
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            retryClickListener = {
                                viewModel.loadReadingHistory()
                            }
                        )
                    )

                    // Categories module
                }

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
                    if (donationUiState is UiState.Success) {
                        // TODO: default is off. Handle this when building the configuration screen.
                        DonationModule(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 16.dp),
                            uiState = donationUiState,
                            wikiErrorClickEvents = WikiErrorClickEvents(
                                retryClickListener = {
                                    viewModel.loadDonationResults()
                                }
                            ),
                            onClick = {
                                (requireActivity() as? BaseActivity)?.launchDonateDialog(campaignId = ActivityTabViewModel.CAMPAIGN_ID)
                            }
                        )
                    }
                }

                // --- new column ---

                // impact module

                // game module

                // donation module

                // --- new column ---

                // timeline module
            }
        }
    }

    // @TODO: error view and handling
    @Composable
    fun ReadingHistoryModule(
        modifier: Modifier,
        userName: String,
        readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>,
        wikiErrorClickEvents: WikiErrorClickEvents? = null
    ) {
        Text(
            text = stringResource(R.string.activity_tab_user_reading, userName),
            modifier = modifier
                .padding(top = 16.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )
        Box(
            modifier = modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(
                    color = WikipediaTheme.colors.additionColor,
                    shape = RoundedCornerShape(8.dp)
                )
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
        if (readingHistoryState is UiState.Success) {
            val readingHistory = readingHistoryState.data
            val todayDate = LocalDate.now()

            Text(
                text = stringResource(
                    R.string.activity_tab_weekly_time_spent_hm,
                    (readingHistory.timeSpentThisWeek / 3600),
                    (readingHistory.timeSpentThisWeek % 60)
                ),
                modifier = modifier
                    .padding(top = 12.dp),
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
                text = stringResource(R.string.activity_tab_weekly_time_spent),
                modifier = modifier
                    .padding(top = 8.dp, bottom = 16.dp),
                fontWeight = FontWeight.W500,
                textAlign = TextAlign.Center,
                color = WikipediaTheme.colors.primaryColor
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clickable {
                        // TODO
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WikipediaTheme.colors.paperColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = WikipediaTheme.colors.borderColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = modifier.fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(
                        modifier = modifier.weight(1f)
                    ) {
                        Row(
                            modifier = modifier.fillMaxWidth()
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(R.drawable.ic_newsstand_24),
                                tint = WikipediaTheme.colors.primaryColor,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.activity_tab_monthly_articles_read),
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = WikipediaTheme.colors.primaryColor
                            )
                        }
                    }
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                        tint = WikipediaTheme.colors.secondaryColor,
                        contentDescription = null
                    )
                }
                if (readingHistory.lastArticleReadTime != null) {
                    Text(
                        text = if (todayDate == readingHistory.lastArticleReadTime.toLocalDate())
                            readingHistory.lastArticleReadTime
                                .format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "hhmm a")))
                        else
                            readingHistory.lastArticleReadTime
                                .format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMM d"))),
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                        fontSize = 12.sp,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
                Row(
                    modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = readingHistory.articlesReadThisMonth.toString(),
                        modifier = Modifier.padding(start = 16.dp).align(Alignment.Bottom),
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.padding(end = 16.dp)
                            .size(width = 80.dp, height = 48.dp)
                            .background(
                                color = WikipediaTheme.colors.additionColor,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                    .clickable {
                        // TODO
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WikipediaTheme.colors.paperColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = WikipediaTheme.colors.borderColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = modifier.fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(
                        modifier = modifier.weight(1f)
                    ) {
                        Row(
                            modifier = modifier.fillMaxWidth()
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(R.drawable.ic_bookmark_border_white_24dp),
                                tint = WikipediaTheme.colors.primaryColor,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.activity_tab_monthly_articles_saved),
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = WikipediaTheme.colors.primaryColor
                            )
                        }
                    }
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                        tint = WikipediaTheme.colors.secondaryColor,
                        contentDescription = null
                    )
                }
                if (readingHistory.lastArticleSavedTime != null) {
                    Text(
                        text = if (todayDate == readingHistory.lastArticleSavedTime.toLocalDate())
                            readingHistory.lastArticleSavedTime
                                .format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "hhmm a")))
                        else
                            readingHistory.lastArticleSavedTime
                                .format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMM d"))),
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                        fontSize = 12.sp,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
                Row(
                    modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = readingHistory.articlesSavedThisMonth.toString(),
                        modifier = Modifier.padding(start = 16.dp).align(Alignment.Bottom),
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        val itemsToShow = if (readingHistory.articlesSaved.size <= 4) readingHistory.articlesSaved.size else 3
                        val showOverflowItem = readingHistory.articlesSaved.size > 4

                        for (i in 0 until itemsToShow) {
                            val url = readingHistory.articlesSaved[i].thumbUrl
                            if (url == null) {
                                Box(
                                    modifier = Modifier.padding(start = 4.dp).size(38.dp)
                                        .background(
                                            color = Color.White,
                                            shape = RoundedCornerShape(19.dp)
                                        ).border(
                                            0.5.dp,
                                            WikipediaTheme.colors.borderColor,
                                            RoundedCornerShape(19.dp))
                                ) {
                                    Icon(
                                        modifier = Modifier.size(24.dp).align(Alignment.Center),
                                        painter = painterResource(R.drawable.ic_wikipedia_b),
                                        tint = WikipediaTheme.colors.primaryColor,
                                        contentDescription = null
                                    )
                                }
                            } else {
                                val request = ImageService.getRequest(LocalContext.current, url = url)
                                AsyncImage(
                                    model = request,
                                    placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                                    error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp).size(38.dp)
                                        .clip(RoundedCornerShape(19.dp))
                                )
                            }
                        }

                        if (showOverflowItem) {
                            Box(
                                modifier = Modifier.padding(start = 4.dp).size(38.dp)
                                    .background(
                                        color = WikipediaTheme.colors.placeholderColor,
                                        shape = RoundedCornerShape(19.dp)
                                    )
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "+%d", readingHistory.articlesSavedThisMonth - 3),
                                    modifier = Modifier.align(Alignment.Center),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (readingHistory.articlesReadThisMonth == 0L && readingHistory.articlesSavedThisMonth == 0L) {
                Text(
                    text = stringResource(R.string.activity_tab_discover_encourage),
                    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = WikipediaTheme.colors.primaryColor
                )
                Button(
                    modifier = modifier.padding(top = 8.dp, bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor,
                        contentColor = WikipediaTheme.colors.paperColor,
                    ),
                    onClick = {
                        // TODO
                    },
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_globe),
                        tint = WikipediaTheme.colors.paperColor,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = stringResource(R.string.activity_tab_explore_wikipedia)
                    )
                }
            }
        }
    }

    @Preview
    @Composable
    fun ActivityTabScreenPreview() {
        val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                userName = "User",
                donationUiState = UiState.Success("2023-10-01T12:00:00"),
                readingHistoryState = UiState.Success(ActivityTabViewModel.ReadingHistory(
                    timeSpentThisWeek = 12345,
                    articlesReadThisMonth = 123,
                    lastArticleReadTime = LocalDateTime.now(),
                    articlesReadByWeek = listOf(0, 12, 34, 56),
                    articlesSavedThisMonth = 23,
                    lastArticleSavedTime = LocalDateTime.of(2025, 6, 1, 12, 30),
                    articlesSaved = listOf(
                        PageTitle(text = "Psychology of art", wiki = site, thumbUrl = "foo.jpg", description = "Study of mental functions and behaviors", displayText = null),
                        PageTitle(text = "Industrial design", wiki = site, thumbUrl = null, description = "Process of design applied to physical products", displayText = null),
                        PageTitle(text = "Dufourspitze", wiki = site, thumbUrl = "foo.jpg", description = "Highest mountain in Switzerland", displayText = null),
                        PageTitle(text = "Barack Obama", wiki = site, thumbUrl = "foo.jpg", description = "President of the United States from 2009 to 2017", displayText = null),
                        PageTitle(text = "Octagon house", wiki = site, thumbUrl = "foo.jpg", description = "North American house style briefly popular in the 1850s", displayText = null)
                    )
                ))
            )
        }
    }

    @Preview
    @Composable
    fun ActivityTabScreenEmptyPreview() {
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                userName = "User",
                donationUiState = UiState.Success("2023-10-01T12:00:00"),
                readingHistoryState = UiState.Success(ActivityTabViewModel.ReadingHistory(
                    timeSpentThisWeek = 0,
                    articlesReadThisMonth = 0,
                    lastArticleReadTime = null,
                    articlesReadByWeek = listOf(0, 0, 0, 0),
                    articlesSavedThisMonth = 0,
                    lastArticleSavedTime = null,
                    articlesSaved = emptyList()
                ))
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
