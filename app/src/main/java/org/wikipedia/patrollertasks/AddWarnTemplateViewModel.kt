package org.wikipedia.patrollertasks

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.patrollertasks.db.WarnTemplate

class AddWarnTemplateViewModel(bundle: Bundle) : ViewModel() {
    private val warnTemplatesRepository = WarnTemplatesRepository(AppDatabase.instance.warnTemplateDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun saveTemplate(title: String, subject: String, body: String) {
        viewModelScope.launch(handler) {
            val orderNumber = warnTemplatesRepository.getLastOrderNumber() + 1
            val warnTemplate = WarnTemplate(order = orderNumber, title = title, subject = subject, message = body)
            warnTemplatesRepository.insertWarnTemplate(warnTemplate)
            _uiState.value = UiState.Success()
        }
    }

    open class UiState {
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
