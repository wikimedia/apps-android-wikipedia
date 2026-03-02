package org.wikipedia.lesson08.homeworks

import android.view.View
import com.google.android.material.textview.MaterialTextView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher

class DateItem(matcher: Matcher<View>) : KRecyclerItem<DateItem>(matcher) {

    val dateText = KTextView(matcher) {
        isInstanceOf(MaterialTextView::class.java)
    }
}