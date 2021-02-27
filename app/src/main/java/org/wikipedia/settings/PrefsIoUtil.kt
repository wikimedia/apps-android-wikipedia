package org.wikipedia.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import org.wikipedia.WikipediaApp

/** Shared preferences input / output utility providing set* functionality that writes to SP on the
 * client's behalf, IO without client supplied [Context], and wrappers for using string
 * resources as keys, and unifies SP access.  */
object PrefsIoUtil {

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(WikipediaApp.getInstance())
    private val resources: Resources = WikipediaApp.getInstance().resources

    fun getString(@StringRes id: Int, defaultValue: String?): String? {
        return getString(getKey(id), defaultValue)
    }

    fun setString(@StringRes id: Int, value: String?) {
        setString(getKey(id), value)
    }

    fun getLong(@StringRes id: Int, defaultValue: Long): Long {
        return getLong(getKey(id), defaultValue)
    }

    fun setLong(@StringRes id: Int, value: Long) {
        setLong(getKey(id), value)
    }

    fun getInt(@StringRes id: Int, defaultValue: Int): Int {
        return getInt(getKey(id), defaultValue)
    }

    fun setInt(@StringRes id: Int, value: Int) {
        setInt(getKey(id), value)
    }

    fun getBoolean(@StringRes id: Int, defaultValue: Boolean): Boolean {
        return getBoolean(getKey(id), defaultValue)
    }

    fun setBoolean(@StringRes id: Int, value: Boolean) {
        setBoolean(getKey(id), value)
    }

    @JvmStatic
    fun getString(key: String?, defaultValue: String?): String? {
        return preferences.getString(key, defaultValue)
    }

    @JvmStatic
    fun setString(key: String?, value: String?) {
        edit().putString(key, value).apply()
    }

    @JvmStatic
    fun getLong(key: String?, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    @JvmStatic
    fun setLong(key: String?, value: Long) {
        edit().putLong(key, value).apply()
    }

    @JvmStatic
    fun getInt(key: String?, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    @JvmStatic
    fun setInt(key: String?, value: Int) {
        edit().putInt(key, value).apply()
    }

    @JvmStatic
    fun getBoolean(key: String?, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    @JvmStatic
    fun setBoolean(key: String?, value: Boolean) {
        edit().putBoolean(key, value).apply()
    }

    fun remove(@StringRes id: Int) {
        remove(getKey(id))
    }

    @JvmStatic
    fun remove(key: String?) {
        edit().remove(key).apply()
    }

    /** @return Key String resource from preference_keys.xml.
     */
    @JvmStatic
    fun getKey(@StringRes id: Int, vararg formatArgs: Any?): String {
        return resources.getString(id, *formatArgs)
    }

    operator fun contains(@StringRes id: Int): Boolean {
        return preferences.contains(getKey(id))
    }

    @JvmStatic
    operator fun contains(key: String?): Boolean {
        return preferences.contains(key)
    }

    private fun edit(): SharedPreferences.Editor {
        return preferences.edit()
    }
}
