package org.wikipedia.talk

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class TalkReplyViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(Constants.ARG_TITLE)!!
    val topic = bundle.getParcelable<ThreadItem>(TalkReplyActivity.EXTRA_TOPIC)
    val isNewTopic = topic == null
    val postReplyData = SingleLiveData<Resource<Long>>()

    fun postReply(subject: String, body: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            postReplyData.postValue(Resource.Error(throwable))
        }) {
            val token = ServiceFactory.get(pageTitle.wikiSite).getToken().query?.csrfToken()!!
            val response = if (topic != null) {
                ServiceFactory.get(pageTitle.wikiSite).postTalkPageTopicReply(pageTitle.prefixedText, topic.id, body, token)
            } else {
                ServiceFactory.get(pageTitle.wikiSite).postTalkPageTopic(pageTitle.prefixedText, subject, body, token)
            }
            postReplyData.postValue(Resource.Success(response.result!!.newRevId))
        }
    }

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalkReplyViewModel(bundle) as T
        }
    }
}
