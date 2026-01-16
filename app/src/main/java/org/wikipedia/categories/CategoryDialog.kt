package org.wikipedia.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class CategoryDialog : ExtendedBottomSheetDialogFragment() {
    private val viewModel: CategoryDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BaseTheme {
                    CategoryDialogContent(
                        viewModel = viewModel,
                        onCategoryClick = { title ->
                            startActivity(CategoryActivity.newIntent(requireActivity(), title))
                        },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(title: PageTitle): CategoryDialog {
            return CategoryDialog().apply { arguments = bundleOf(Constants.ARG_TITLE to title) }
        }
    }
}

@Composable
fun CategoryDialogContent(
    viewModel: CategoryDialogViewModel,
    onCategoryClick: (PageTitle) -> Unit,
    onDismiss: () -> Unit
) {
    var categoriesData by remember { mutableStateOf<Resource<List<PageTitle>>?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.categoriesData.observe(lifecycleOwner) { data ->
                categoriesData = data
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WikipediaTheme.colors.paperColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_category_black_24dp),
                tint = WikipediaTheme.colors.secondaryColor,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_item_categories),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = WikipediaTheme.colors.primaryColor,
                    maxLines = 1
                )

                HtmlText(
                    text = viewModel.pageTitle.displayText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = WikipediaTheme.colors.primaryColor,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        HorizontalDivider(
            color = WikipediaTheme.colors.borderColor,
            thickness = 0.5.dp
        )

        when (val data = categoriesData) {
            is Resource.Success -> {
                val categories = data.data
                if (categories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.page_no_categories),
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(categories) { category ->
                            CategoryDialogItem(
                                pageTitle = category,
                                onClick = { onCategoryClick(category) }
                            )
                            HorizontalDivider(
                                color = WikipediaTheme.colors.borderColor,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }

            is Resource.Error -> {
                WikiErrorView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    caught = data.throwable,
                    errorClickEvents = WikiErrorClickEvents(
                        backClickListener = { onDismiss() }
                    )
                )
            }

            else -> {
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
    }
}

@Composable
fun CategoryDialogItem(
    pageTitle: PageTitle,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(WikipediaTheme.colors.paperColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = StringUtil.removeNamespace(pageTitle.displayText),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}
