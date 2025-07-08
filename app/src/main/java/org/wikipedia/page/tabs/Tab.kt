package org.wikipedia.page.tabs

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import org.wikipedia.page.tabs.PageBackStackItem
import org.wikipedia.page.PageTitle

@Entity
@Serializable
class Tab(
    @PrimaryKey(autoGenerate = true) var id: Long = 0
) {
    var backStackPosition: Int = -1
        get() = if (field < 0) backStack.size - 1 else field

    fun getBackStackPositionTitle(): PageTitle? {
        return backStack.getOrNull(backStackPosition)?.title
    }

    fun setBackStackPositionTitle(title: PageTitle) {
        getBackStackPositionTitle()?.run {
            backStack[backStackPosition].title = title
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
