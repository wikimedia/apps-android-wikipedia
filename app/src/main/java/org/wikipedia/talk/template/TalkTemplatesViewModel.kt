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

    fun loadTalkTemplates() {
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

    fun updateTalkTemplate(title: String, subject: String, body: String, talkTemplate: TalkTemplate, position: Int) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                talkTemplate.title = title
                talkTemplate.subject = subject
                talkTemplate.message = body
                talkTemplatesRepository.updateTemplate(talkTemplate)
                _uiState.value = UiState.Saved(position)
            }
        }
    }

    fun deleteTemplate(talkTemplate: TalkTemplate, position: Int) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                talkTemplatesRepository.deleteTemplate(talkTemplate)
                talkTemplatesList.remove(talkTemplate)
                _uiState.value = UiState.Deleted(position)
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
