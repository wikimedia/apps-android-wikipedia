package org.wikipedia.talk.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.talk.db.TalkTemplate
import java.util.Collections

class TalkTemplatesViewModel : ViewModel() {

    private val talkTemplatesRepository = TalkTemplatesRepository(AppDatabase.instance.talkTemplateDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    val talkTemplatesList = mutableListOf<TalkTemplate>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    var resetState = false // TODO: verify this if this is really needed. It fixes view re-creation after screen rotation after saved/deleted item from the list/

    init {
        loadTalkTemplates()
    }

    fun loadTalkTemplates() {
        resetState = false
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                talkTemplatesList.clear()
                _uiState.value = UiState.Loading()
                talkTemplatesList.addAll(talkTemplatesRepository.getAllTemplates())
                _uiState.value = UiState.Success()
            }
        }
    }

    fun swapList(oldPosition: Int, newPosition: Int) {
        Collections.swap(talkTemplatesList, oldPosition, newPosition)
        resetOrder()
    }

    private fun resetOrder() {
        for (i in talkTemplatesList.indices) {
            talkTemplatesList[i].order = i
        }
    }

    fun updateItemOrder() {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                talkTemplatesRepository.updateTemplates(talkTemplatesList)
            }
        }
    }

    fun updateTalkTemplate(title: String, subject: String, body: String, talkTemplate: TalkTemplate) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                talkTemplate.title = title
                talkTemplate.subject = subject
                talkTemplate.message = body
                talkTemplatesRepository.updateTemplate(talkTemplate)
                talkTemplatesList.find { it == talkTemplate }?.apply {
                    this.title = title
                    this.subject = subject
                    this.message = body
                }
                resetState = true
                _uiState.value = UiState.Saved(talkTemplate.order)
            }
        }
    }

    fun deleteTemplate(talkTemplate: TalkTemplate) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                talkTemplatesRepository.deleteTemplate(talkTemplate)
                talkTemplatesList.remove(talkTemplate)
                resetOrder()
                talkTemplatesRepository.updateTemplates(talkTemplatesList)
                resetState = true
                _uiState.value = UiState.Deleted(talkTemplate.order)
            }
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Saved(val position: Int) : UiState()
        class Deleted(val position: Int) : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
