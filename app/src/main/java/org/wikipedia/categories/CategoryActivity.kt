package org.wikipedia.categories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import org.wikipedia.views.imageservice.ImageService

class CategoryActivity : BaseActivity() {
    private val viewModel: CategoryActivityViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                CategoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onShowCategoryDialog = {
                        ExclusiveBottomSheetPresenter.show(
                            supportFragmentManager,
                            CategoryDialog.newInstance(viewModel.pageTitle)
                        )
                    },
                    onNavigateToCategory = { title ->
                        startActivity(newIntent(this, title))
                    },
                    onShowArticlePreview = { title ->
                        val entry = HistoryEntry(title, HistoryEntry.SOURCE_CATEGORY)
                        ExclusiveBottomSheetPresenter.show(
                            supportFragmentManager,
                            LinkPreviewDialog.newInstance(entry)
                        )
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context, categoryTitle: PageTitle): Intent {
            return Intent(context, CategoryActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, categoryTitle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryActivityViewModel,
    onNavigateBack: () -> Unit,
    onShowCategoryDialog: () -> Unit,
    onNavigateToCategory: (PageTitle) -> Unit,
    onShowArticlePreview: (PageTitle) -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(if (viewModel.showSubcategories) 1 else 0) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            Column {
                WikiTopAppBar(
                    title = StringUtil.removeHTMLTags(viewModel.pageTitle.displayText),
                    titleStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 24.sp
                    ),
                    onNavigationClick = {
                        BreadCrumbLogEvent.logClick(context, "navigationButton")
                        onNavigateBack()
                    },
                    actions = {
                        Box {
                            IconButton(onClick = {
                                showMenu = true
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                                    tint = WikipediaTheme.colors.primaryColor,
                                    contentDescription = stringResource(R.string.menu_feed_overflow_label)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = WikipediaTheme.colors.paperColor,
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.action_item_categories),
                                            color = WikipediaTheme.colors.primaryColor
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        BreadCrumbLogEvent.logClick(context, "categoryButton")
                                        onShowCategoryDialog()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_category_black_24dp),
                                            tint = WikipediaTheme.colors.secondaryColor,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                )

                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = WikipediaTheme.colors.paperColor,
                    contentColor = WikipediaTheme.colors.progressiveColor,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            color = WikipediaTheme.colors.progressiveColor,
                            modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                            width = Dp.Unspecified
                        )
                    },
                    divider = {
                        HorizontalDivider(color = WikipediaTheme.colors.borderColor)
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            viewModel.showSubcategories = false
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.category_tab_articles),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selectedTabIndex == 0) {
                                    WikipediaTheme.colors.progressiveColor
                                } else {
                                    WikipediaTheme.colors.placeholderColor
                                }
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            viewModel.showSubcategories = true
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.category_tab_subcategories),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selectedTabIndex == 1) {
                                    WikipediaTheme.colors.progressiveColor
                                } else {
                                    WikipediaTheme.colors.placeholderColor
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (selectedTabIndex == 0) {
            CategoryMembersList(
                viewModel = viewModel,
                isSubcategories = false,
                onItemClick = onShowArticlePreview,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            CategoryMembersList(
                viewModel = viewModel,
                isSubcategories = true,
                onItemClick = onNavigateToCategory,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun CategoryMembersList(
    viewModel: CategoryActivityViewModel,
    isSubcategories: Boolean,
    onItemClick: (PageTitle) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagingItems = if (isSubcategories) {
        viewModel.subcategoriesFlow.collectAsLazyPagingItems()
    } else {
        viewModel.categoryMembersFlow.collectAsLazyPagingItems()
    }

    val loadState = pagingItems.loadState

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loadState.refresh is LoadState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = WikipediaTheme.colors.progressiveColor
                )
            }

            loadState.refresh is LoadState.Error -> {
                val error = (loadState.refresh as LoadState.Error).error
                WikiErrorView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(16.dp),
                    caught = error,
                    pageTitle = viewModel.pageTitle,
                    errorClickEvents = WikiErrorClickEvents(
                        retryClickListener = { pagingItems.retry() }
                    ),
                    retryForGenericError = true
                )
            }

            loadState.append is LoadState.NotLoading && loadState.append.endOfPaginationReached && pagingItems.itemCount == 0 -> {
                Text(
                    text = stringResource(
                        if (isSubcategories) R.string.subcategory_empty else R.string.category_empty
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = WikipediaTheme.colors.secondaryColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = pagingItems.itemCount) { index ->
                        pagingItems[index]?.let { pageTitle ->
                            CategoryItem(
                                pageTitle = pageTitle,
                                onClick = { onItemClick(pageTitle) }
                            )
                            if (index < pagingItems.itemCount - 1) {
                                HorizontalDivider(
                                    color = WikipediaTheme.colors.borderColor,
                                    thickness = 1.dp
                                )
                            }
                        }
                    }

                    if (loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = WikipediaTheme.colors.progressiveColor
                                )
                            }
                        }
                    }

                    if (loadState.append is LoadState.Error) {
                        item {
                            val error = (loadState.append as LoadState.Error).error
                            WikiErrorView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                caught = error,
                                pageTitle = viewModel.pageTitle,
                                errorClickEvents = WikiErrorClickEvents(
                                    retryClickListener = { pagingItems.retry() }
                                ),
                                retryForGenericError = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    pageTitle: PageTitle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(WikipediaTheme.colors.paperColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (pageTitle.namespace() !== Namespace.CATEGORY) {
                    pageTitle.displayText
                } else {
                    StringUtil.removeUnderscores(pageTitle.text)
                },
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!pageTitle.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = pageTitle.description.orEmpty(),
                    color = WikipediaTheme.colors.secondaryColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!pageTitle.thumbUrl.isNullOrEmpty()) {
            val request = ImageService.getRequest(context, url = pageTitle.thumbUrl)
            AsyncImage(
                model = request,
                placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryItemPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CategoryItem(
            pageTitle = PageTitle(
                "Example Article",
                WikiSite("https://en.wikipedia.org/".toUri(), "en")
            ).apply {
                description = "This is an example article description"
            },
            onClick = {}
        )
    }
}
