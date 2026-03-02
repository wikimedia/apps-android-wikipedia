package org.wikipedia.lesson08.homeworks

import android.view.View
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import org.wikipedia.R

class TopReadItems(matcher: Matcher<View>) : KRecyclerItem<TopReadItems>(matcher) {

    val numberView = KTextView(matcher) {
        withId(R.id.numberView)
    }

    val title = KTextView(matcher) {
        withId(R.id.view_list_card_item_title)
    }

    val subtitle = KTextView(matcher) {
        withId(R.id.view_list_card_item_subtitle)
    }

    val cardItemViews = KTextView(matcher) {
        withId(R.id.view_list_card_item_pageviews)
    }

    val imageCard = KTextView(matcher) {
        withId(R.id.view_list_card_item_image)
    }
}