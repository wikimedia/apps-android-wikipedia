package org.wikipedia.notifications.experiment

import android.content.Context
import android.icu.util.Calendar
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.isPm
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.recommended.RecommendedReadingListNotificationManager
import org.wikipedia.settings.Prefs
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

fun showNotificationsExpTimerPickerDialog(context: Context, onChanged: () -> Unit = {}) {
    val composeView = ComposeView(context)

    val dialog = MaterialAlertDialogBuilder(context)
        .setView(composeView)
        .show()

    composeView.setContent {
        BaseTheme {
            DevSettingsTimePicker(
                onCancel = {
                    Prefs.notificationsV1Exp1CustomTriggerTime = 0L
                    onChanged()
                    dialog.dismiss()
                },
                onSave = { timeInMillis ->
                    Prefs.notificationsV1Exp1CustomTriggerTime = timeInMillis
                    RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(context)
                    Toast.makeText(context, "Scheduled for ${Date(timeInMillis)}", Toast.LENGTH_SHORT).show()
                    onChanged()
                    dialog.dismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSettingsTimePicker(
    onCancel: () -> Unit = {},
    onSave: (Long) -> Unit = {}
) {
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = false
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = WikipediaTheme.colors.progressiveColor
            )
        ) {
            TimeInput(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = WikipediaTheme.colors.inactiveColor,
                    timeSelectorUnselectedContainerColor = WikipediaTheme.colors.paperColor,
                    timeSelectorSelectedContentColor = WikipediaTheme.colors.primaryColor,
                    timeSelectorUnselectedContentColor = WikipediaTheme.colors.secondaryColor,
                    periodSelectorBorderColor = WikipediaTheme.colors.borderColor,
                    periodSelectorSelectedContainerColor = WikipediaTheme.colors.inactiveColor,
                    periodSelectorSelectedContentColor = WikipediaTheme.colors.primaryColor,
                ),
                modifier = Modifier.wrapContentWidth()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(end = 16.dp)
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WikipediaTheme.colors.paperColor
                )
            ) {
                Text(
                    text = "Cancel/Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = WikipediaTheme.colors.progressiveColor
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Button(
                onClick = {
                    val selectedTimeInMilli = timeToMillis(
                        timePickerState.hour,
                        timePickerState.minute, timePickerState.isPm
                    )
                    onSave(selectedTimeInMilli)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WikipediaTheme.colors.progressiveColor
                )
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge,
                    color = WikipediaTheme.colors.paperColor
                )
            }
        }
    }
}

fun timeToMillis(hour: Int, minute: Int, isPm: Boolean): Long {
    return LocalDateTime.now()
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

@Preview
@Composable
fun DevSettingsTimePickerPreview() {
    BaseTheme {
        DevSettingsTimePicker()
    }
}
