package org.wikipedia.settings.dev.playground

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetWorker
import java.time.LocalDate

class ReadingChallengePlayGroundDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val repository = ReadingChallengeWidgetRepository(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                val stateFlow = remember { repository.observeState() }
                val coroutineScope = rememberCoroutineScope()

                BaseTheme {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { dismiss() },
                                content = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                                        contentDescription = stringResource(R.string.nav_item_back),
                                        tint = WikipediaTheme.colors.primaryColor
                                    )
                                }
                            )
                            Text(
                                text = "Reading Challenge Playground",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = WikipediaTheme.colors.primaryColor,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        ReadingChallengePlayground(
                            modifier = Modifier.padding(16.dp),
                            state = stateFlow.collectAsState(initial = ReadingChallengeState.NotLiveYet).value,
                            updateWidgetsExplicitly = {
                                coroutineScope.launch {
                                    ReadingChallengeWidgetRepository(requireContext()).updateWidgetsAndSendAnalytics()
                                }
                            },
                            updateWidgetsUpdateFrequency = {
                                ReadingChallengeWidgetWorker.scheduleNextWidgetUpdate(requireContext())
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { view ->
                BottomSheetBehavior.from(view).isDraggable = false
            }
        }
    }
}

@Composable
fun ReadingChallengePlayground(
    modifier: Modifier = Modifier,
    state: ReadingChallengeState,
    updateWidgetsExplicitly: () -> Unit,
    updateWidgetsUpdateFrequency: () -> Unit
) {
    var streak by remember { mutableIntStateOf(Prefs.readingChallengeStreak) }
    var enrolled by remember { mutableStateOf(Prefs.readingChallengeEnrolled) }
    var lastReadDate by remember { mutableStateOf(Prefs.readingChallengeLastReadDate) }
    var startDate by remember { mutableStateOf(Prefs.readingChallengeStartDate) }
    var endDate by remember { mutableStateOf(Prefs.readingChallengeEndDate) }
    var onboardingShown by remember { mutableStateOf(Prefs.readingChallengeOnboardingShown) }
    var widgetPromptShown by remember { mutableStateOf(Prefs.readingChallengeInstallPromptShown) }
    var fastCycle by remember { mutableStateOf(Prefs.readingChallengeWidgetFastCycle) }

    fun syncFromPrefs() {
        streak = Prefs.readingChallengeStreak
        enrolled = Prefs.readingChallengeEnrolled
        lastReadDate = Prefs.readingChallengeLastReadDate
        startDate = Prefs.readingChallengeStartDate
        endDate = Prefs.readingChallengeEndDate
        onboardingShown = Prefs.readingChallengeOnboardingShown
        widgetPromptShown = Prefs.readingChallengeInstallPromptShown
    }

    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    val threeDaysAgo = LocalDate.now().minusDays(3).toString()

    val switchColor = SwitchDefaults.colors(
        uncheckedTrackColor = WikipediaTheme.colors.paperColor,
        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
        checkedTrackColor = WikipediaTheme.colors.progressiveColor,
        checkedThumbColor = WikipediaTheme.colors.paperColor
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Current State: ${state::class.simpleName}",
            style = MaterialTheme.typography.titleLarge,
            color = WikipediaTheme.colors.primaryColor
        )

        // --- Streak ---
        Card {
            Column(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "readingChallengeStreak",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
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
                    Text(
                        text = "$streak",
                        style = MaterialTheme.typography.headlineMedium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    IconButton(onClick = {
                        Prefs.readingChallengeStreak = ++streak
                        updateWidgetsExplicitly()
                    }) { Text("+", color = WikipediaTheme.colors.primaryColor) }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "readingChallengeEnrolled",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Switch(
                    checked = enrolled,
                    onCheckedChange = {
                        enrolled = it
                        Prefs.readingChallengeEnrolled = it
                        Prefs.readingChallengeEnrollmentDate = LocalDate.now().toString()
                        updateWidgetsExplicitly()
                    },
                    colors = switchColor
                )
            }
        }

        if (state is ReadingChallengeState.EnrolledNotStarted || state is ReadingChallengeState.StreakOngoingNeedsReading || state is ReadingChallengeState.StreakOngoingReadToday) {
            // --- Fast Cycle ---
            Card {
                Row(
                    Modifier
                        .background(WikipediaTheme.colors.backgroundColor)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "readingChallengeWidgetFastCycle",
                            style = MaterialTheme.typography.labelMedium,
                            color = WikipediaTheme.colors.primaryColor
                        )
                        Text(
                            text = "Updates every 1 min instead of midnight",
                            style = MaterialTheme.typography.bodySmall,
                            color = WikipediaTheme.colors.secondaryColor
                        )
                    }
                    Switch(
                        checked = fastCycle,
                        onCheckedChange = {
                            fastCycle = it
                            Prefs.readingChallengeWidgetFastCycle = it
                            updateWidgetsUpdateFrequency()
                        },
                        colors = switchColor
                    )
                }
            }
        }

        // --- OnBoarding ---
        Card {
            Row(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "readingChallengeOnboardingShown",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Switch(
                    checked = onboardingShown,
                    onCheckedChange = {
                        onboardingShown = it
                        Prefs.readingChallengeOnboardingShown = it
                        updateWidgetsExplicitly()
                    },
                    colors = switchColor
                )
            }
        }

        // --- Widget prompt ---
        Card {
            Row(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "readingChallengeInstallPromptShown",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Switch(
                    checked = widgetPromptShown,
                    onCheckedChange = {
                        widgetPromptShown = it
                        Prefs.readingChallengeInstallPromptShown = it
                        updateWidgetsExplicitly()
                    },
                    colors = switchColor
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
                Text(
                    text = "readingChallengeLastReadDate",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = lastReadDate,
                        onValueChange = {
                            lastReadDate = it
                        },
                        placeholder = { Text("YYYY-MM-DD", color = WikipediaTheme.colors.primaryColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WikipediaTheme.colors.primaryColor,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = WikipediaTheme.colors.primaryColor,
                            errorTextColor = WikipediaTheme.colors.primaryColor,
                        ),
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
                    }, label = { Text("Today", color = WikipediaTheme.colors.primaryColor) })
                    AssistChip(onClick = {
                        Prefs.readingChallengeLastReadDate = yesterday
                        lastReadDate = yesterday
                        updateWidgetsExplicitly()
                    }, label = { Text("Yesterday", color = WikipediaTheme.colors.primaryColor) })
                    AssistChip(onClick = {
                        Prefs.readingChallengeLastReadDate = threeDaysAgo
                        lastReadDate = threeDaysAgo
                        updateWidgetsExplicitly()
                    }, label = { Text("3 Days Ago", color = WikipediaTheme.colors.primaryColor) })
                    AssistChip(onClick = {
                        Prefs.readingChallengeLastReadDate = ""
                        lastReadDate = ""
                        updateWidgetsExplicitly()
                    }, label = { Text("Clear", color = WikipediaTheme.colors.primaryColor) })
                }
            }
        }

        // --- Challenge Start Date ---
        Card {
            Column(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "readingChallengeStartDate",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                        },
                        placeholder = {
                            Text(
                                "YYYY-MM-DD",
                                color = WikipediaTheme.colors.primaryColor
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WikipediaTheme.colors.primaryColor,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = WikipediaTheme.colors.primaryColor,
                            errorTextColor = WikipediaTheme.colors.primaryColor,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            Prefs.readingChallengeStartDate = startDate
                            updateWidgetsExplicitly()
                        },
                        enabled = startDate.isEmpty() || runCatching { LocalDate.parse(startDate) }.isSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WikipediaTheme.colors.progressiveColor,
                            contentColor = WikipediaTheme.colors.paperColor
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        // --- Challenge End Date ---
        Card {
            Column(
                Modifier
                    .background(WikipediaTheme.colors.backgroundColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "readingChallengeEndDate",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {
                            endDate = it
                        },
                        placeholder = {
                            Text(
                                "YYYY-MM-DD",
                                color = WikipediaTheme.colors.primaryColor
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WikipediaTheme.colors.primaryColor,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedTextColor = WikipediaTheme.colors.primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = WikipediaTheme.colors.primaryColor,
                            errorTextColor = WikipediaTheme.colors.primaryColor,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            Prefs.readingChallengeEndDate = endDate
                            updateWidgetsExplicitly()
                        },
                        enabled = endDate.isEmpty() || runCatching { LocalDate.parse(endDate) }.isSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WikipediaTheme.colors.progressiveColor,
                            contentColor = WikipediaTheme.colors.paperColor
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        // --- Reset ---
        OutlinedButton(
            onClick = {
                Prefs.readingChallengeStreak = 0
                Prefs.readingChallengeEnrolled = false
                Prefs.readingChallengeOnboardingShown = false
                Prefs.readingChallengeInstallPromptShown = false
                Prefs.readingChallengeLastReadDate = ""
                Prefs.readingChallengeEndDate = ReadingChallengeWidgetRepository.READING_CHALLENGE_END_DATE
                Prefs.readingChallengeStartDate = ReadingChallengeWidgetRepository.READING_CHALLENGE_START_DATE
                syncFromPrefs()
                updateWidgetsExplicitly()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Reset All", color = WikipediaTheme.colors.primaryColor)
        }
    }
}
