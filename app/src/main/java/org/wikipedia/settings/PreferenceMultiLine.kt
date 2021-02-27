package org.wikipedia.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceViewHolder
import org.wikipedia.R

class PreferenceMultiLine(context: Context) : Preference(context) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.findViewById<TextView>(android.R.id.title)?.let {
            it.isSingleLine = false
        }
        // Intercept the click listener for this preference, and if the preference has an intent,
        // launch the intent ourselves, so that we can catch the exception if the intent fails.
        // (but only do this if the preference doesn't already have a click listener)
        if (onPreferenceClickListener == null) {
            onPreferenceClickListener = OnPreferenceClickListener { preference: Preference ->
                preference.intent?.let {
                    try {
                        context.startActivity(it)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, context.getString(R.string.error_browser_not_found), Toast.LENGTH_LONG).show()
                    }
                    return@OnPreferenceClickListener true
                }
                false
            }
        }
    }
}
