package org.wikipedia.diff

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.page.PageTitle

class ArticleEditDetailsActivity : SingleFragmentActivity<ArticleEditDetailsFragment>() {

    override fun createFragment(): ArticleEditDetailsFragment {
        return ArticleEditDetailsFragment.newInstance(intent.getParcelableExtra(EXTRA_ARTICLE_TITLE)!!,
                intent.getLongExtra(EXTRA_EDIT_REVISION_FROM, -1),
                intent.getLongExtra(EXTRA_EDIT_REVISION_TO, -1))
    }

    companion object {
        const val EXTRA_ARTICLE_TITLE = "articleTitle"
        const val EXTRA_EDIT_REVISION_FROM = "revisionFrom"
        const val EXTRA_EDIT_REVISION_TO = "revisionTo"

        fun newIntent(context: Context, title: PageTitle, revisionTo: Long): Intent {
            return newIntent(context, title, -1, revisionTo)
        }

        fun newIntent(context: Context, title: PageTitle, revisionFrom: Long, revisionTo: Long): Intent {
            return Intent(context, ArticleEditDetailsActivity::class.java)
                .putExtras(bundleOf(EXTRA_ARTICLE_TITLE to title, EXTRA_EDIT_REVISION_FROM to revisionFrom,
                    EXTRA_EDIT_REVISION_TO to revisionTo))
        }
    }
}
