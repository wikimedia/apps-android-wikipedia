package org.wikipedia.diff

import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import org.wikipedia.activity.SingleFragmentActivity

class ArticleEditDetailsActivity : SingleFragmentActivity<ArticleEditDetailsFragment>() {

    override fun createFragment(): ArticleEditDetailsFragment {
        return ArticleEditDetailsFragment.newInstance()
    }

    companion object {
        const val EXTRA_SOURCE_ARTICLE_TITLE = "articleTitle"
        const val EXTRA_SOURCE_EDIT_REVISION_ID = "revisionId"
        const val EXTRA_SOURCE_EDIT_LANGUAGE_CODE = "languageCode"

        fun newIntent(@NonNull context: Context, @NonNull articleTitle: String, @NonNull revisionId: Long, @NonNull languageCode: String): Intent {
            return Intent(context, ArticleEditDetailsActivity::class.java)
                    .putExtra(EXTRA_SOURCE_ARTICLE_TITLE, articleTitle)
                    .putExtra(EXTRA_SOURCE_EDIT_REVISION_ID, revisionId)
                    .putExtra(EXTRA_SOURCE_EDIT_LANGUAGE_CODE, languageCode)
        }
    }
}
