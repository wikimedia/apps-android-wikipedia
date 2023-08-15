package org.wikipedia.talk.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    init {
        loadTalkTemplates()
    }

    fun loadTalkTemplates() {
        viewModelScope.launch(handler) {
            _uiState.value = UiState.Loading()
            talkTemplatesList.addAll(talkTemplatesRepository.getAllTemplates())
            _uiState.value = UiState.Success()
        }
    }

    fun swapList(oldPosition: Int, newPosition: Int) {
        Collections.swap(talkTemplatesList, oldPosition, newPosition)
    }

    fun updateItemOrder() {
        viewModelScope.launch(handler) {
            _uiState.value = UiState.Loading()
            talkTemplatesRepository.updateTemplates(talkTemplatesList)
            _uiState.value = UiState.Success()
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
