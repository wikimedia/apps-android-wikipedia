package org.wikipedia.readinglist.recommended

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.readinglist.ReadingListMode
import org.wikipedia.search.SearchActivity
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.views.imageservice.ImageService

class RecommendedReadingListInterestsFragment : Fragment() {
    private val viewModel: RecommendedReadingListInterestsViewModel by viewModels()

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SearchActivity.RESULT_LINK_SUCCESS) {
            val pageTitle = it.data?.parcelableExtra<PageTitle>(SearchActivity.EXTRA_RETURN_LINK_TITLE)!!
            RecommendedReadingListEvent.submit("search_click", "rrl_interests_select")
            viewModel.addTitle(pageTitle)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        viewModel.randomizeEvent.observe(viewLifecycleOwner) { state ->
            if (state.fromRandomize) {
                FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.recommended_reading_list_interest_pick_random_snackbar))
                    .setAction(R.string.recommended_reading_list_interest_pick_random_snackbar_action) {
                        viewModel.restoreState(state.prevItems, state.prevSelectedItems)
                    }
                    .show()
            }
        }

        RecommendedReadingListEvent.submit("impression", "rrl_interests_select")

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    RecommendedReadingListInterestsScreen(
                        uiState = viewModel.uiState.collectAsState().value,
                        fromSettings = viewModel.fromSettings,
                        onCloseClick = {
                            if (viewModel.fromSettings) {
                                viewModel.commitSelection()
                                requireActivity().setResult(RESULT_OK)
                                requireActivity().finish()
                            } else {
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        },
                        onNextClick = {
                            viewModel.commitSelection()
                            startActivity(ReadingListActivity.newIntent(requireContext(), readingListMode = ReadingListMode.RECOMMENDED))
                            requireActivity().finish()
                        },
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            backClickListener = {
                                requireActivity().finish()
                            },
                            retryClickListener = {
                                viewModel.loadItems()
                            }
                        ),
                        onSearchClick = {
                            val intent = SearchActivity.newIntent(requireActivity(), Constants.InvokeSource.READING_LIST_ACTIVITY, null, returnLink = true)
                            searchLauncher.launch(intent)
                        },
                        onItemClick = {
                            viewModel.toggleSelection(it)
                        },
                        onRandomizeClick = { listState ->
                            viewModel.randomizeSelection()
                            lifecycleScope.launch {
                                listState.scrollToItem(0)
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(fromSettings: Boolean = false): RecommendedReadingListInterestsFragment {
            return RecommendedReadingListInterestsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(RecommendedReadingListOnboardingActivity.EXTRA_FROM_SETTINGS, fromSettings)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendedReadingListInterestsScreen(
    uiState: Resource<RecommendedReadingListInterestsViewModel.UiState>,
    fromSettings: Boolean = false,
    wikiErrorClickEvents: WikiErrorClickEvents? = null,
    onItemClick: (PageTitle) -> Unit = {},
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
    onRandomizeClick: (listState: LazyStaggeredGridState) -> Unit,
    onSearchClick: () -> Unit
) {
    val listState = rememberLazyStaggeredGridState()
    val collapseHeight = LocalDensity.current.run { 100.dp.toPx() }

    val showTitle = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseHeight
        }
    }

    BackHandler {
        onCloseClick()
    }

    var showRandomizeDialog by remember { mutableStateOf(false) }
    if (showRandomizeDialog) {
        WikipediaAlertDialog(
            title = stringResource(R.string.recommended_reading_list_interest_pick_random_dialog_title),
            message = stringResource(R.string.recommended_reading_list_interest_pick_random_dialog_message),
            confirmButtonText = stringResource(R.string.recommended_reading_list_interest_pick_random_dialog_positive_button),
            dismissButtonText = stringResource(R.string.recommended_reading_list_interest_pick_random_dialog_negative_button),
            onDismissRequest = {
                showRandomizeDialog = false
            },
            onConfirmButtonClick = {
                onRandomizeClick(listState)
                RecommendedReadingListEvent.submit("random_confirm_click", "rrl_interests_select")
                showRandomizeDialog = false
            },
            onDismissButtonClick = {
                RecommendedReadingListEvent.submit("random_cancel_click", "rrl_interests_select")
                showRandomizeDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .background(WikipediaTheme.colors.paperColor),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showTitle.value,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        Text(
                            text = stringResource(R.string.recommended_reading_list_interest_pick_title),
                            color = WikipediaTheme.colors.primaryColor,
                            style = WikipediaTheme.typography.h2
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.search_back_button_content_description),
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
        when (uiState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = WikipediaTheme.colors.progressiveColor,
                        trackColor = WikipediaTheme.colors.borderColor
                    )
                }
            }

            is Resource.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    RecommendedReadingListInterestsContent(
                        fromSettings = fromSettings,
                        listState = listState,
                        items = uiState.data.items,
                        selectedItems = uiState.data.selectedItems,
                        onNextClick = onNextClick,
                        onItemClick = onItemClick,
                        onRandomizeClick = {
                            showRandomizeDialog = true
                        },
                        onSearchClick = onSearchClick
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedReadingListInterestsContent(
    fromSettings: Boolean,
    listState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    items: List<PageTitle> = emptyList(),
    selectedItems: Set<PageTitle> = emptySet(),
    onItemClick: (PageTitle) -> Unit = {},
    onNextClick: () -> Unit,
    onRandomizeClick: () -> Unit = {},
    onSearchClick: () -> Unit
) {
    Box {
        val borderColor = WikipediaTheme.colors.borderColor

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(140.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp),
            state = listState,
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = WikipediaTheme.colors.primaryColor,
                        textAlign = TextAlign.Center,
                        text = stringResource(R.string.recommended_reading_list_interest_pick_title)
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    ReadingListInterestSearchCard(onSearchClick)
                }
                items(items) { item ->
                    ReadingListInterestCard(
                        modifier = Modifier.animateItem(),
                        item = item,
                        isSelected = selectedItems.contains(item),
                        onItemClick = onItemClick
                    )
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
            if (!fromSettings) {
                Icon(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onRandomizeClick)
                        .padding(12.dp),
                    painter = painterResource(R.drawable.ic_dice_24),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = stringResource(R.string.recommended_reading_list_interest_pick_random_button_content_description)
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .weight(1f),
                text = pluralStringResource(
                    R.plurals.recommended_reading_list_interest_pick_selected_articles,
                    selectedItems.size,
                    selectedItems.size
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = WikipediaTheme.colors.primaryColor
            )
            if (!fromSettings) {
                Icon(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(enabled = selectedItems.isNotEmpty(), onClick = onNextClick)
                        .padding(12.dp)
                        .alpha(if (selectedItems.isNotEmpty()) 1f else 0.5f),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = stringResource(R.string.nav_item_forward)
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
fun ReadingListInterestCard(
    modifier: Modifier,
    item: PageTitle,
    isSelected: Boolean = false,
    onItemClick: (PageTitle) -> Unit = {},
) {
    WikiCard(
        modifier = modifier
            .fillMaxWidth(),
        elevation = 0.dp,
        border = BorderStroke(width = 1.dp, color = WikipediaTheme.colors.borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) WikipediaTheme.colors.additionColor else WikipediaTheme.colors.paperColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onItemClick(item)
                }
        ) {
            if (!item.thumbUrl.isNullOrEmpty()) {
                val request = ImageService.getRequest(LocalContext.current, url = item.thumbUrl, detectFace = true)
                AsyncImage(
                    model = request,
                    placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                HtmlText(
                    text = item.displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    if (!item.description.isNullOrEmpty()) {
                        HtmlText(
                            text = item.description.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WikipediaTheme.colors.secondaryColor,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            modifier = Modifier.size(24.dp).align(Alignment.Bottom),
                            imageVector = Icons.Default.CheckCircle,
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                    } else {
                        Spacer(modifier = Modifier.width(32.dp).height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingListInterestSearchCard(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                color = WikipediaTheme.colors.backgroundColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onSearchClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.search_hint),
            tint = WikipediaTheme.colors.secondaryColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = stringResource(R.string.recommended_reading_list_interest_pick_search_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReadingListInterestsScreen() {
    val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
    val titles = listOf(
        PageTitle(text = "Psychology of art", wiki = site, thumbUrl = "foo.jpg", description = "Study of mental functions and behaviors", displayText = null),
        PageTitle(text = "Industrial design", wiki = site, thumbUrl = "foo.jpg", description = "Process of design applied to physical products", displayText = null),
        PageTitle(text = "Dufourspitze", wiki = site, thumbUrl = "foo.jpg", description = "Highest mountain in Switzerland", displayText = null),
        PageTitle(text = "Sample title without description", wiki = site, thumbUrl = "foo.jpg", description = "", displayText = null),
        PageTitle(text = "Sample title without thumbnail", wiki = site, thumbUrl = "", description = "Sample description", displayText = null),
        PageTitle(text = "Octagon house", wiki = site, thumbUrl = "foo.jpg", description = "North American house style briefly popular in the 1850s", displayText = null),
        PageTitle(text = "Barack Obama", wiki = site, thumbUrl = "foo.jpg", description = "President of the United States from 2009 to 2017", displayText = null),
    )
    val selectedItems = setOf(PageTitle(text = "Industrial design", wiki = site, thumbUrl = "foo.jpg", description = "Process of design applied to physical products", displayText = null))

    BaseTheme(currentTheme = Theme.LIGHT) {
        RecommendedReadingListInterestsScreen(
            uiState = Resource.Success(
                RecommendedReadingListInterestsViewModel.UiState(
                    fromSettings = false,
                    items = titles,
                    selectedItems = selectedItems
                )
            ),
            onCloseClick = {},
            onNextClick = {},
            onSearchClick = {},
            onItemClick = {},
            onRandomizeClick = {},
        )
    }
}
