package org.wikipedia.settings.dev.playground

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.categories.db.Category
import org.wikipedia.categories.db.CategoryCount
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UiState

class CategoryDeveloperPlayGround : BaseActivity() {

    private val viewModel: CategoryDeveloperPlayGroundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val categoryCountState = viewModel.categoryCountState.collectAsState()
            val categoryState = viewModel.categoryState.collectAsState()

            BaseTheme {
                CategoryDeveloperPlayGroundScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    categoryCountState = categoryCountState.value,
                    categoryState = categoryState.value,
                    onAddToDb = { title, languageCode, year ->
                        if (!validateInput(title.isEmpty() || languageCode.isEmpty() || year.isEmpty())) {
                            return@CategoryDeveloperPlayGroundScreen
                        }
                        viewModel.addTestData(this, title, languageCode, year.toInt())
                    },
                    onBulkAddToDb = { numberOfRows, year ->
                        if (!validateInput(numberOfRows.isEmpty() || year.isEmpty())) {
                            return@CategoryDeveloperPlayGroundScreen
                        }
                        viewModel.addTestDataBulk(this, numberOfRows.toInt(), year.toInt())
                    },
                    onDeleteAll = {
                        viewModel.deleteAllCategories(this)
                    },
                    onDeleteBeforeYear = { yearsAgo ->
                        if (!validateInput(yearsAgo.isEmpty())) {
                            return@CategoryDeveloperPlayGroundScreen
                        }
                        viewModel.deleteBeforeYear(this, yearsAgo.toInt())
                    },
                    onFilter = { year ->
                        if (!validateInput(year.isEmpty())) {
                            return@CategoryDeveloperPlayGroundScreen
                        }
                        viewModel.filterBy(year.toInt())
                    },
                    onOptionSelected = { option ->
                        when (option) {
                            Option.ENTRY -> {
                                viewModel.loadCategories()
                            }

                            Option.FILTER -> {}
                            Option.DELETE -> {
                                viewModel.loadCategories()
                            }
                        }
                    },
                    onBackButtonClick = {
                        onBackPressed()
                    }
                )
            }
        }
    }

    private fun validateInput(isEmpty: Boolean): Boolean {
        if (isEmpty) {
            Toast.makeText(this, "Please enter all information", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}

@Composable
fun CategoryDeveloperPlayGroundScreen(
    modifier: Modifier = Modifier,
    categoryCountState: UiState<List<CategoryCount>>,
    categoryState: UiState<List<Category>>,
    onAddToDb: (String, String, String) -> Unit,
    onBulkAddToDb: (String, String) -> Unit,
    onFilter: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onDeleteBeforeYear: (String) -> Unit,
    onOptionSelected: (Option) -> Unit,
    onBackButtonClick: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(Option.ENTRY) }
    var selectedEntry by remember { mutableStateOf(ENTRY.SINGLE) }

    // Keep tracks of top content height to calculate remaining space for table
    var topContentHeight by remember { mutableIntStateOf(0) }
    val topContentHeightDp = with(LocalDensity.current) { topContentHeight.toDp() }

    // Calculate available height for the entire screen
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = "Category Playground",
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        // Measure the height of the top content
                        topContentHeight = coordinates.size.height
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main options row
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ChipButton(
                            text = "Entry",
                            isSelected = selectedOption == Option.ENTRY,
                            onClick = {
                                onOptionSelected(Option.ENTRY)
                                selectedOption = Option.ENTRY
                            }
                        )
                    }
                    item {
                        ChipButton(
                            text = "Filter",
                            isSelected = selectedOption == Option.FILTER,
                            onClick = {
                                onOptionSelected(Option.FILTER)
                                selectedOption = Option.FILTER
                            }
                        )
                    }
                    item {
                        ChipButton(
                            text = "Delete",
                            isSelected = selectedOption == Option.DELETE,
                            onClick = {
                                onOptionSelected(Option.DELETE)
                                selectedOption = Option.DELETE
                            }
                        )
                    }
                }

                when (selectedOption) {
                    Option.ENTRY -> {
                        LazyRow(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                ChipButton(
                                    text = "Single",
                                    isSelected = selectedEntry == ENTRY.SINGLE,
                                    onClick = {
                                        selectedEntry = ENTRY.SINGLE
                                    }
                                )
                            }
                            item {
                                ChipButton(
                                    text = "Random Bulk Entries",
                                    isSelected = selectedEntry == ENTRY.RANDOM_BULK,
                                    onClick = {
                                        selectedEntry = ENTRY.RANDOM_BULK
                                    }
                                )
                            }
                        }
                        when (selectedEntry) {
                            ENTRY.SINGLE -> SingleEntryView(onAddToDb)
                            ENTRY.RANDOM_BULK -> RandomBulkEntryView(onBulkAddToDb)
                        }
                    }
                    Option.FILTER -> {
                        FilterView(
                            onFilter = { year ->
                                onFilter(year)
                            }
                        )
                    }
                    Option.DELETE -> {
                        DeleteView(
                            onDeleteAll = onDeleteAll,
                            onDeleteBeforeYear = onDeleteBeforeYear
                        )
                    }
                }
            }

            // Calculates the remaining height for table
            // We subtract topContentHeight, paddingValues, and add some buffer space
            val tableHeight = screenHeight - topContentHeightDp - paddingValues.calculateTopPadding() -
                    paddingValues.calculateBottomPadding() - statusBarHeight - navBarHeight - 16.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tableHeight.coerceAtLeast(300.dp))
            ) {
                when {
                    selectedOption == Option.FILTER && categoryCountState is UiState.Success -> {
                        CategoryTable(
                            modifier = Modifier.fillMaxSize(),
                            categoriesCount = categoryCountState.data
                        )
                    }

                    selectedOption != Option.FILTER && categoryState is UiState.Success -> {
                        CategoryTable(
                            modifier = Modifier.fillMaxSize(),
                            categories = categoryState.data
                        )
                    }

                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            color = WikipediaTheme.colors.progressiveColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteView(
    modifier: Modifier = Modifier,
    onDeleteAll: () -> Unit,
    onDeleteBeforeYear: (String) -> Unit
) {
    var year by remember { mutableStateOf("") }

    Column(
        modifier = modifier
    ) {
        AppButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                onDeleteAll()
            }
        ) {
            Text("Delete All")
        }

        OutlinedTextField(
            value = year,
            singleLine = true,
            onValueChange = {
                year = it
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Years before") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WikipediaTheme.colors.primaryColor,
                unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                focusedBorderColor = WikipediaTheme.colors.primaryColor,
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
        AppButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                onDeleteBeforeYear(year)
            }
        ) {
            Text("Delete Before")
        }
    }
}

