package org.wikipedia.games.onthisday

import org.wikipedia.settings.Prefs

enum class OnThisDayGameNotificationState {
    NO_INTERACTED,
    ENABLED,
    DISABLED
}

fun getOnThisDayGameNotificationState(): OnThisDayGameNotificationState {
    return OnThisDayGameNotificationState.valueOf(Prefs.otdNotificationState)
}
