package org.wikipedia.auth

import android.accounts.*
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import org.wikipedia.R
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.auth.AccountUtil.account
import org.wikipedia.auth.AccountUtil.accountType
import org.wikipedia.login.LoginActivity

class WikimediaAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {
    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle {
        return unsupportedOperation()
    }

    override fun addAccount(response: AccountAuthenticatorResponse, accountType: String,
                            authTokenType: String?, requiredFeatures: Array<String>?,
                            options: Bundle?): Bundle {
        return if (!supportedAccountType(accountType) || account() != null) {
            unsupportedOperation()
        } else addAccount(response)
    }

    override fun confirmCredentials(response: AccountAuthenticatorResponse,
                                    account: Account, options: Bundle?): Bundle {
        return unsupportedOperation()
    }

    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account,
                              authTokenType: String, options: Bundle?): Bundle {
        return unsupportedOperation()
    }

    override fun getAuthTokenLabel(authTokenType: String): String? {
        return if (supportedAccountType(authTokenType)) context.getString(R.string.wikimedia) else null
    }

    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account,
                                   authTokenType: String?, options: Bundle?): Bundle {
        return unsupportedOperation()
    }

    override fun hasFeatures(response: AccountAuthenticatorResponse,
                             account: Account, features: Array<String>): Bundle {
        return bundleOf(AccountManager.KEY_BOOLEAN_RESULT to false)
    }

    private fun supportedAccountType(type: String?): Boolean {
        return accountType() == type
    }

    private fun addAccount(response: AccountAuthenticatorResponse): Bundle {
        val intent = LoginActivity.newIntent(context, LoginFunnel.SOURCE_SYSTEM)
                .putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }

    private fun unsupportedOperation(): Bundle {
        return bundleOf(AccountManager.KEY_ERROR_CODE to AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                AccountManager.KEY_ERROR_MESSAGE to "") // HACK: the docs indicate that this is a required key bit it's not displayed to the user.
    }
}
