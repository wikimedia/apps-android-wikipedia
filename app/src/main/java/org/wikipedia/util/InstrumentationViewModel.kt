package org.wikipedia.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InstrumentationViewModel : ViewModel() {
    private val _editCount = MutableStateFlow<Resource<Int>>(Resource.Loading())
    val editCount = _editCount.asStateFlow()

    init {
        fetchEditCount()
    }

    private fun fetchEditCount() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _editCount.value = Resource.Error(throwable)
        }) {
            val userEditCount = org.wikipedia.dataclient.ServiceFactory.get(org.wikipedia.dataclient.WikiSite.forLanguageCode(org.wikipedia.WikipediaApp.instance.appOrSystemLanguageCode)).globalUserInfo(org.wikipedia.auth.AccountUtil.userName)
            userEditCount.query?.globalUserInfo?.let { count ->
                _editCount.value = Resource.Success(count.editCount)
            } ?: run {
                _editCount.value = Resource.Error(Throwable("Cannot fetch user information."))
            }
        }
    }

    fun refreshEditCount() {
        fetchEditCount()
    }
}