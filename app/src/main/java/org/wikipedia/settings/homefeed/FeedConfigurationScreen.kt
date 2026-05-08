package org.wikipedia.settings.homefeed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun WhatsDrivingFeedModulesScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.home_feed_settings_whats_driving_title),
                onNavigationClick = onBack,
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "TODO: What's Driving Your Feed")
        }
    }
}
