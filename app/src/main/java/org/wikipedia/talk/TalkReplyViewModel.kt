package org.wikipedia.talk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.edit.EditTags
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.talk.template.TalkTemplatesRepository
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.util.log.L

class TalkReplyViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val talkTemplatesRepository = TalkTemplatesRepository(AppDatabase.instance.talkTemplateDao())

    var talkTemplateSaved = false
    val talkTemplatesList = mutableListOf<TalkTemplate>()

    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    val topic = savedStateHandle.get<ThreadItem>(TalkReplyActivity.EXTRA_TOPIC)
    val isFromDiff = savedStateHandle[TalkReplyActivity.EXTRA_FROM_DIFF] ?: false
    val selectedTemplate = savedStateHandle.get<TalkTemplate>(TalkReplyActivity.EXTRA_SELECTED_TEMPLATE)
    val isExampleTemplate = savedStateHandle[TalkReplyActivity.EXTRA_EXAMPLE_TEMPLATE] ?: false
    val templateManagementMode = savedStateHandle[TalkReplyActivity.EXTRA_TEMPLATE_MANAGEMENT] ?: false
    val fromRevisionId = savedStateHandle[TalkReplyActivity.FROM_REVISION_ID] ?: -1L
    val toRevisionId = savedStateHandle[TalkReplyActivity.TO_REVISION_ID] ?: -1L
    val isNewTopic = topic == null && !isFromDiff

    val postReplyData = SingleLiveData<Resource<Long>>()
    val saveTemplateData = SingleLiveData<Resource<TalkTemplate>>()
    var doesPageExist = false

    init {
        if (isFromDiff) {
            loadTemplates()
        }
        checkPageExists()
    }

    @Suppress("KotlinConstantConditions")
    private fun checkPageExists() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            ServiceFactory.get(pageTitle.wikiSite).getPageIds(pageTitle.prefixedText).let {
                doesPageExist = (it.query?.pages?.firstOrNull()?.pageId ?: 0) > 0
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            talkTemplatesList.clear()
            talkTemplatesList.addAll(talkTemplatesRepository.getAllTemplates())
        }
    }

    fun postReply(subject: String, body: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            postReplyData.postValue(Resource.Error(throwable))
        }) {
            val token = ServiceFactory.get(pageTitle.wikiSite).getToken().query?.csrfToken()!!
            val response = if (topic != null) {
                ServiceFactory.get(pageTitle.wikiSite).postTalkPageTopicReply(pageTitle.prefixedText, topic.id, body, token, tags = EditTags.APP_TALK_REPLY)
            } else {
                ServiceFactory.get(pageTitle.wikiSite).postTalkPageTopic(pageTitle.prefixedText, subject, body, token, tags = EditTags.APP_TALK_TOPIC)
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
