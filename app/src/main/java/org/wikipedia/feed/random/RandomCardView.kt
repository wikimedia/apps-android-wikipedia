package org.wikipedia.feed.random

import android.content.Context
import org.wikipedia.feed.featured.FeaturedArticleCardView
import org.wikipedia.feed.view.CardFooterView

class RandomCardView(context: Context) : FeaturedArticleCardView(context) {
    interface Callback {
        fun onRandomClick(view: RandomCardView)
    }

    override val footerCallback: CardFooterView.Callback
        get() = CardFooterView.Callback {
            card?.let {
                callback?.onRandomClick(this@RandomCardView)
            }
        }
}
