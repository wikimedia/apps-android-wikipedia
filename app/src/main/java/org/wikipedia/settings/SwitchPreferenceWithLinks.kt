package org.wikipedia.settings

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.PreferenceViewHolder
import org.wikipedia.R
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil

class SwitchPreferenceWithLinks : SwitchPreferenceMultiLine {

    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val view = holder.findViewById(android.R.id.summary) as TextView

        view.movementMethod = LinkMovementMethodExt { url ->
            UriUtil.handleExternalLink(context, Uri.parse(url))
        }

        view.setOnClickListener {
            isChecked = !isChecked
            callChangeListener(isChecked)
        }

        view.background = AppCompatResources.getDrawable(context,
            ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackgroundBorderless))
    }
}
