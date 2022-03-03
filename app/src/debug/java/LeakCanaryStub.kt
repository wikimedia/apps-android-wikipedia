package org.wikipedia

import leakcanary.LeakCanary
import org.wikipedia.settings.Prefs

fun setupLeakCanary() {
    val enabled = Prefs.isMemoryLeakTestEnabled
    LeakCanary.config = LeakCanary.config.copy(dumpHeap = enabled)
}
