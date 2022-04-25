package org.wikipedia.talk

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.DiscussionToolsEditResponse
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.edit.Edit
import org.wikipedia.page.PageTitle

class TalkTopicViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(TalkTopicActivity.EXTRA_PAGE_TITLE)!!
    val topicId = bundle.getString(TalkTopicActivity.EXTRA_TOPIC)!!

    var topic: ThreadItem? = null
    val sectionId get() = threadItems.indexOf(topic)
    val threadItems = mutableListOf<ThreadItem>()
    val flattenedThreadItems = mutableListOf<ThreadItem>()
    val uiState = MutableStateFlow(UiState())

    init {
        loadTopic()
    }

    fun loadTopic() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            uiState.value = UiState.LoadError(throwable)
        }) {
            val discussionToolsInfoResponse = async { ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopics(pageTitle.prefixedText) }

            threadItems.clear()
            threadItems.addAll(discussionToolsInfoResponse.await().pageInfo?.threads.orEmpty())

            topic = threadItems.find { it.id == topicId }

            flattenedThreadItems.clear()
            flattenThreadItem(topic?.replies.orEmpty(), flattenedThreadItems)

            uiState.value = UiState.LoadTopic(threadItems)
        }
    }

    private fun flattenThreadItem(list: List<ThreadItem>, flatList: MutableList<ThreadItem>) {
        list.forEach {
            flatList.add(it)
            flattenThreadItem(it.replies, flatList)
        }
    }

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalkTopicViewModel(bundle) as T
        }
    }

    open class UiState {
        data class LoadTopic(val threadItems: List<ThreadItem>) : UiState()
        data class LoadError(val throwable: Throwable) : UiState()
        data class DoEdit(val editResult: DiscussionToolsEditResponse.EditResult) : UiState()
        data class UndoEdit(val edit: Edit, val topicId: String, val undoneSubject: String, val undoneBody: String) : UiState()
        data class EditError(val throwable: Throwable) : UiState()
    }
}
