package org.wikipedia.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    placeHolderTitle: String,
    onSearchQueryChange: (String) -> Unit,
    onBackButtonClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        title = {
            OutlinedTextField(
                modifier = Modifier
                    .padding(top = 2.dp),
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(placeHolderTitle)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    focusedTextColor = WikipediaTheme.colors.primaryColor,
                    cursorColor = WikipediaTheme.colors.progressiveColor
                ),
                textStyle = TextStyle(
                    fontSize = 14.sp
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_button_content_description),
                    tint = WikipediaTheme.colors.primaryColor
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = WikipediaTheme.colors.paperColor,
            titleContentColor = WikipediaTheme.colors.primaryColor
        ),
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
