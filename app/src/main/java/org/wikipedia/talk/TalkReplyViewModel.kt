package org.wikipedia.talk

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.talk.template.TalkTemplatesActivity
import org.wikipedia.talk.template.TalkTemplatesRepository
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class TalkReplyViewModel(bundle: Bundle) : ViewModel() {
    private val talkTemplatesRepository = TalkTemplatesRepository(AppDatabase.instance.talkTemplateDao())

    var talkTemplateSaved = false
    val talkTemplatesList = mutableListOf<TalkTemplate>()

    val pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!
    val topic = bundle.parcelable<ThreadItem>(TalkReplyActivity.EXTRA_TOPIC)
    val isFromDiff = bundle.getBoolean(TalkReplyActivity.EXTRA_FROM_DIFF, false)
    val isNewTopic = topic == null && !isFromDiff
    val postReplyData = SingleLiveData<Resource<Long>>()
    val saveTemplateData = SingleLiveData<Resource<TalkTemplate>>()
    val selectedTemplate = bundle.parcelable<TalkTemplate>(TalkTemplatesActivity.EXTRA_SELECTED_TEMPLATE)
    val isSavedTemplate = bundle.getBoolean(TalkTemplatesActivity.EXTRA_SAVED_TEMPLATE, false)
    val templateManagementMode = bundle.getBoolean(TalkTemplatesActivity.EXTRA_TEMPLATE_MANAGEMENT, false)

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

    fun saveTemplate(title: String, subject: String, body: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            saveTemplateData.postValue(Resource.Error(throwable))
        }) {
            val orderNumber = talkTemplatesRepository.getLastOrderNumber() + 1
            val talkTemplate = TalkTemplate(type = 0, order = orderNumber, title = title, subject = subject, message = body)
            talkTemplatesRepository.insertTemplate(talkTemplate)
            saveTemplateData.postValue(Resource.Success(talkTemplate))
        }
    }

    fun updateTemplate(title: String, subject: String, body: String, talkTemplate: TalkTemplate) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            saveTemplateData.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                talkTemplate.apply {
                    this.title = title
                    this.subject = subject
                    this.message = body
                }
                talkTemplatesRepository.updateTemplate(talkTemplate)
                talkTemplatesList.find { it == talkTemplate }?.apply {
                    this.title = title
                    this.subject = subject
                    this.message = body
                }
                saveTemplateData.postValue(Resource.Success(talkTemplate))
            }
        }
    }

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalkReplyViewModel(bundle) as T
        }
    }
}
