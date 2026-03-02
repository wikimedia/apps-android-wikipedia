package org.wikipedia.lesson08.homeworks

import android.view.View
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import org.wikipedia.R

class TopReadItem(matcher: Matcher<View>) : KRecyclerItem<TopReadItem>(matcher) {

    val menuList = KImageView(matcher) {
        withId(R.id.view_list_card_header_menu)
    }

    val headerTitle = KTextView(matcher) {
        withId(R.id.view_card_header_title)
    }

    val more = KTextView(matcher) {
        withId(R.id.footerActionButton)
    }


    val items = KRecyclerView(
        matcher,
        builder = {
            withId(R.id.view_list_card_list)
        },
        itemTypeBuilder = {
            itemType(::TopReadItems)

        }
    )
}