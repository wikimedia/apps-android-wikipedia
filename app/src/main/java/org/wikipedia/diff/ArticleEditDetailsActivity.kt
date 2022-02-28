package org.wikipedia.diff

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class ArticleEditDetailsActivity : SingleFragmentActivity<ArticleEditDetailsFragment>() {

    override fun createFragment(): ArticleEditDetailsFragment {
        return ArticleEditDetailsFragment.newInstance(intent.getStringExtra(EXTRA_ARTICLE_TITLE)!!,
                intent.getLongExtra(EXTRA_EDIT_REVISION_FROM, 0),
                intent.getLongExtra(EXTRA_EDIT_REVISION_TO, 0),
                intent.getStringExtra(EXTRA_EDIT_LANGUAGE_CODE)!!)
    }

    companion object {
        const val EXTRA_ARTICLE_TITLE = "articleTitle"
        const val EXTRA_EDIT_REVISION_FROM = "revisionFrom"
        const val EXTRA_EDIT_REVISION_TO = "revisionTo"
        const val EXTRA_EDIT_LANGUAGE_CODE = "languageCode"

        fun newIntent(context: Context, articleTitle: String, revisionTo: Long, languageCode: String): Intent {
            return newIntent(context, articleTitle, 0, revisionTo, languageCode)
        }

        fun newIntent(context: Context, articleTitle: String, revisionFrom: Long,
                      revisionTo: Long, languageCode: String): Intent {
            return Intent(context, ArticleEditDetailsActivity::class.java)
                    .putExtra(EXTRA_ARTICLE_TITLE, articleTitle)
                    .putExtra(EXTRA_EDIT_REVISION_FROM, revisionFrom)
                    .putExtra(EXTRA_EDIT_REVISION_TO, revisionTo)
                    .putExtra(EXTRA_EDIT_LANGUAGE_CODE, languageCode)
        }
    }
}
