package org.wikipedia.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import org.wikipedia.WikipediaApp

class LoginViewModel : ViewModel() {

    // Null while logging in is in progress.
    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    // Delivered exactly once, even if they happen while the activity is being recreated.
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAuthorizationStarted() {
        _error.value = null
    }

    fun onAuthorizationResult(data: Intent?) {
        val response = data?.let { AuthorizationResponse.fromIntent(it) }
        val exception = data?.let { AuthorizationException.fromIntent(it) }
        when {
            response != null -> completeLogin(response)
            exception == null || isCanceledByUser(exception) -> _events.trySend(Event.Canceled)
            else -> onError(exception)
        }
    }

    fun onError(throwable: Throwable) {
        _error.value = throwable
        _events.trySend(Event.Failed(throwable))
    }

    private fun completeLogin(response: AuthorizationResponse) {
        _error.value = null
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable -> onError(throwable) }) {
            WikipediaApp.instance.oauthClient.handleAuthorizationResponse(response)
            _events.send(Event.LoggedIn)
        }
    }

    // Either they closed the browser, or declined to authorize the app.
    private fun isCanceledByUser(exception: AuthorizationException): Boolean {
        return exception == AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW ||
                exception == AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED
    }

    sealed interface Event {
        data object LoggedIn : Event
        data object Canceled : Event
        data class Failed(val throwable: Throwable) : Event
    }
}
