package org.wikipedia.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.StringUtil

class CreateAccountActivityViewModel : ViewModel() {
    private val _createAccountInfoState = MutableStateFlow(AccountInfoState())
    val createAccountInfoState = _createAccountInfoState.asStateFlow()

    private val _doCreateAccountState = MutableStateFlow(CreateAccountState())
    val doCreateAccountState = _doCreateAccountState.asStateFlow()

    fun createAccountInfo() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _createAccountInfoState.value = AccountInfoState.Error(throwable)
        }) {
            val response = withContext(Dispatchers.IO) {
                ServiceFactory.get(WikipediaApp.instance.wikiSite).getAuthManagerInfo()
            }
            val token = response.query?.createAccountToken()
            val captchaId = response.query?.captchaId()
            if (token.isNullOrEmpty()) {
                _createAccountInfoState.value = AccountInfoState.InvalidToken
            } else if (!captchaId.isNullOrEmpty()) {
                _createAccountInfoState.value = AccountInfoState.HandleCaptcha(token, captchaId)
            } else {
                _createAccountInfoState.value = AccountInfoState.DoCreateAccount(token)
            }
        }
    }

    fun doCreateAccount(token: String, captchaId: String, captchaWord: String, userName: String, password: String, repeat: String, email: String?) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _doCreateAccountState.value = CreateAccountState.Error(throwable)
        }) {
            val response = withContext(Dispatchers.IO) {
                ServiceFactory.get(WikipediaApp.instance.wikiSite).postCreateAccount(userName, password, repeat, token, Service.WIKIPEDIA_URL,
                    email, captchaId, captchaWord)
            }
            if ("PASS" == response.status) {
                _doCreateAccountState.value = CreateAccountState.Pass(response.user)
            } else {
                throw CreateAccountException(StringUtil.removeStyleTags(response.message))
            }
        }
    }

    open class AccountInfoState {
        data class DoCreateAccount(val token: String) : AccountInfoState()
        data class HandleCaptcha(val token: String?, val captchaId: String) : AccountInfoState()
        data object InvalidToken : AccountInfoState()
        data class Error(val throwable: Throwable) : AccountInfoState()
    }

    open class CreateAccountState {
        data class Pass(val userName: String) : CreateAccountState()
        data class Error(val throwable: Throwable) : CreateAccountState()
    }
}