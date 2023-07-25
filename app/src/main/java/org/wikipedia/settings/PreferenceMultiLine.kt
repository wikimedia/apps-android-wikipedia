package org.wikipedia.settings

import android.annotation.SuppressLint
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
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.util.DimenUtil

@Suppress("unused")
class PreferenceMultiLine : Preference {
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val titleView = holder.itemView.findViewById<TextView>(android.R.id.title)
        titleView?.isSingleLine = false
        // TODO: move this over to styles.xml when we figure out which styles to override.
        titleView?.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
        titleView?.setPadding(0, 0, 0, DimenUtil.roundedDpToPx(4f))

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

    @SuppressLint("RestrictedApi")
    override fun performClick() {
        BreadCrumbLogEvent.logSettingsSelection(context, if (!key.isNullOrEmpty()) key else title.toString())
        super.performClick()
    }
}
