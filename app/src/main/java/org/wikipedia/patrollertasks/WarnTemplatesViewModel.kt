package org.wikipedia.patrollertasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.patrollertasks.db.WarnTemplate

class WarnTemplatesViewModel : ViewModel() {

    private val warnTemplatesRepository = WarnTemplatesRepository(AppDatabase.instance.warnTemplateDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    val warnTemplatesList = mutableListOf<WarnTemplate>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadWarnTemplates()
    }

    fun loadWarnTemplates() {
        viewModelScope.launch(handler) {
            warnTemplatesList.addAll(warnTemplatesRepository.getAllWarnTemplates())
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
