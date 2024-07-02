package org.wikipedia.auth

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Build
import androidx.core.os.bundleOf
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil
import org.wikipedia.login.LoginResult
import org.wikipedia.util.log.L.d
import org.wikipedia.util.log.L.logRemoteErrorIfProd
import java.util.*

object AccountUtil {

    fun updateAccount(response: AccountAuthenticatorResponse?, result: LoginResult) {
        if (createAccount(result.userName!!, result.password!!)) {
            response?.onResult(bundleOf(AccountManager.KEY_ACCOUNT_NAME to result.userName,
                    AccountManager.KEY_ACCOUNT_TYPE to accountType()))
        } else {
            response?.onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "")
            d("account creation failure")
            return
        }
        setPassword(result.password)
        groups = result.groups
    }

    val isLoggedIn: Boolean
        get() = account() != null

    val userName: String?
        get() {
            val account = account()
            return account?.name
        }

    val password: String?
        get() {
            val account = account()
            return if (account == null) null else accountManager().getPassword(account)
        }

    var groups: Set<String>
        get() {
            val account = account() ?: return emptySet()
            val setStr = accountManager().getUserData(account, WikipediaApp.instance.getString(R.string.preference_key_login_groups))
            return if (setStr.isNullOrEmpty()) emptySet() else (JsonUtil.decodeFromString(setStr) ?: emptySet())
        }
        set(groups) {
            val account = account() ?: return
            accountManager().setUserData(account,
                    WikipediaApp.instance.getString(R.string.preference_key_login_groups),
                    JsonUtil.encodeToString(groups))
        }

    fun isMemberOf(groups: Set<String?>): Boolean {
        return groups.isNotEmpty() && !Collections.disjoint(groups, AccountUtil.groups)
    }

    fun removeAccount() {
        val account = account()
        if (account != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager().removeAccountExplicitly(account)
            } else {
                accountManager().removeAccount(account, null, null)
            }
        }
    }

    fun supported(account: Account): Boolean {
        return account == account()
    }

    fun account(): Account? {
        return try {
            accountManager().getAccountsByType(accountType()).firstOrNull()
        } catch (e: SecurityException) {
            logRemoteErrorIfProd(e)
            null
        }
    }

    fun accountType(): String {
        return WikipediaApp.instance.getString(R.string.account_type)
    }

    private fun createAccount(userName: String, password: String): Boolean {
        var account = account()
        if (account == null || account.name.isNullOrEmpty() || account.name != userName) {
            removeAccount()
            account = Account(userName, accountType())
            return accountManager().addAccountExplicitly(account, password, null)
        }
        return true
    }

    private fun setPassword(password: String) {
        val account = account()
        if (account != null) {
            accountManager().setPassword(account, password)
        }
    }

    private fun accountManager(): AccountManager {
        return AccountManager.get(WikipediaApp.instance)
    }
}
