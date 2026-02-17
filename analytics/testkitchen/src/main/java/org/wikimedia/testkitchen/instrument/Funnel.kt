package org.wikimedia.testkitchen.instrument

import org.wikimedia.testkitchen.SessionController

class Funnel(
    val name: String? = null
) {
    val token = SessionController.generateSessionId()
    val sequence get() = _sequence

    private var _sequence: Int = 0

    fun touch() {
        _sequence++
    }
}
