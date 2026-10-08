package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.updatePadding
import androidx.preference.PreferenceViewHolder
import org.wikipedia.util.DimenUtil

class SwitchPreferenceBetaPill @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = androidx.preference.R.attr.switchPreferenceStyle
) : SwitchPreferenceMultiLine(ctx, attrs, defStyle) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(android.R.id.title)?.parent as? View)
            ?.updatePadding(top = DimenUtil.roundedDpToPx(TOP_PADDING_DP))
    }

    companion object {
        private const val TOP_PADDING_DP = 4f
    }
}
