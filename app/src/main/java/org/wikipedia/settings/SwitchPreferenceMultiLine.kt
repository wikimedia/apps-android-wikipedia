package org.wikipedia.settings

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.metricsplatform.BreadcrumbLogEvent
import org.wikipedia.util.DimenUtil

open class SwitchPreferenceMultiLine : SwitchPreferenceCompat {
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    init {
        widgetLayoutResource = R.layout.preference_material_switch
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val titleView = holder.itemView.findViewById<TextView>(android.R.id.title)
        titleView?.isSingleLine = false
        // TODO: move this over to styles.xml when we figure out which styles to override.
        titleView?.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
        titleView?.setPadding(0, 0, 0, DimenUtil.roundedDpToPx(4f))
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        val ret = super.callChangeListener(newValue)
        BreadCrumbLogEvent.logSettingsSelection(context, key, newValue)
        BreadcrumbLogEvent().logSettingsSelection(context, key, newValue)
        return ret
    }
}
