package org.wikipedia.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.updatePadding
import androidx.preference.PreferenceViewHolder
import org.wikipedia.util.DimenUtil

class SwitchPreferenceBetaPill : SwitchPreferenceMultiLine {
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(android.R.id.title)?.parent as? View)
            ?.updatePadding(top = DimenUtil.roundedDpToPx(TOP_PADDING_DP))
    }

    companion object {
        private const val TOP_PADDING_DP = 4f
    }
}
