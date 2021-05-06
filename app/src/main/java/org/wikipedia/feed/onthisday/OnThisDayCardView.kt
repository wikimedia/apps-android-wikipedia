package org.wikipedia.feed.onthisday

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.FeedFunnel
import org.wikipedia.databinding.ItemOnThisDayPagesBinding
import org.wikipedia.databinding.ViewCardOnThisDayBinding
import org.wikipedia.databinding.ViewOnThisDayEventBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.readinglist.AddToReadingListDialog.Companion.newInstance
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.MoveToReadingListDialog.Companion.newInstance
import org.wikipedia.readinglist.ReadingListBehaviorsUtil.AddToDefaultListCallback
import org.wikipedia.readinglist.ReadingListBehaviorsUtil.addToDefaultList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.DateUtil.getYearDifferenceString
import org.wikipedia.util.DateUtil.yearToStringWithEra
import org.wikipedia.util.StringUtil.fromHtml
import org.wikipedia.util.TransitionUtil.getSharedElements

class OnThisDayCardView(context: Context) : DefaultFeedCardView<OnThisDayCard?>(context), CardFooterView.Callback {

    private val binding = ViewCardOnThisDayBinding.inflate(LayoutInflater.from(context), this, true)
    private val otdEventViewBinding = ViewOnThisDayEventBinding.inflate(LayoutInflater.from(context), this, true)
    private val otdItemViewBinding = ItemOnThisDayPagesBinding.inflate(LayoutInflater.from(context), this, true)

    private val funnel = FeedFunnel(WikipediaApp.getInstance())
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var age = 0

    init {
        binding.clickContainer.setOnClickListener { view -> onCardClicked(view) }
        otdEventViewBinding.year.setOnClickListener { view -> onCardClicked(view) }
    }

    override fun onFooterClicked() {
        funnel.cardClicked(CardType.ON_THIS_DAY, card!!.wikiSite().languageCode())
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation((context as Activity),
                binding.cardHeader.titleView, context.getString(R.string.transition_on_this_day))
        context.startActivity(OnThisDayActivity.newIntent(context, age, -1, card!!.wikiSite(), InvokeSource.ON_THIS_DAY_CARD_FOOTER), options.toBundle())
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        binding.cardHeader.setCallback(callback)
    }

    private fun header(card: OnThisDayCard) {
        binding.cardHeader.setTitle(card.title())
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(callback)
        otdEventViewBinding.text.text = card.text()
        otdEventViewBinding.year.text = yearToStringWithEra(card.year())
    }

    private fun footer(card: OnThisDayCard) {
        otdEventViewBinding.pagesIndicator.visibility = GONE
        binding.cardFooterView.setFooterActionText(card.footerActionText(), card.wikiSite().languageCode())
        binding.cardFooterView.callback = this
    }

    override fun setCard(card: OnThisDayCard) {
        super.setCard(card)
        age = card.age
        setLayoutDirectionByWikiSite(card.wikiSite(), binding.viewRtlContainer)
        otdEventViewBinding.yearsText.text = getYearDifferenceString(card.year())
        updateOtdEventUI(card)
        header(card)
        footer(card)
    }

    private fun onCardClicked(view: View) {
        val isYearClicked = view.id == R.id.year
        funnel.cardClicked(CardType.ON_THIS_DAY, card!!.wikiSite().languageCode())
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation((context as Activity),
                binding.cardHeader.titleView, context.getString(R.string.transition_on_this_day))
        context.startActivity(OnThisDayActivity.newIntent(context, age, if (isYearClicked) card!!.year() else -1,
                card!!.wikiSite(), if (isYearClicked) InvokeSource.ON_THIS_DAY_CARD_YEAR else InvokeSource.ON_THIS_DAY_CARD_BODY), options.toBundle())
    }

    private fun updateOtdEventUI(card: OnThisDayCard) {
        otdEventViewBinding.pagesPager.visibility = GONE
        var chosenPage: PageSummary? = null
        if (card.pages() != null) {
            otdEventViewBinding.page.root.visibility = VISIBLE
            for (pageSummary in card.pages()!!) {
                chosenPage = pageSummary
                if (pageSummary.thumbnailUrl != null) {
                    break
                }
            }
            val finalChosenPage = chosenPage
            if (chosenPage != null) {
                if (chosenPage.thumbnailUrl == null) {
                    otdItemViewBinding.image.visibility = GONE
                } else {
                    otdItemViewBinding.image.visibility = VISIBLE
                    otdItemViewBinding.image.loadImage(Uri.parse(chosenPage.thumbnailUrl))
                }
                otdItemViewBinding.description.text = chosenPage.description
                otdItemViewBinding.description.visibility = if (TextUtils.isEmpty(chosenPage.description)) GONE else VISIBLE
                otdItemViewBinding.title.maxLines = if (TextUtils.isEmpty(chosenPage.description)) 2 else 1
                otdItemViewBinding.title.text = fromHtml(chosenPage.displayTitle)
                otdEventViewBinding.page.root.setOnClickListener {
                    if (callback != null) {
                        callback!!.onSelectPage(card, HistoryEntry(finalChosenPage!!.getPageTitle(card.wikiSite()),
                                HistoryEntry.SOURCE_ON_THIS_DAY_CARD), getSharedElements(context, otdItemViewBinding.image))
                    }
                }
                otdEventViewBinding.page.root.setOnLongClickListener { view ->
                    val pageTitle = finalChosenPage!!.getPageTitle(card.wikiSite())
                    val entry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_CARD)
                    LongPressMenu(view!!, true, object : LongPressMenu.Callback {
                        override fun onOpenLink(entry: HistoryEntry) {
                            if (callback != null) {
                                callback!!.onSelectPage(card, entry, getSharedElements(context, otdItemViewBinding.image))
                            }
                        }

                        override fun onOpenInNewTab(entry: HistoryEntry) {
                            if (callback != null) {
                                callback!!.onSelectPage(card, entry, true)
                            }
                        }

                        override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                            if (addToDefault) {
                                addToDefaultList(context as AppCompatActivity, entry.title, InvokeSource.ON_THIS_DAY_CARD_BODY,
                                        AddToDefaultListCallback { readingListId: Long ->
                                            bottomSheetPresenter.show((context as AppCompatActivity).getSupportFragmentManager(),
                                                    newInstance(readingListId, entry.title, InvokeSource.ON_THIS_DAY_CARD_BODY))
                                        })
                            } else {
                                bottomSheetPresenter.show((context as AppCompatActivity).getSupportFragmentManager(),
                                        newInstance(entry.title, InvokeSource.ON_THIS_DAY_CARD_BODY))
                            }
                        }

                        override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                            bottomSheetPresenter.show((context as AppCompatActivity).getSupportFragmentManager(),
                                    newInstance(page!!.listId, entry.title, InvokeSource.ON_THIS_DAY_CARD_BODY))
                        }
                    }).show(entry)
                    true
                }
            }
        } else {
            otdEventViewBinding.page.root.visibility = GONE
        }
    }
}
