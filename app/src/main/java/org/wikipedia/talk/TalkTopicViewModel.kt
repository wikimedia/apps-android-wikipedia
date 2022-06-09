package org.wikipedia.talk

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class TalkTopicViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(TalkTopicActivity.EXTRA_PAGE_TITLE)!!
    val topicName = bundle.getString(TalkTopicActivity.EXTRA_TOPIC_NAME)!!
    val topicId = bundle.getString(TalkTopicActivity.EXTRA_TOPIC_ID)!!
    var currentSearchQuery = bundle.getString(TalkTopicActivity.EXTRA_SEARCH_QUERY)
    var scrollTargetId = bundle.getString(TalkTopicActivity.EXTRA_REPLY_ID)

    var topic: ThreadItem? = null
    val sectionId get() = threadItems.indexOf(topic)
    val threadItems = mutableListOf<ThreadItem>()
    val flattenedThreadItems = mutableListOf<ThreadItem>()
    var subscribed = false
        private set
    val threadItemsData = MutableLiveData<Resource<List<ThreadItem>>>()
    val subscribeData = SingleLiveData<Resource<Boolean>>()
    val undoResponseData = SingleLiveData<Resource<Boolean>>()

    var undoSubject: CharSequence? = null
    var undoBody: CharSequence? = null
    var undoTopicId: String? = null

    val isExpandable: Boolean get() {
        return topic?.allReplies.orEmpty().any { it.level > 1 }
    }

    val isFullyExpanded: Boolean get() {
        return !currentSearchQuery.isNullOrEmpty() || flattenedThreadItems.size == topic?.allReplies?.size
    }

    init {
        loadTopic()
    }

    fun loadTopic() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            threadItemsData.postValue(Resource.Error(throwable))
        }) {
            val discussionToolsInfoResponse = async { ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopics(pageTitle.prefixedText) }
            val subscribeResponse = async { ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopicSubscriptions(topicName) }
            val oldItemsFlattened = topic?.allReplies.orEmpty()

            topic = discussionToolsInfoResponse.await().pageInfo?.threads.orEmpty().find { it.id == topicId }

            val res = subscribeResponse.await()
            subscribed = res.subscriptions[topicName] == 1

            threadSha(topic)?.let {
                AppDatabase.instance.talkPageSeenDao().insertTalkPageSeen(TalkPageSeen(it))
            }

            val newItemsFlattened = topic?.allReplies.orEmpty().filter { it.id !in oldItemsFlattened.map { item -> item.id } }

            if (oldItemsFlattened.isNotEmpty() && newItemsFlattened.isNotEmpty()) {
                if (AccountUtil.isLoggedIn) {
                    scrollTargetId = newItemsFlattened.findLast { it.author == AccountUtil.userName }?.id
                }
                if (scrollTargetId.isNullOrEmpty()) {
                    scrollTargetId = newItemsFlattened.first().id
                }
            }

            threadItems.clear()
            threadItems.addAll(topic?.replies.orEmpty())

            if (scrollTargetId.isNullOrEmpty()) {
                // By default, expand or collapse based on user preference
                expandOrCollapseAll()
            } else {
                // If we have a scroll target, make sure we're expanded to view the target
                topic?.allReplies?.forEach { it.isExpanded = true }
            }
            updateFlattenedThreadItems()

            threadItemsData.postValue(Resource.Success(threadItems))
        }
    }

    fun toggleSubscription() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            subscribeData.postValue(Resource.Error(throwable))
        }) {
            val token = ServiceFactory.get(pageTitle.wikiSite).getToken().query?.csrfToken()!!
            val response = ServiceFactory.get(pageTitle.wikiSite).subscribeTalkPageTopic(pageTitle.prefixedText, topicName, token, if (!subscribed) true else null)
            subscribed = response.status!!.subscribe
            subscribeData.postValue(Resource.Success(subscribed))
        }
    }

    fun toggleItemExpanded(item: ThreadItem): DiffUtil.DiffResult {
        val prevList = mutableListOf<ThreadItem>()
        prevList.addAll(flattenedThreadItems)
        item.isExpanded = !item.isExpanded
        updateFlattenedThreadItems()
        return getDiffResult(prevList, flattenedThreadItems)
    }

    fun expandOrCollapseAll(): DiffUtil.DiffResult {
        val prevList = mutableListOf<ThreadItem>()
        prevList.addAll(flattenedThreadItems)
        val expand = Prefs.talkTopicExpandOrCollapseByDefault
        topic?.allReplies?.forEach { if (it.level > 1) it.isExpanded = expand }
        updateFlattenedThreadItems()
        return getDiffResult(prevList, flattenedThreadItems)
    }

    private fun getDiffResult(prevList: List<ThreadItem>, newList: List<ThreadItem>): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return prevList.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return prevList[oldItemPosition] == newList[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return prevList[oldItemPosition].id == newList[newItemPosition].id
            }
        })
    }

    fun undo(undoRevId: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            undoResponseData.postValue(Resource.Error(throwable))
        }) {
            val token = ServiceFactory.get(pageTitle.wikiSite).getToken().query?.csrfToken()!!
            val response = ServiceFactory.get(pageTitle.wikiSite).postUndoEdit(title = pageTitle.prefixedText, undoRevId = undoRevId, token = token)
            undoResponseData.postValue(Resource.Success(response.edit!!.editSucceeded))
        }
    }

    fun findTopicById(id: String?): ThreadItem? {
        return topic?.allReplies?.find { it.id == id }
    }

    private fun threadSha(threadItem: ThreadItem?): String? {
        return threadItem?.let { it.name + "|" + it.allReplies.map { reply -> reply.timestamp }.maxOrNull() }
    }

    private fun updateFlattenedThreadItems() {
        flattenedThreadItems.clear()
        flattenThreadLevel(threadItems, flattenedThreadItems)
        for (i in flattenedThreadItems.indices) {
            flattenedThreadItems[i].isFirstTopLevel = false
            flattenedThreadItems[i].isLastSibling = i > 0 && flattenedThreadItems[i].level > 1 && (if (i < flattenedThreadItems.size - 1) flattenedThreadItems[i + 1].level < flattenedThreadItems[i].level else true)
        }
        flattenedThreadItems.find { it.level == 1 }?.isFirstTopLevel = true
    }

    private fun flattenThreadLevel(list: List<ThreadItem>, flatList: MutableList<ThreadItem>) {
        list.forEach {
            flatList.add(it)
            if (it.isExpanded) {
                flattenThreadLevel(it.replies, flatList)
            }
        }
    }

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalkTopicViewModel(bundle) as T
        }
    }
}
