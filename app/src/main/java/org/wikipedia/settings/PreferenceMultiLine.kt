package org.wikipedia.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceViewHolder
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil

@Suppress("unused")
class PreferenceMultiLine : Preference {
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    var titleColor: Int = ResourceUtil.getThemedColor(context, android.R.attr.textColorPrimary)
    var titleAllCaps: Boolean = false
    var titleTypeFace: Typeface = Typeface.DEFAULT

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val title = holder.itemView.findViewById<TextView>(android.R.id.title)
        title.isSingleLine = false
        title.isAllCaps = titleAllCaps
        title.typeface = titleTypeFace
        title.setTextColor(titleColor)

        // Intercept the click listener for this preference, and if the preference has an intent,
        // launch the intent ourselves, so that we can catch the exception if the intent fails.
        // (but only do this if the preference doesn't already have a click listener)
        if (onPreferenceClickListener == null) {
            onPreferenceClickListener = OnPreferenceClickListener { preference ->
                if (preference.intent != null) {
                    try {
                        context.startActivity(preference.intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, context.getString(R.string.error_browser_not_found),
                                Toast.LENGTH_LONG).show()
                    }
                    return@OnPreferenceClickListener true
                }
                false
            }
        }
    }
}
