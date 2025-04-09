package org.wikipedia.usercontrib

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.Resource
import java.util.Date

class UserInformationDialogViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    var userName = savedStateHandle.get<String>(UserInformationDialog.USERNAME_ARG)!!

    private val _uiState = MutableStateFlow(Resource<Pair<String, Date>>())
    val uiState = _uiState.asStateFlow()

    init {
        fetchUserInformation()
    }

    private fun fetchUserInformation() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            _uiState.value = Resource.Loading()
            val userInfo = ServiceFactory.get(WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)).globalUserInfo(userName)
            userInfo.query?.globalUserInfo?.let {
                val editCount = String.format("%,d", it.editCount)
                _uiState.value = Resource.Success(Pair(editCount, it.registrationDate))
            } ?: run {
                _uiState.value = Resource.Error(Throwable("Cannot fetch user information."))
            }
        }
    }
}
