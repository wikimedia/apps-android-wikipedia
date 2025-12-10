package org.wikimedia.testkitchen

interface LogAdapter {
    fun info(message: String, vararg args: Any)
    fun warn(message: String, vararg args: Any)
    fun error(message: String, vararg args: Any)
}

/**
 * Stub implementation of LogAdapter (intended to be overridden by clients.)
 */
class LogAdapterImpl : LogAdapter {
    override fun info(message: String, vararg args: Any) {
    }

    override fun warn(message: String, vararg args: Any) {
    }

    override fun error(message: String, vararg args: Any) {
    }
}