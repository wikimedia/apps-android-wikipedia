package org.wikipedia.settings.dev.playground

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidget
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import java.time.LocalDate

class ReadingChallengePlayGroundActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ReadingChallengeWidgetRepository(this)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            BaseTheme {
                Scaffold(
                    topBar = {
                        WikiTopAppBar(
                            title = "Reading Challenge Playground",
                            onNavigationClick = {
                                onBackPressedDispatcher.onBackPressed()
                            }
                        )
                    },
                    containerColor = WikipediaTheme.colors.paperColor
                ) { paddingValues ->
                    ReadingChallengePlayground(
                        modifier = Modifier
                            .padding(paddingValues),
                        state = repository.observeState().collectAsState(initial = ReadingChallengeState.NotLiveYet).value,
                        updateWidgetsExplicitly = {
                            coroutineScope.launch {
                                ReadingChallengeWidget().updateAll(this@ReadingChallengePlayGroundActivity)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingChallengePlayground(
    modifier: Modifier = Modifier,
    state: ReadingChallengeState,
    updateWidgetsExplicitly: () -> Unit
) {
    var streak by remember { mutableIntStateOf(Prefs.readingChallengeStreak) }
    var enrolled by remember { mutableStateOf(Prefs.readingChallengeEnrolled) }
    var lastReadDate by remember { mutableStateOf(Prefs.readingChallengeLastReadDate) }

    fun syncFromPrefs() {
        streak = Prefs.readingChallengeStreak
        enrolled = Prefs.readingChallengeEnrolled
        lastReadDate = Prefs.readingChallengeLastReadDate
    }

    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    val threeDaysAgo = LocalDate.now().minusDays(3).toString()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Current State: ${state::class.simpleName}",
            style = MaterialTheme.typography.headlineSmall
        )

        // --- Streak ---
        Card {
            Column(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("readingChallengeStreak", style = MaterialTheme.typography.labelMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        if (streak > 0) {
                            Prefs.readingChallengeStreak = --streak
                            updateWidgetsExplicitly()
                        }
                    }) { Text("−") }
                    Text("$streak", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = {
                        Prefs.readingChallengeStreak = ++streak
                        updateWidgetsExplicitly()
                    }) { Text("+") }
                }
            }
        }

        // --- Enrolled ---
        Card {
            Row(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("readingChallengeEnrolled", style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = enrolled,
                    onCheckedChange = {
                        enrolled = it
                        Prefs.readingChallengeEnrolled = it
                        updateWidgetsExplicitly()
                    },
                    colors = SwitchDefaults.colors(
                        uncheckedTrackColor = WikipediaTheme.colors.paperColor,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                        checkedThumbColor = WikipediaTheme.colors.paperColor
                    )
                )
            }
        }

        // --- Last Read Date ---
        Card {
            Column(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("readingChallengeLastReadDate", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = lastReadDate,
                        onValueChange = {
                            lastReadDate = it
                        },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            Prefs.readingChallengeLastReadDate = lastReadDate
                            updateWidgetsExplicitly()
                        },
                        enabled = lastReadDate.isEmpty() || runCatching { LocalDate.parse(lastReadDate) }.isSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WikipediaTheme.colors.progressiveColor,
                            contentColor = WikipediaTheme.colors.paperColor
                        )
                    ) {
                        Text("Save")
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {
                        Prefs.readingChallengeLastReadDate = today
                        lastReadDate = today
                        updateWidgetsExplicitly()
                    }, label = { Text("Today") })
                    AssistChip(onClick = {
                        Prefs.readingChallengeLastReadDate = yesterday
                        lastReadDate = yesterday
                        updateWidgetsExplicitly()
                    }, label = { Text("Yesterday") })
                    AssistChip(onClick = {
                        Prefs.readingChallengeLastReadDate = threeDaysAgo
                        lastReadDate = threeDaysAgo
                        updateWidgetsExplicitly()
                    }, label = { Text("3 Days Ago") })
                    AssistChip(onClick = {
                        Prefs.readingChallengeLastReadDate = ""
                        lastReadDate = ""
                        updateWidgetsExplicitly()
                    }, label = { Text("Clear") })
                }
            }
        }

        // --- Reset ---
        OutlinedButton(
            onClick = {
                Prefs.readingChallengeStreak = 0
                Prefs.readingChallengeEnrolled = false
                Prefs.readingChallengeLastReadDate = ""
                syncFromPrefs()
                updateWidgetsExplicitly()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Reset All")
        }
    }
}
