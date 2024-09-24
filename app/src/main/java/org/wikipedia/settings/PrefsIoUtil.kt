package org.wikipedia.settings

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import org.wikipedia.WikipediaApp
import java.util.Collections

object PrefsIoUtil {

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

    fun getString(key: String?, defaultValue: String?): String? {
        return preferences.getString(key, defaultValue)
    }

    fun setString(key: String?, value: String?) {
        edit().putString(key, value).apply()
    }

    fun getStringSet(key: String?, defaultValue: Set<String?>?): Set<String>? {
        val set = preferences.getStringSet(key, defaultValue)
        return if (set == null) null else Collections.unmodifiableSet(set)
    }

    fun setStringSet(key: String?, value: Set<String?>?) {
        edit().putStringSet(key, value).apply()
    }

    fun getLong(key: String?, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    fun setLong(key: String?, value: Long) {
        edit().putLong(key, value).apply()
    }

    fun getInt(key: String?, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    fun setInt(key: String?, value: Int) {
        edit().putInt(key, value).apply()
    }

    fun getBoolean(key: String?, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    fun setBoolean(key: String?, value: Boolean) {
        edit().putBoolean(key, value).apply()
    }

    fun remove(@StringRes id: Int) {
        remove(getKey(id))
    }

    fun remove(key: String?) {
        edit().remove(key).apply()
    }

    fun contains(@StringRes id: Int): Boolean {
        return preferences.contains(getKey(id))
    }

    fun contains(key: String?): Boolean {
        return preferences.contains(key)
    }

    /** @return Key String resource from preference_keys.xml.
     */
    fun getKey(@StringRes id: Int, vararg formatArgs: Any?): String {
        return WikipediaApp.instance.resources.getString(id, *formatArgs)
    }

    private fun edit(): SharedPreferences.Editor {
        return preferences.edit()
    }

    private val preferences get() = PreferenceManager.getDefaultSharedPreferences(WikipediaApp.instance)
}
