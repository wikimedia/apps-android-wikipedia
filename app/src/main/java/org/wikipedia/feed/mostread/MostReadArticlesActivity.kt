package org.wikipedia.feed.mostread

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.feed.mostread.MostReadFragment.Companion.newInstance
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller

class MostReadArticlesActivity : SingleFragmentActivity<MostReadFragment>() {

    public override fun createFragment(): MostReadFragment {
        return newInstance(GsonUnmarshaller.unmarshal(MostReadItemCard::class.java,
            intent.getStringExtra(MOST_READ_CARD)))
    }

    companion object {
        const val MOST_READ_CARD = "item"
        @JvmStatic
        fun newIntent(context: Context, card: MostReadListCard): Intent {
            return Intent(context, MostReadArticlesActivity::class.java)
                .putExtra(MOST_READ_CARD, GsonMarshaller.marshal(card))
        }
    }
}
