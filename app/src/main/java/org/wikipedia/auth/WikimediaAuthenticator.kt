package org.wikipedia.auth

import android.accounts.*
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import org.wikipedia.BuildConfig
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
        val bundle = Bundle()
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return bundle
    }

    private fun supportedAccountType(type: String?): Boolean {
        return accountType() == type
    }

    private fun addAccount(response: AccountAuthenticatorResponse): Bundle {
        val intent = LoginActivity.newIntent(context, LoginFunnel.SOURCE_SYSTEM)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    private fun unsupportedOperation(): Bundle {
        val bundle = Bundle()
        bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)

        // HACK: the docs indicate that this is a required key bit it's not displayed to the user.
        bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "")
        return bundle
    }

    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse, account: Account): Bundle {
        val result = super.getAccountRemovalAllowed(response, account)
        if (result.containsKey(AccountManager.KEY_BOOLEAN_RESULT) &&
                !result.containsKey(AccountManager.KEY_INTENT)) {
            val allowed = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT)
            if (allowed) {
                for (auth in SYNC_AUTHORITIES) {
                    ContentResolver.cancelSync(account, auth)
                }
            }
        }
        return result
    }

    companion object {
        private val SYNC_AUTHORITIES = arrayOf(BuildConfig.READING_LISTS_AUTHORITY)
    }
}
