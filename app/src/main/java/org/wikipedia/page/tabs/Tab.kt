package org.wikipedia.page.tabs

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle

@Entity
@Serializable
class Tab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var order: Int = 0,
    var backStackIds: String = ""
) {
    var backStackPosition: Int = -1
        get() = if (field < 0) backStack.size - 1 else field

    // The value of backStack will be initialized from the database in TabHelper
    @Ignore
    var backStack = mutableListOf<PageBackStackItem>()

    fun setBackStackIds(ids: List<Long>) {
        backStackIds = ids.joinToString(separator = ",")
    }

    fun getBackStackIds(): List<Long> {
        return if (backStackIds.isEmpty()) {
            emptyList()
        } else {
            backStackIds.split(",").mapNotNull { it.toLongOrNull() }
        }
    }

    fun getBackStackPositionTitle(): PageTitle? {
        return backStack.getOrNull(backStackPosition)?.getPageTitle()
    }

    fun setBackStackPositionTitle(title: PageTitle) {
        getBackStackPositionTitle()?.run {
            backStack[backStackPosition] = PageBackStackItem(
                apiTitle = title.prefixedText,
                displayTitle = title.displayText,
                langCode = title.wikiSite.languageCode,
                namespace = title.namespace,
                thumbUrl = title.thumbUrl,
                description = title.description,
                extract = title.extract
            )
        }
    }

    fun canGoBack(): Boolean {
        return backStackPosition > 0
    }

    fun canGoForward(): Boolean {
        return backStackPosition < backStack.size - 1
    }

    fun moveForward() {
        if (backStackPosition < backStack.size - 1) {
            backStackPosition++
        }
    }

    fun moveBack() {
        if (backStackPosition > 0) {
            backStackPosition--
        }
    }

    fun pushBackStackItem(item: PageBackStackItem) {
        // remove all backstack items past the current position
        while (backStack.size > backStackPosition + 1) {
            backStack.removeAt(backStackPosition + 1)
        }
        backStack.add(item)
        backStackPosition = backStack.size - 1
    }

    fun clearBackstack() {
        backStack.clear()
        backStackPosition = -1
    }

    fun squashBackstack() {
        backStack.lastOrNull()?.let {
            backStack.clear()
            backStack.add(it)
            backStackPosition = 0
        }
    }
}
