package org.wikipedia.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeTest(modifier: Modifier = Modifier) {
    var currentTheme by remember { mutableStateOf(WikipediaThemeType.SYSTEM) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scrollableState = rememberScrollState()
    MainTheme(
        wikipediaThemeType = currentTheme
    ) {
        Scaffold(
            content = { paddingValues ->
                Column(
                    modifier = modifier
                        .verticalScroll(scrollableState)
                        .padding(paddingValues)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeButtons(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onLightThemeClick = {
                            currentTheme = WikipediaThemeType.LIGHT
                        },
                        onDarkThemeClick = {
                            currentTheme = WikipediaThemeType.DARK
                        },
                        onBlackThemeClick = {
                            currentTheme = WikipediaThemeType.BLACK
                        },
                        onSepiaThemeClick = {
                            currentTheme = WikipediaThemeType.SEPIA
                        }
                    )

                    NewWikiCardView(
                        modifier = Modifier
                            .fillMaxWidth(),
                        content = {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ThemedText(
                                    text = "Jan 6, 2025"
                                )
                                ThemedText(
                                    text = "Featured Article",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = WikipediaTheme.colors.successColor
                                )
                                Image(
                                    modifier = Modifier
                                        .height(192.dp)
                                        .fillMaxWidth(),
                                    painter = painterResource(R.drawable.ic_image_tags_onboarding_dark),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null
                                )
                                ThemedText(
                                    text = "Test",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = WikipediaTheme.colors.highlightColor
                                )
                                ThemedText(
                                    text = "What is Lorem Ipsum?\n" +
                                            "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.",
                                    color = WikipediaTheme.colors.destructiveColor
                                )
                            }
                        }
                    )

                    ThemedButton(
                        onClick = {
                            showDatePicker = true
                        },
                        content = {
                            Text("Date Picker")
                        }
                    )

                    if (showDatePicker) {
                        ThemedDatePicker(
                            onDismissRequest = {
                                showDatePicker = false
                            },

                            confirmButton = {},
                        )
                    }
                }
            }
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ThemeTestPreview() {
    ThemeTest(
        modifier = Modifier
            .fillMaxSize()
    )
}
