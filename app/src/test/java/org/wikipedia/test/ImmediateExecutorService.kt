package org.wikipedia.test

import java.lang.UnsupportedOperationException
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class ImmediateExecutorService : AbstractExecutorService() {
    override fun shutdown() {
        throw UnsupportedOperationException()
    }

    override fun shutdownNow(): MutableList<Runnable?> {
        throw UnsupportedOperationException()
    }

    override fun isShutdown(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isTerminated(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun awaitTermination(l: Long, timeUnit: TimeUnit): Boolean {
        throw UnsupportedOperationException()
    }

    override fun execute(runnable: Runnable) {
        runnable.run()
    }
}
