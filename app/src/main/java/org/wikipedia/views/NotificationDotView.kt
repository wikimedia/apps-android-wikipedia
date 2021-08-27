package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewNotificationDotBinding
import kotlin.math.min

class NotificationDotView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewNotificationDotBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setUnreadCount(count: Int) {
        binding.unreadCountText.text = min(count, 99).toString()
    }

    fun runAnimation() {
        startAnimation(AnimationUtils.loadAnimation(context, R.anim.tab_list_zoom_enter))
    }
}
