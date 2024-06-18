package org.wikipedia.talk.template

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.TalkReplyActivity
import org.wikipedia.talk.TalkReplyActivity.Companion.EXTRA_TEMPLATE_MANAGEMENT
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import java.util.Collections

class TalkTemplatesViewModel(bundle: Bundle) : ViewModel() {

    private val talkTemplatesRepository = TalkTemplatesRepository(AppDatabase.instance.talkTemplateDao())
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }
    private val actionExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _actionState.value = ActionState.Error(throwable)
    }
    val talkTemplatesList = mutableListOf<TalkTemplate>()
    val savedTemplatesList = mutableListOf<TalkTemplate>()

    private val _uiState = MutableStateFlow(Resource<Unit>())
    val uiState = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow(ActionState())
    val actionState = _actionState.asStateFlow()

    val templateManagementMode = bundle.getBoolean(EXTRA_TEMPLATE_MANAGEMENT, false)
    val pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!
    val fromRevisionId = bundle.getLong(TalkReplyActivity.FROM_REVISION_ID)
    val toRevisionId = bundle.getLong(TalkReplyActivity.TO_REVISION_ID)

    init {
        loadTalkTemplates()
        loadSavedTemplates()
    }

    fun loadTalkTemplates() {
        viewModelScope.launch(exceptionHandler) {
            talkTemplatesList.clear()
            _uiState.value = Resource.Loading()
            talkTemplatesList.addAll(talkTemplatesRepository.getAllTemplates())
            _uiState.value = Resource.Success(Unit)
        }
    }

    private fun loadSavedTemplates() {
        val langCode = pageTitle.wikiSite.languageCode
        for (i in savedMessagesSubjectList.indices) {
            val subjectString = if (i == 0) "" else L10nUtil.getStringForArticleLanguage(langCode, savedMessagesSubjectList[i])
            val bodyString = L10nUtil.getStringForArticleLanguage(langCode, savedMessagesBodyList[i])
            val talkTemplate = TalkTemplate(0, 0, -1, savedMessagesTitleList[i], subjectString, bodyString)
            savedTemplatesList.add(talkTemplate)
        }
    }

    fun swapList(oldPosition: Int, newPosition: Int) {
        Collections.swap(talkTemplatesList, oldPosition, newPosition)
        resetOrder()
    }

    private fun resetOrder() {
        for (i in talkTemplatesList.indices) {
            talkTemplatesList[i].order = i + 1
        }
    }

    fun updateItemOrder() {
        viewModelScope.launch(exceptionHandler) {
            talkTemplatesRepository.updateTemplates(talkTemplatesList)
        }
    }

    fun saveTemplates(talkTemplates: List<TalkTemplate>) {
        viewModelScope.launch(actionExceptionHandler) {
            talkTemplates.forEach { talkTemplatesRepository.insertTemplate(it) }
            talkTemplatesList.addAll(talkTemplates)
            _actionState.value = ActionState.Added()
        }
    }

    fun deleteTemplates(talkTemplates: List<TalkTemplate>) {
        viewModelScope.launch(actionExceptionHandler) {
            talkTemplatesRepository.deleteTemplates(talkTemplates)
            talkTemplatesList.removeAll(talkTemplates)
            resetOrder()
            talkTemplatesRepository.updateTemplates(talkTemplatesList)
            _actionState.value = ActionState.Deleted(talkTemplates.size)
        }
    }

    open class ActionState {
        class Added : ActionState()
        class Deleted(val size: Int) : ActionState()
        class Error(val throwable: Throwable) : ActionState()
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalkTemplatesViewModel(bundle) as T
        }
    }

    companion object {
        // This is for data analytics only
        private val savedMessagesTitleList = listOf("", "vandalism", "edit_tests", "neutral", "translation", "conflict_interest", "final", "copyright", "leave_summary", "no_censor", "improvement")

        private val savedMessagesSubjectList = listOf(-1, R.string.patroller_saved_message_title_vandalism, R.string.patroller_saved_message_title_editing_tests, R.string.patroller_saved_message_title_npov,
            R.string.patroller_saved_message_title_auto_trans, R.string.patroller_saved_message_title_coi_rem, R.string.patroller_saved_message_title_final_warning,
            R.string.patroller_saved_message_title_copy_vio, R.string.patroller_saved_message_title_edit_summary_reminder, R.string.patroller_saved_message_title_do_not_censor, R.string.patroller_saved_message_title_art_imp)

        private val savedMessagesBodyList = listOf(
            R.string.talk_warn_saved_messages_usage_instruction, R.string.patroller_saved_message_body_vandalism, R.string.patroller_saved_message_body_editing_tests, R.string.patroller_saved_message_body_npov,
            R.string.patroller_saved_message_body_auto_trans, R.string.patroller_saved_message_body_coi_rem, R.string.patroller_saved_message_body_final_warning,
            R.string.patroller_saved_message_body_copy_vio, R.string.patroller_saved_message_body_edit_summary, R.string.patroller_saved_message_body_do_not_censor, R.string.patroller_saved_message_body_art_imp)
    }
}
