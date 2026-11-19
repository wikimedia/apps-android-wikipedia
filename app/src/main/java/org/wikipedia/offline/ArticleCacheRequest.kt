package org.wikipedia.offline

import kotlinx.coroutines.Job
import org.wikipedia.readinglist.ArticleKey

class ArticleCacheRequest(val key: ArticleKey, val download: OfflineCacheWrite? = null)

/** A request started before OFF cannot publish after a later ON. */
class OfflineCacheWrite private constructor(val key: ArticleKey, private val worker: Job?) : AutoCloseable {
    @Volatile
    private var active = true

    fun isCurrent() = active && worker?.isActive != false

    override fun close() {
        synchronized(writes) {
            active = false
            writes[key]?.let { entries ->
                entries.remove(this)
                if (entries.isEmpty()) writes.remove(key)
            }
        }
    }

    companion object {
        private val writes = mutableMapOf<ArticleKey, MutableSet<OfflineCacheWrite>>()

        fun start(key: ArticleKey, worker: Job? = null): OfflineCacheWrite = synchronized(writes) {
            val entries = writes.getOrPut(key) { mutableSetOf() }
            if (worker != null) {
                entries.removeAll { previous ->
                    if (previous.worker == null) false else {
                        previous.active = false
                        true
                    }
                }
            }
            OfflineCacheWrite(key, worker).also { entries.add(it) }
        }

        fun invalidate(key: ArticleKey) {
            synchronized(writes) {
                writes.remove(key)?.forEach { it.active = false }
            }
        }
    }
}
