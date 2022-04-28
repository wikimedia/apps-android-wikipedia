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
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class TalkReplyViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(TalkReplyActivity.EXTRA_PAGE_TITLE)!!
    val topic = bundle.getParcelable<ThreadItem>(TalkReplyActivity.EXTRA_TOPIC)
    val isNewTopic = topic == null

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalkReplyViewModel(bundle) as T
        }
    }
}
