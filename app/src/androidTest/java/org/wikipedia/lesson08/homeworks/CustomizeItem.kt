package org.wikipedia.lesson08.homeworks

import android.view.View
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import org.wikipedia.R

class CustomizeItem(matcher: Matcher<View>) : KRecyclerItem<CustomizeItem>(matcher) {

    val imageHeader = KView(matcher) {
        withId(R.id.view_announcement_header_image)
    }
    val announcementText = KTextView(matcher) {
        withId(R.id.view_announcement_text)
    }
    val positiveButton = KButton(matcher) {
        withId(R.id.view_announcement_action_positive)
    }
    val negativeButton = KButton(matcher){
        withId(R.id.view_announcement_action_negative)
    }
}