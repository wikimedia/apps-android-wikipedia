package org.wikipedia.diff

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle

class ArticleEditDetailsActivity : SingleFragmentActivity<ArticleEditDetailsFragment>() {

    override fun createFragment(): ArticleEditDetailsFragment {
        return ArticleEditDetailsFragment.newInstance(intent.parcelableExtra(EXTRA_ARTICLE_TITLE)!!,
                intent.getIntExtra(EXTRA_PAGE_ID, -1),
                intent.getLongExtra(EXTRA_EDIT_REVISION_FROM, -1),
                intent.getLongExtra(EXTRA_EDIT_REVISION_TO, -1))
    }

    companion object {
        const val EXTRA_ARTICLE_TITLE = "articleTitle"
        const val EXTRA_PAGE_ID = "pageId"
        const val EXTRA_EDIT_REVISION_FROM = "revisionFrom"
        const val EXTRA_EDIT_REVISION_TO = "revisionTo"

        fun newIntent(context: Context, title: PageTitle, revisionTo: Long): Intent {
            return newIntent(context, title, -1, -1, revisionTo)
        }

        fun newIntent(context: Context, title: PageTitle, pageId: Int, revisionTo: Long): Intent {
            return newIntent(context, title, pageId, -1, revisionTo)
        }

        fun newIntent(context: Context, title: PageTitle, pageId: Int, revisionFrom: Long = -1, revisionTo: Long): Intent {
            return Intent(context, ArticleEditDetailsActivity::class.java)
                    .putExtra(EXTRA_ARTICLE_TITLE, title)
                    .putExtra(EXTRA_PAGE_ID, pageId)
                    .putExtra(EXTRA_EDIT_REVISION_FROM, revisionFrom)
                    .putExtra(EXTRA_EDIT_REVISION_TO, revisionTo)
        }
    }
}
