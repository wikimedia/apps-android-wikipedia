package org.wikipedia.usercontrib

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import java.util.*

class UserInformationDialogViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    var userName: String = bundle.getString(UserInformationDialog.USERNAME_ARG)!!

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchUserInformation()
    }

    private fun fetchUserInformation() {
        viewModelScope.launch(handler) {
            _uiState.value = UiState.Loading()
            val userInfo = withContext(Dispatchers.IO) {
                ServiceFactory.get(WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)).globalUserInfo(userName)
            }
            userInfo.query?.globalUserInfo?.let {
                val editCount = String.format("%,d", it.editCount)
                _uiState.value = UiState.Success(editCount, it.registrationDate)
            } ?: run {
                _uiState.value = UiState.Error(Throwable("Cannot fetch user information."))
            }
        }
    }

    open class UiState {
        class Loading : UiState()
        class Success(val editCount: String, val registrationDate: Date) : UiState()
        class Error(val throwable: Throwable) : UiState()
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserInformationDialogViewModel(bundle) as T
        }
    }
}
