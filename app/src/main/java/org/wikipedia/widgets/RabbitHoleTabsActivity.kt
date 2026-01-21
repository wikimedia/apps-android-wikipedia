package org.wikipedia.widgets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityRabbitHoleTabsBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class RabbitHoleTabsActivity : BaseActivity() {

    private lateinit var binding: ActivityRabbitHoleTabsBinding
    private var articleTitles: List<String> = emptyList()
    private var startTitle: String = ""
    private var endTitle: String = ""
    private val articleSummaries = mutableMapOf<String, PageSummary>()
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRabbitHoleTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dark background for stacked deck effect
        window.setBackgroundDrawableResource(android.R.color.black)
        setStatusBarColor(getColor(R.color.black))
        setNavigationBarColor(getColor(R.color.black))

        articleTitles = intent.getStringArrayListExtra(EXTRA_ARTICLE_TITLES) ?: emptyList()
        startTitle = intent.getStringExtra(EXTRA_START_TITLE) ?: articleTitles.firstOrNull() ?: ""
        endTitle = intent.getStringExtra(EXTRA_END_TITLE) ?: articleTitles.lastOrNull() ?: ""

        setupViews()
        loadArticleSummaries()
    }

    override fun onDestroy() {
        loadJob?.cancel()
        super.onDestroy()
    }

    private fun setupViews() {
        binding.rabbitHoleStartTitle.text = StringUtil.fromHtml(startTitle)
        binding.rabbitHoleEndTitle.text = StringUtil.fromHtml(endTitle)

        binding.rabbitHoleCloseButton.setOnClickListener {
            finish()
        }

        binding.rabbitHoleContainer.setOnClickListener {
            finish()
        }

        // Normal layout - first article at top
        val layoutManager = LinearLayoutManager(this)
        binding.rabbitHoleRecyclerView.layoutManager = layoutManager
        binding.rabbitHoleRecyclerView.clipChildren = false
        binding.rabbitHoleRecyclerView.clipToPadding = false

        binding.rabbitHoleRecyclerView.adapter = RabbitHoleTabAdapter()
    }

    private fun loadArticleSummaries() {
        val wikiSite = WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)

        loadJob = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }).launch {
            articleTitles.forEachIndexed { index, title ->
                try {
                    val summary = ServiceFactory.getRest(wikiSite).getPageSummary(title)
                    articleSummaries[title] = summary
                    withContext(Dispatchers.Main) {
                        binding.rabbitHoleRecyclerView.adapter?.notifyItemChanged(index)
                    }
                } catch (e: Exception) {
                    L.e(e)
                }
            }
        }
    }

    private fun openArticle(position: Int) {
        if (position < 0 || position >= articleTitles.size) return

        val title = articleTitles[position]
        val wikiSite = WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)
        val pageTitle = PageTitle(title, wikiSite)
        val historyEntry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_WIDGET)

        val intent = PageActivity.newIntentForNewTab(this, historyEntry, pageTitle)
            .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.WIDGET)
            .putStringArrayListExtra(Constants.INTENT_EXTRA_RABBIT_HOLE_ARTICLE_TITLES, ArrayList(articleTitles))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)
        finish()
    }

    private inner class RabbitHoleTabAdapter : RecyclerView.Adapter<RabbitHoleTabViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RabbitHoleTabViewHolder {
            val view = layoutInflater.inflate(R.layout.item_rabbit_hole_tab, parent, false)
            return RabbitHoleTabViewHolder(view)
        }

        override fun getItemCount(): Int = articleTitles.size

        override fun onBindViewHolder(holder: RabbitHoleTabViewHolder, position: Int) {
            holder.bind(articleTitles[position], position)
        }
    }

    private inner class RabbitHoleTabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootView = itemView.findViewById<FrameLayout>(R.id.rabbit_hole_tab_root)
        private val cardView = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.rabbit_hole_tab_card)
        private val titleView = itemView.findViewById<TextView>(R.id.rabbit_hole_tab_title)
        private val descriptionView = itemView.findViewById<TextView>(R.id.rabbit_hole_tab_description)
        private val dividerView = itemView.findViewById<View>(R.id.rabbit_hole_tab_divider)
        private val summaryView = itemView.findViewById<TextView>(R.id.rabbit_hole_tab_summary)
        private val overlayView = itemView.findViewById<View>(R.id.rabbit_hole_tab_overlay)

        fun bind(title: String, position: Int) {
            titleView.text = StringUtil.fromHtml(title)
            descriptionView.text = ""
            summaryView.text = ""

            val summary = articleSummaries[title]
            if (summary != null) {
                titleView.text = StringUtil.fromHtml(summary.displayTitle)

                // Show description if available
                if (!summary.description.isNullOrEmpty()) {
                    descriptionView.text = summary.description
                }

                // Show article extract/summary
                if (!summary.extractHtml.isNullOrEmpty()) {
                    summaryView.text = StringUtil.fromHtml(summary.extractHtml)
                } else if (!summary.extract.isNullOrEmpty()) {
                    summaryView.text = summary.extract
                }
            }

            // Reset visibility and styling
            cardView.isVisible = true
            rootView.isVisible = true

            // Reset transforms
            cardView.scaleX = 1f
            cardView.scaleY = 1f
            overlayView.alpha = 0f
            cardView.elevation = 8f

            // Show all content
            titleView.maxLines = 3
            descriptionView.isVisible = !descriptionView.text.isNullOrEmpty()
            dividerView.isVisible = true
            summaryView.isVisible = true

            // Simple bottom margin for spacing between cards
            val spacing = resources.getDimensionPixelSize(R.dimen.rabbit_hole_card_spacing)
            (rootView.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                it.topMargin = 0
                it.bottomMargin = spacing
            }

            cardView.setOnClickListener {
                openArticle(position)
            }
        }
    }

    companion object {
        private const val EXTRA_ARTICLE_TITLES = "articleTitles"
        private const val EXTRA_START_TITLE = "startTitle"
        private const val EXTRA_END_TITLE = "endTitle"
        private const val IMAGE_SIZE = 256

        fun newIntent(context: Context, articleTitles: List<String>, startTitle: String?, endTitle: String?): Intent {
            return Intent(context, RabbitHoleTabsActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_ARTICLE_TITLES, ArrayList(articleTitles))
                putExtra(EXTRA_START_TITLE, startTitle ?: articleTitles.firstOrNull() ?: "")
                putExtra(EXTRA_END_TITLE, endTitle ?: articleTitles.lastOrNull() ?: "")
            }
        }
    }
}
