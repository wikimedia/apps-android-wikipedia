package org.wikipedia.readinglist.recommended

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.R
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.Resource

val sampleItems = listOf(
    "Short description",
    "This is a longer description that will make the card taller",
    "Medium length description here",
    "Another description",
    "Very long description that goes on and on to demonstrate the staggered grid layout working properly",
    "Short",
    "Another medium length description",
    "Brief description",
)


class RecommendedReadingListInterestsFragment : Fragment() {

    private val viewModel: RecommendedReadingListInterestsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    RecommendedReadingListInterestsScreen(
                        uiState = viewModel.uiState.collectAsState().value,
                        onCloseClick = {

                        },
                        onNextClick = {

                        },
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            backClickListener = {
                                requireActivity().finish()
                            },
                            retryClickListener = {

                            }
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(): RecommendedReadingListInterestsFragment {
            return RecommendedReadingListInterestsFragment()
        }
    }
}

@Composable
fun RecommendedReadingListInterestsScreen(
    uiState: Resource<RecommendedReadingListInterestsViewModel.UiState>,
    wikiErrorClickEvents: WikiErrorClickEvents? = null,
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    when (uiState) {
        is Resource.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    color = WikipediaTheme.colors.progressiveColor,
                )
            }
        }

        is Resource.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WikiErrorView(
                    modifier = Modifier
                        .fillMaxWidth(),
                    caught = uiState.throwable,
                    errorClickEvents = wikiErrorClickEvents
                )
            }
        }

        is Resource.Success -> {
            RecommendedReadingListInterestsContent(
                fromSettings = uiState.data.fromSettings,
                onCloseClick = onCloseClick,
                onNextClick = onNextClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendedReadingListInterestsContent(
    fromSettings: Boolean = false,
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .background(WikipediaTheme.colors.paperColor),
        topBar = {
            TopAppBar(
                title = {
                    if (fromSettings) {
                        Text(
                            text = stringResource(id = R.string.recommended_reading_list_settings_updates_base_title),
                            color = WikipediaTheme.colors.primaryColor,
                            style = WikipediaTheme.typography.h1.copy(lineHeight = 24.sp)
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = if (fromSettings) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                        contentDescription = stringResource(id = if (fromSettings) R.string.search_back_button_content_description else R.string.table_close),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onCloseClick)
                            .padding(12.dp),
                        tint = WikipediaTheme.colors.primaryColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WikipediaTheme.colors.paperColor,
                    scrolledContainerColor = WikipediaTheme.colors.paperColor
                )
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        Box(

        ) {
            val borderColor = WikipediaTheme.colors.borderColor

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(120.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 16.dp, end = 16.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                content = {

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth().padding(top = 48.dp, bottom = 4.dp),
                            style = WikipediaTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.primaryColor,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            text = "What are you interested in?"
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        ReadingListInterestSearchCard("Foo")
                    }

                    items(sampleItems) { item ->
                        ReadingListInterestCard(
                            text = item
                        )

                        /*
                        AsyncImage(
                            model = photo,
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
 */
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(
                            modifier = Modifier.height(64.dp)
                        )
                    }
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(color = WikipediaTheme.colors.paperColor)
                    .align(Alignment.BottomCenter)
                    .drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(48.dp)
                        .clickable {

                        }
                        .padding(12.dp),
                    painter = painterResource(R.drawable.ic_dice_24),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = "Randomize"
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp).weight(1f),
                    text = "X selected",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = WikipediaTheme.colors.primaryColor
                )
                Icon(
                    modifier = Modifier.size(48.dp)
                        .clickable {

                        }
                        .padding(12.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = "Randomize"
                )
            }
        }
    }
}



@Composable
fun ReadingListInterestCard(
    isSelected: Boolean = false,
    text: String
) {
    WikiCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clickable {

                }
        ) {
            // TODO: replace with thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = WikipediaTheme.colors.progressiveColor,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
            }
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = text,
                    style = WikipediaTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Foo Foo Foo foo foo foo foo foo foo foo foo foo foo",
                    style = WikipediaTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.secondaryColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ReadingListInterestSearchCard(
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                color = WikipediaTheme.colors.backgroundColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable {

            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = WikipediaTheme.colors.secondaryColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = "Search for an article",
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReadingListInterestsScreen() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        RecommendedReadingListInterestsScreen(
            uiState = Resource.Success(
                RecommendedReadingListInterestsViewModel.UiState(
                    fromSettings = false
                )
            ),
            onCloseClick = {},
            onNextClick = {}
        )
    }
}
