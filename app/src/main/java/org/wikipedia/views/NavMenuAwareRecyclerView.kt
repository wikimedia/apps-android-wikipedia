package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.views.FrameLayoutNavMenuTriggerer.Companion.setChildViewScrolled

class NavMenuAwareRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        RecyclerView(context, attrs, defStyleAttr) {

    override fun onScrolled(dx: Int, dy: Int) {
        setChildViewScrolled()
    }
}
