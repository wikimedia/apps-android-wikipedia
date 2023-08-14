package org.wikipedia.talk.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.talk.db.TalkTemplate

class TalkTemplatesViewModel : ViewModel() {

    private val talkTemplatesRepository = TalkTemplatesRepository(AppDatabase.instance.talkTemplateDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    val talkTemplatesList = mutableListOf<TalkTemplate>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadWarnTemplates()
    }

    fun loadWarnTemplates() {
        viewModelScope.launch(handler) {
            talkTemplatesList.addAll(talkTemplatesRepository.getAllTemplates())
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
