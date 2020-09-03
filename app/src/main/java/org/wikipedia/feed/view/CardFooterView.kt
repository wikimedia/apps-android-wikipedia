package org.wikipedia.feed.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.ButterKnife
import kotlinx.android.synthetic.main.view_card_footer.view.*
import org.wikipedia.R

internal class CardFooterView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    var callback: Callback? = null

    interface Callback {
        fun onFooterClicked()
    }

    init {
        View.inflate(context, R.layout.view_card_footer, this)
        ButterKnife.bind(this)
        footerActionButton.setOnClickListener {
            callback?.onFooterClicked()
        }
    }

    fun setFooterActionText(actionText: String) {
        footerActionButton.text = actionText
    }
}
