package org.wikipedia.settings.dev.playground

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.data.YearInReviewReadingStats

class YearInReviewPlaygroundDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    var selectedData by remember { mutableStateOf(Prefs.yearInReviewPlaygroundData) }
                    YearInReviewPlaygroundScreen(
                        isLoggedIn = AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount,
                        selectedData = selectedData,
                        onDataSelected = {
                            selectedData = it
                            Prefs.yearInReviewPlaygroundData = it
                        },
                        onBackClick = { dismiss() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            BottomSheetBehavior.from(it).isDraggable = false
        }
    }
}

@Composable
fun YearInReviewPlaygroundScreen(
    isLoggedIn: Boolean,
    selectedData: YearInReviewPlaygroundData,
    onDataSelected: (YearInReviewPlaygroundData) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                    contentDescription = stringResource(R.string.nav_item_back),
                    tint = WikipediaTheme.colors.primaryColor
                )
            }
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "Year in Review Playground",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = WikipediaTheme.colors.primaryColor
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReadingDataCard(
                isLoggedIn = isLoggedIn,
                selectedData = selectedData,
                onDataSelected = onDataSelected
            )
        }
    }
}

@Composable
private fun ReadingDataCard(
    isLoggedIn: Boolean,
    selectedData: YearInReviewPlaygroundData,
    onDataSelected: (YearInReviewPlaygroundData) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .background(WikipediaTheme.colors.backgroundColor)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Year in Review data",
                style = MaterialTheme.typography.titleMedium,
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = "Uses a test snapshot instead of your real Year in Review data, for both the Year in Review flow and the Activity tab card. " +
                        "Your real reading history isn't changed. Takes effect the next time Year in Review or the Activity tab loads.",
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.secondaryColor
            )
            Text(
                text = if (isLoggedIn) {
                    "Logged in as ${AccountUtil.userName}."
                } else {
                    "You're not logged in. Data rich only shows the personalized flow when you're logged in, so log in first."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isLoggedIn) WikipediaTheme.colors.successColor else WikipediaTheme.colors.destructiveColor
            )
            Column(modifier = Modifier.selectableGroup()) {
                YearInReviewPlaygroundData.entries.forEach { data ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = data == selectedData,
                                onClick = { onDataSelected(data) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = data == selectedData,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = WikipediaTheme.colors.progressiveColor,
                                unselectedColor = WikipediaTheme.colors.primaryColor
                            )
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = data.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = WikipediaTheme.colors.primaryColor
                            )
                            Text(
                                text = data.readingStats?.let { describe(it) } ?: "Uses your actual Year in Review data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WikipediaTheme.colors.secondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun describe(readingStats: YearInReviewReadingStats): String {
    return "${readingStats.articlesReadCount} articles read (personalized from ${YearInReviewReadingStats.MIN_ARTICLES_READ}), " +
            "${readingStats.visitedDaysCount} days visited (personalized from ${YearInReviewReadingStats.MIN_VISITED_DAYS})"
}

@Preview
@Composable
private fun YearInReviewPlaygroundScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewPlaygroundScreen(
            isLoggedIn = false,
            selectedData = YearInReviewPlaygroundData.DATA_RICH,
            onDataSelected = {},
            onBackClick = {}
        )
    }
}
