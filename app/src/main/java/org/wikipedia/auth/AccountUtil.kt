package org.wikipedia.auth

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Build
import androidx.core.os.bundleOf
import com.google.gson.reflect.TypeToken
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.login.LoginResult
import org.wikipedia.util.log.L.d
import org.wikipedia.util.log.L.logRemoteErrorIfProd
import java.util.*

object AccountUtil {

    @JvmStatic
    fun updateAccount(response: AccountAuthenticatorResponse?, result: LoginResult) {
        if (createAccount(result.userName!!, result.password!!)) {
            response?.onResult(bundleOf(AccountManager.KEY_ACCOUNT_NAME to result.userName,
                    AccountManager.KEY_ACCOUNT_TYPE to accountType()))
        } else {
            response?.onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "")
            d("account creation failure")
            return
        }
        setPassword(result.password!!)
        putUserIdForLanguage(result.site.languageCode(), result.userId)
        groups = result.groups
    }

    @JvmStatic
    val isLoggedIn: Boolean
        get() = account() != null

    @JvmStatic
    val userName: String?
        get() {
            val account = account()
            return account?.name
        }

    @JvmStatic
    val password: String?
        get() {
            val account = account()
            return if (account == null) null else accountManager().getPassword(account)
        }

    @JvmStatic
    fun getUserIdForLanguage(code: String): Int {
        val map = userIds
        val id = map[code]
        return id ?: 0
    }

    @JvmStatic
    fun putUserIdForLanguage(code: String, id: Int) {
        val ids: MutableMap<String, Int> = HashMap()
        ids.putAll(userIds)
        ids[code] = id
        userIds = ids
    }

    @JvmStatic
    var groups: Set<String>
        get() {
            val account = account() ?: return emptySet()
            val setStr = accountManager().getUserData(account, WikipediaApp.instance.getString(R.string.preference_key_login_groups))
            return if (setStr.isNullOrEmpty()) emptySet() else GsonUnmarshaller.unmarshal(object : TypeToken<Set<String>>() {}, setStr)
        }
        set(groups) {
            val account = account() ?: return
            accountManager().setUserData(account,
                    WikipediaApp.instance.getString(R.string.preference_key_login_groups),
                    GsonMarshaller.marshal(groups))
        }

    @JvmStatic
    fun isMemberOf(groups: Set<String?>): Boolean {
        return groups.isNotEmpty() && !Collections.disjoint(groups, AccountUtil.groups)
    }

    @JvmStatic
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

    @JvmStatic
    fun supported(account: Account): Boolean {
        return account == account()
    }

    @JvmStatic
    fun account(): Account? {
        try {
            val accounts = accountManager().getAccountsByType(accountType())
            if (accounts.isNotEmpty()) {
                return accounts[0]
            }
        } catch (e: SecurityException) {
            logRemoteErrorIfProd(e)
        }
        return null
    }

    @JvmStatic
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

    private var userIds: Map<String, Int>
        get() {
            val account = account() ?: return emptyMap()
            val mapStr = accountManager().getUserData(account, WikipediaApp.instance.getString(R.string.preference_key_login_user_id_map))
            return if (mapStr.isNullOrEmpty()) emptyMap() else GsonUnmarshaller.unmarshal(object : TypeToken<Map<String, Int>>() {}, mapStr)
        }
        private set(ids) {
            val account = account() ?: return
            accountManager().setUserData(account,
                    WikipediaApp.instance.getString(R.string.preference_key_login_user_id_map),
                    GsonMarshaller.marshal(ids))
        }

    private fun accountManager(): AccountManager {
        return AccountManager.get(WikipediaApp.instance)
    }
}
