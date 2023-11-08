package org.wikipedia.talk.template

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.talk.db.TalkTemplate

class AddTemplateViewModel(bundle: Bundle) : ViewModel() {
    private val talkTemplatesRepository = TalkTemplatesRepository(AppDatabase.instance.talkTemplateDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    val talkTemplateId = bundle.getInt(AddTemplateActivity.EXTRA_TEMPLATE_ID)
    val talkTemplatesList = mutableListOf<TalkTemplate>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    fun saveTemplate(title: String, subject: String, body: String) {
        viewModelScope.launch(handler) {
            val orderNumber = talkTemplatesRepository.getLastOrderNumber() + 1
            val talkTemplate = TalkTemplate(type = 0, order = orderNumber, title = title, subject = subject, message = body)
            talkTemplatesRepository.insertTemplate(talkTemplate)
            _uiState.value = UiState.Saved()
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch(handler) {
            talkTemplatesList.addAll(talkTemplatesRepository.getAllTemplates())
            _uiState.value = UiState.Success()
        }
    }

    open class UiState {
        class Success : UiState()
        class Saved : UiState()
        class Error(val throwable: Throwable) : UiState()
    }

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTemplateViewModel(bundle) as T
        }
    }
}
