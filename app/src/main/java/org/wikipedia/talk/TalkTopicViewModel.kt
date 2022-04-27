package org.wikipedia.talk

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.*
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class TalkTopicViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(TalkTopicActivity.EXTRA_PAGE_TITLE)!!
    val topicId = bundle.getString(TalkTopicActivity.EXTRA_TOPIC)!!

    var topic: ThreadItem? = null
    val sectionId get() = threadItems.indexOf(topic)
    val threadItems = mutableListOf<ThreadItem>()
    val flattenedThreadItems = mutableListOf<ThreadItem>()
    var subscribed = false
        private set
    val uiState = MutableLiveData<Resource<List<ThreadItem>>>()
    val subscribeData = SingleLiveData<Resource<Boolean>>()

    private val talkPageSeenRepository = TalkPageSeenRepository(AppDatabase.instance.talkPageSeenDao())

    init {
        loadTopic()
    }

    fun loadTopic() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            uiState.postValue(Resource.Error(throwable))
        }) {
            val discussionToolsInfoResponse = async { ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopics(pageTitle.prefixedText) }
            val subscribeResponse = async { ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopicSubscriptions(topicId) }

            topic = discussionToolsInfoResponse.await().pageInfo?.threads.orEmpty().find { it.name == topicId }
            val res = subscribeResponse.await()
            subscribed = res.subscriptions[topicId] == 1

            topic?.name?.let {
                if (it.isNotEmpty()) {
                    talkPageSeenRepository.insertTalkPageSeen(TalkPageSeen(it))
                }
            }

            threadItems.clear()
            threadItems.addAll(topic?.replies.orEmpty())

            // By default expand the first level of the thread
            threadItems.forEach { it.isExpanded = true }
            updateFlattenedThreadItems()

            uiState.postValue(Resource.Success(threadItems))
        }
    }

    fun toggleSubscription() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            subscribeData.postValue(Resource.Error(throwable))
        }) {
            val token = ServiceFactory.get(pageTitle.wikiSite).getCsrfToken().query?.csrfToken()!!
            val response = ServiceFactory.get(pageTitle.wikiSite).subscribeTalkPageTopic(pageTitle.prefixedText, topicId, token, if (!subscribed) true else null)
            subscribed = response.status!!.subscribe
            subscribeData.postValue(Resource.Success(subscribed))
        }
    }

    fun toggleItemExpanded(item: ThreadItem): DiffUtil.DiffResult {
        val prevList = mutableListOf<ThreadItem>()
        prevList.addAll(flattenedThreadItems)
        item.isExpanded = !item.isExpanded

        updateFlattenedThreadItems()

        return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return prevList.size
            }

            override fun getNewListSize(): Int {
                return flattenedThreadItems.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return prevList[oldItemPosition] == flattenedThreadItems[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return prevList[oldItemPosition].id == flattenedThreadItems[newItemPosition].id
            }
        })
    }

    private fun updateFlattenedThreadItems() {
        flattenedThreadItems.clear()
        flattenThreadLevel(threadItems, flattenedThreadItems)
        for (i in flattenedThreadItems.indices) {
            flattenedThreadItems[i].isLastSibling = i > 0 && flattenedThreadItems[i].level > 1 && (if(i < flattenedThreadItems.size - 1) flattenedThreadItems[i + 1].level < flattenedThreadItems[i].level else true)
        }
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
