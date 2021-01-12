package org.wikipedia.diff

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants.*
import org.wikipedia.activity.SingleFragmentActivity

class ArticleEditDetailsActivity : SingleFragmentActivity<ArticleEditDetailsFragment>() {

    override fun createFragment(): ArticleEditDetailsFragment {
        return ArticleEditDetailsFragment.newInstance(intent.getStringExtra(INTENT_EXTRA_ARTICLE_TITLE)!!,
                intent.getLongExtra(INTENT_EXTRA_EDIT_REVISION_ID, 0),
                intent.getStringExtra(INTENT_EXTRA_EDIT_LANGUAGE_CODE)!!,
                intent.getIntExtra(INTENT_EXTRA_EDIT_SIZE, 0))
    }

    companion object {
        fun newIntent(context: Context, articleTitle: String, revisionId: Long, languageCode: String, diffSize: Int): Intent {
            return Intent(context, ArticleEditDetailsActivity::class.java)
                    .putExtra(INTENT_EXTRA_ARTICLE_TITLE, articleTitle)
                    .putExtra(INTENT_EXTRA_EDIT_REVISION_ID, revisionId)
                    .putExtra(INTENT_EXTRA_EDIT_LANGUAGE_CODE, languageCode)
                    .putExtra(INTENT_EXTRA_EDIT_SIZE, diffSize)
        }
    }
}
