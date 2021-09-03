package org.wikipedia.feed.news

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.databinding.ViewCardNewsBinding
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ImageZoomHelper

class NewsCardView(context: Context) : DefaultFeedCardView<NewsCard>(context) {

    interface Callback {
        fun onNewsItemSelected(card: NewsCard, view: NewsItemView)
    }

    private val binding = ViewCardNewsBinding.inflate(LayoutInflater.from(context), this, true)
    private var prevImageDownloadSettings = Prefs.isImageDownloadEnabled()
    private var isSnapHelperAttached = false

    private fun setUpIndicatorDots(card: NewsCard) {
        val indicatorRadius = 4
        val indicatorPadding = 8
        val indicatorHeight = 20
        binding.newsRecyclerView.addItemDecoration(
            RecyclerViewIndicatorDotDecor(
                DimenUtil.roundedDpToPx(indicatorRadius.toFloat()).toFloat(),
                DimenUtil.roundedDpToPx(indicatorPadding.toFloat()),
                DimenUtil.roundedDpToPx(indicatorHeight.toFloat()),
                ResourceUtil.getThemedColor(context, R.attr.chart_shade5),
                ResourceUtil.getThemedColor(context, R.attr.colorAccent),
                L10nUtil.isLangRTL(card.wikiSite().languageCode)
            )
        )
    }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.headerView.setCallback(value)
        }

    override var card: NewsCard? = null
        set(value) {
            if (field != value || prevImageDownloadSettings != Prefs.isImageDownloadEnabled()) {
                field = value
                prevImageDownloadSettings = Prefs.isImageDownloadEnabled()
                value?.let {
                    header(it)
                    setLayoutDirectionByWikiSite(it.wikiSite(), binding.rtlContainer)
                    setUpRecycler(it)
                }
            }
        }

    private fun setUpRecycler(card: NewsCard) {
        binding.newsRecyclerView.setHasFixedSize(true)
        binding.newsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.newsRecyclerView.isNestedScrollingEnabled = false
        binding.newsRecyclerView.clipToPadding = false
        binding.newsRecyclerView.adapter = NewsAdapter(card)
        setUpIndicatorDots(card)
        setUpSnapHelper()
    }

    private fun setUpSnapHelper() {
        if (!isSnapHelperAttached) {
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.newsRecyclerView)
            isSnapHelperAttached = true
        }
    }

    private fun header(card: NewsCard) {
        binding.headerView.setTitle(card.title())
            .setLangCode(card.wikiSite().languageCode)
            .setCard(card)
    }

    private class NewsItemHolder constructor(private val newsItemView: NewsItemView) : RecyclerView.ViewHolder(newsItemView) {
        fun bindItem(newsItem: NewsItem) {
            newsItemView.setContents(newsItem)
        }

        val view get() = newsItemView
    }

    private inner class NewsAdapter constructor(private val card: NewsCard) : RecyclerView.Adapter<NewsItemHolder>() {

        override fun getItemCount(): Int {
            return card.news().size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsItemHolder {
            return NewsItemHolder(NewsItemView(context))
        }

        override fun onBindViewHolder(holder: NewsItemHolder, position: Int) {
            holder.bindItem(card.news()[position])
            holder.view.setOnClickListener {
                callback?.onNewsItemSelected(card, holder.view)
            }
            holder.view.setOnLongClickListener {
                if (ImageZoomHelper.isZooming) {
                    ImageZoomHelper.dispatchCancelEvent(holder.view)
                }
                true
            }
        }

        override fun onViewAttachedToWindow(holder: NewsItemHolder) {
            super.onViewAttachedToWindow(holder)
            holder.view.callback = callback
        }

        override fun onViewDetachedFromWindow(holder: NewsItemHolder) {
            holder.view.callback = null
            super.onViewDetachedFromWindow(holder)
        }
    }
}
