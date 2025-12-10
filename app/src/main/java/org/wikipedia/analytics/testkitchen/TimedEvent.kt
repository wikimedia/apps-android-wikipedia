package org.wikipedia.analytics.testkitchen

import org.wikipedia.util.ActiveTimer

open class TimedEvent : Event() {
    protected val timer = ActiveTimer()
}