@Composable
fun FilterView(
    modifier: Modifier = Modifier,
    onFilter: (String) -> Unit
) {
    var year by remember { mutableStateOf("2025") }

    Column(
        modifier = modifier
    ) {
        OutlinedTextField(
            value = year,
            onValueChange = {
                year = it
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Year") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WikipediaTheme.colors.primaryColor,
                unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                focusedBorderColor = WikipediaTheme.colors.primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(
            modifier = Modifier
                .height(6.dp)
        )
        AppButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                onFilter(year)
            }
        ) {
            Text("Filter")
        }
    }
}

@Composable
fun SingleEntryView(
    onAddToDb: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var languageCode by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2025") }

    Column(
        modifier = modifier
    ) {
        // Input field for size
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
            },
            singleLine = true,
            label = {
                Text(
                    text = "Title",
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WikipediaTheme.colors.primaryColor,
                unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                focusedBorderColor = WikipediaTheme.colors.primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        OutlinedTextField(
            value = languageCode,
            onValueChange = {
                languageCode = it
            },
            singleLine = true,
            label = {
                Text(
                    text = "language code",
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WikipediaTheme.colors.primaryColor,
                unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                focusedBorderColor = WikipediaTheme.colors.primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        OutlinedTextField(
            value = year,
            onValueChange = {
                year = it
            },
            singleLine = true,
            label = {
                Text(
                    text = "Year",
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WikipediaTheme.colors.primaryColor,
                unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                focusedBorderColor = WikipediaTheme.colors.primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(
            modifier = Modifier
                .height(6.dp)
        )
        AppButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                onAddToDb(title, languageCode, year)
            }
        ) {
            Text("Add Entry")
        }
    }
}

@Composable
fun RandomBulkEntryView(
    onBulkAddToDb: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var numberOfRows by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2025") }

    Column(
        modifier = modifier
    ) {
        // Input field for size
        OutlinedTextField(
            value = numberOfRows,
            onValueChange = {
                numberOfRows = it
            },
            singleLine = true,
            label = {
                Text(
                    text = "Number of Rows",
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WikipediaTheme.colors.primaryColor,
                unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                focusedBorderColor = WikipediaTheme.colors.primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        OutlinedTextField(
            value = year,
            onValueChange = {
                year = it
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = {
                Text(
                    text = "Year",
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WikipediaTheme.colors.primaryColor,
                unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                focusedBorderColor = WikipediaTheme.colors.primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(
            modifier = Modifier
                .height(6.dp)
        )
        AppButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                onBulkAddToDb(numberOfRows, year)
            }
        ) {
            Text("Add Random Entries")
        }
    }
}

@Composable
fun ChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) WikipediaTheme.colors.focusColor else
                WikipediaTheme.colors.progressiveColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
        )
    }
}

@Composable
fun CategoryTable(
    modifier: Modifier = Modifier,
    categoriesCount: List<CategoryCount>? = null,
    categories: List<Category>? = null
) {
    LazyColumn(
        modifier = modifier
    ) {
        // Table header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Title",
                    modifier = Modifier.weight(0.4f),
                    color = WikipediaTheme.colors.primaryColor,
                    style = WikipediaTheme.typography.h3
                )
                Text(
                    text = "Language",
                    modifier = Modifier.weight(0.3f),
                    color = WikipediaTheme.colors.primaryColor,
                    style = WikipediaTheme.typography.h3
                )
                Text(
                    text = if (categoriesCount != null) "Count" else "Timestamp",
                    modifier = Modifier.weight(0.3f),
                    color = WikipediaTheme.colors.primaryColor,
                    style = WikipediaTheme.typography.h3
                )
            }
        }

        if (categoriesCount != null) {
            items(categoriesCount) { categoryCount ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryCount.title,
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        text = categoryCount.lang,
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.weight(0.3f)
                    )
                    Text(
                        text = categoryCount.count.toString(),
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.weight(0.3f)
                    )
                }
            }
        }

        if (categories != null) {
            items(categories) { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = category.title,
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        text = category.lang,
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.weight(0.3f)
                    )
                    Text(
                        text = DateUtil.epochMilliToYear(category.timeStamp.time).toString(),
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.weight(0.3f)
                    )
                }
            }
        }
    }
}

enum class Option {
    ENTRY, FILTER, DELETE
}

enum class ENTRY {
    SINGLE, RANDOM_BULK
}

@Preview(showBackground = true)
@Composable
private fun CategoryDeveloperPlayGroundScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        CategoryDeveloperPlayGroundScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            categoryCountState = UiState.Loading,
            categoryState = UiState.Loading,
            onAddToDb = { _, _, _ -> },
            onDeleteAll = {},
            onDeleteBeforeYear = {},
            onBulkAddToDb = { _, _ -> },
            onFilter = {},
            onOptionSelected = {},
            onBackButtonClick = {}
        )
    }
}
