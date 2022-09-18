package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent

open class SwitchPreferenceMultiLine : SwitchPreferenceCompat {
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.findViewById<TextView>(R.id.title)?.isSingleLine = false
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        val ret = super.callChangeListener(newValue)
        BreadCrumbLogEvent.logSettingsSelection(context, key, newValue)
        return ret
    }
}
