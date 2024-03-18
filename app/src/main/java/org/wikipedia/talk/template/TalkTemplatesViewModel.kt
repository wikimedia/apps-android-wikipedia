package org.wikipedia.talk.template

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.db.TalkTemplate
import java.util.*

class TalkTemplatesViewModel(bundle: Bundle) : ViewModel() {

    private val talkTemplatesRepository = TalkTemplatesRepository(AppDatabase.instance.talkTemplateDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    private val actionHandler = CoroutineExceptionHandler { _, throwable ->
        _actionState.value = ActionState.Error(throwable)
    }
    val talkTemplatesList = mutableListOf<TalkTemplate>()
    val savedTemplatesList = mutableListOf<TalkTemplate>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow(ActionState())
    val actionState = _actionState.asStateFlow()

    val templateManagementMode = bundle.getBoolean(TalkTemplatesActivity.EXTRA_TEMPLATE_MANAGEMENT, false)
    val pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!

    init {
        loadTalkTemplates()
        loadSavedTemplates()
    }

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

    private fun loadSavedTemplates() {
        val langCode = pageTitle.wikiSite.languageCode
        val context = WikipediaApp.instance.applicationContext
        for (i in TalkTemplatesFragment.savedMessagesTitleList.indices) {
            val talkTemplate = TalkTemplate(0, 0, -1, "",
                if (i == 0) "" else getLocaleStringResource(Locale(langCode), TalkTemplatesFragment.savedMessagesTitleList[i], context),
                getLocaleStringResource(Locale(langCode), TalkTemplatesFragment.savedMessagesBodyList[i], context))
            savedTemplatesList.add(talkTemplate)
        }
    }

    private fun getLocaleStringResource(requestedLocale: Locale, resourceId: Int, context: Context): String {
        val result: String
        val config = Configuration(context.resources.configuration)
        config.setLocale(requestedLocale)
        result = context.createConfigurationContext(config).getText(resourceId).toString()
        return result
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
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                talkTemplatesRepository.updateTemplates(talkTemplatesList)
            }
        }
    }

    fun saveTemplates(talkTemplates: List<TalkTemplate>) {
        viewModelScope.launch(actionHandler) {
            withContext(Dispatchers.IO) {
                talkTemplates.forEach { talkTemplatesRepository.insertTemplate(it) }
                talkTemplatesList.addAll(talkTemplates)
                _actionState.value = ActionState.Added()
            }
        }
    }

    fun deleteTemplates(talkTemplates: List<TalkTemplate>) {
        viewModelScope.launch(actionHandler) {
            withContext(Dispatchers.IO) {
                talkTemplatesRepository.deleteTemplates(talkTemplates)
                talkTemplatesList.removeAll(talkTemplates)
                resetOrder()
                talkTemplatesRepository.updateTemplates(talkTemplatesList)
                _actionState.value = ActionState.Deleted(talkTemplates.size)
            }
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
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
}
