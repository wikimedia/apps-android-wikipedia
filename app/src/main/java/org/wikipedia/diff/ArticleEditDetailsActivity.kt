package org.wikipedia.diff

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityArticleEditDetailsBinding
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle

class ArticleEditDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityArticleEditDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleEditDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragment = ArticleEditDetailsFragment.newInstance(intent.parcelableExtra(EXTRA_ARTICLE_TITLE)!!,
            intent.getIntExtra(EXTRA_PAGE_ID, -1),
            intent.getLongExtra(EXTRA_EDIT_REVISION_FROM, -1),
            intent.getLongExtra(EXTRA_EDIT_REVISION_TO, -1),
            intent.getSerializableExtra(EXTRA_SOURCE) as InvokeSource)

        if (savedInstanceState == null) {
            supportFragmentManager.commit { add(binding.fragmentContainer.id, fragment) }
        }
    }

    companion object {
        const val EXTRA_ARTICLE_TITLE = "articleTitle"
        const val EXTRA_PAGE_ID = "pageId"
        const val EXTRA_EDIT_REVISION_FROM = "revisionFrom"
        const val EXTRA_EDIT_REVISION_TO = "revisionTo"
        const val EXTRA_SOURCE = "invokeSource"

        fun newIntent(context: Context, title: PageTitle, revisionTo: Long): Intent {
            return newIntent(context, title, -1, -1, revisionTo)
        }

        fun newIntent(context: Context, title: PageTitle, pageId: Int, revisionFrom: Long = -1, revisionTo: Long, source: InvokeSource = InvokeSource.DIFF_ACTIVITY): Intent {
            return Intent(context, ArticleEditDetailsActivity::class.java)
                .putExtra(EXTRA_ARTICLE_TITLE, title)
                .putExtra(EXTRA_PAGE_ID, pageId)
                .putExtra(EXTRA_EDIT_REVISION_FROM, revisionFrom)
                .putExtra(EXTRA_EDIT_REVISION_TO, revisionTo)
                .putExtra(EXTRA_SOURCE, source)
        }
    }
}
