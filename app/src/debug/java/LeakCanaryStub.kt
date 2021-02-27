package org.wikipedia

import leakcanary.AppWatcher
import leakcanary.LeakCanary
import org.wikipedia.settings.Prefs

fun setupLeakCanary() {
    val enabled = Prefs.isMemoryLeakTestEnabled
    LeakCanary.config = LeakCanary.config.copy(dumpHeap = enabled)
    AppWatcher.config = AppWatcher.config.copy(watchActivities = enabled, watchFragments = enabled, watchFragmentViews = enabled, watchViewModels = enabled)
}
