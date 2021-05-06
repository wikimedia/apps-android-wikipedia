package org.wikipedia.feed.onthisday

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.FeedFunnel
import org.wikipedia.databinding.ViewCardOnThisDayBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.MoveToReadingListDialog
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TransitionUtil

class OnThisDayCardView(context: Context) : DefaultFeedCardView<OnThisDayCard?>(context),
    CardFooterView.Callback {

    private val binding = ViewCardOnThisDayBinding.inflate(LayoutInflater.from(context), this, true)
    private val funnel = FeedFunnel(WikipediaApp.getInstance())
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var age = 0

    init {
        binding.clickContainer.setOnClickListener { view -> onCardClicked(view) }
        binding.eventLayout.year.setOnClickListener { view -> onCardClicked(view) }
    }

    override fun onFooterClicked() {
        funnel.cardClicked(CardType.ON_THIS_DAY, card!!.wikiSite().languageCode())
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            (context as Activity),
            binding.cardHeader.titleView, context.getString(R.string.transition_on_this_day)
        )
        context.startActivity(
            OnThisDayActivity.newIntent(
                context, age, -1, card!!.wikiSite(),
                InvokeSource.ON_THIS_DAY_CARD_FOOTER
            ), options.toBundle()
        )
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        binding.cardHeader.setCallback(callback)
    }

    private fun header(card: OnThisDayCard) {
        binding.cardHeader
            .setTitle(card.title())
            .setLangCode(card.wikiSite().languageCode())
            .setCard(card)
            .setCallback(callback)
        binding.eventLayout.text.text = card.text()
        binding.eventLayout.year.text = DateUtil.yearToStringWithEra(card.year())
    }

    private fun footer(card: OnThisDayCard) {
        binding.eventLayout.pagesIndicator.visibility = GONE
        binding.cardFooterView.setFooterActionText(
            card.footerActionText(),
            card.wikiSite().languageCode()
        )
        binding.cardFooterView.callback = this
    }

    override fun setCard(card: OnThisDayCard) {
        super.setCard(card)
        age = card.age
        setLayoutDirectionByWikiSite(card.wikiSite(), binding.rtlContainer)
        binding.eventLayout.yearsText.text = DateUtil.getYearDifferenceString(card.year())
        updateOtdEventUI(card)
        header(card)
        footer(card)
    }

    private fun onCardClicked(view: View) {
        val isYearClicked = view.id == R.id.year
        funnel.cardClicked(CardType.ON_THIS_DAY, card!!.wikiSite().languageCode())
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            (context as Activity),
            binding.cardHeader.titleView, context.getString(R.string.transition_on_this_day)
        )
        context.startActivity(
            OnThisDayActivity.newIntent(
                context,
                age,
                if (isYearClicked) card!!.year() else -1,
                card!!.wikiSite(),
                if (isYearClicked) InvokeSource.ON_THIS_DAY_CARD_YEAR else InvokeSource.ON_THIS_DAY_CARD_BODY
            ), options.toBundle()
        )
    }

    private fun updateOtdEventUI(card: OnThisDayCard) {
        binding.eventLayout.pagesPager.visibility = GONE
        var chosenPage: PageSummary? = null
        if (card.pages() != null) {
            binding.eventLayout.page.root.visibility = VISIBLE
            for (pageSummary in card.pages()!!) {
                chosenPage = pageSummary
                if (pageSummary.thumbnailUrl != null) {
                    break
                }
            }
            val finalChosenPage = chosenPage
            chosenPage?.let { page ->
                if (page.thumbnailUrl == null) {
                    binding.eventLayout.page.image.visibility = GONE
                } else {
                    binding.eventLayout.page.image.visibility = VISIBLE
                    binding.eventLayout.page.image.loadImage(Uri.parse(page.thumbnailUrl))
                }
                binding.eventLayout.page.description.text = page.description
                binding.eventLayout.page.description.visibility =
                    if (page.description!!.isEmpty()) GONE else VISIBLE
                binding.eventLayout.page.title.maxLines =
                    if (page.description!!.isEmpty()) 2 else 1
                binding.eventLayout.page.title.text = StringUtil.fromHtml(page.displayTitle)
                binding.eventLayout.page.root.setOnClickListener {
                    callback?.onSelectPage(
                        card,
                        HistoryEntry(
                            finalChosenPage!!.getPageTitle(card.wikiSite()),
                            HistoryEntry.SOURCE_ON_THIS_DAY_CARD
                        ),
                        TransitionUtil.getSharedElements(context, binding.eventLayout.page.image)
                    )
                }
                binding.eventLayout.page.root.setOnLongClickListener { view ->
                    val pageTitle = finalChosenPage!!.getPageTitle(card.wikiSite())
                    val entry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_CARD)
                    LongPressMenu(view!!, true, object : LongPressMenu.Callback {
                        override fun onOpenLink(entry: HistoryEntry) {
                            callback?.onSelectPage(
                                card,
                                entry,
                                TransitionUtil.getSharedElements(
                                    context,
                                    binding.eventLayout.page.image
                                )
                            )
                        }

                        override fun onOpenInNewTab(entry: HistoryEntry) {
                            callback?.onSelectPage(card, entry, true)
                        }

                        override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                            if (addToDefault) {
                                ReadingListBehaviorsUtil.addToDefaultList(
                                    context as AppCompatActivity,
                                    entry.title, InvokeSource.ON_THIS_DAY_CARD_BODY
                                ) { readingListId ->
                                    bottomSheetPresenter.show(
                                        (context as AppCompatActivity).supportFragmentManager,
                                        MoveToReadingListDialog.newInstance(
                                            readingListId,
                                            entry.title,
                                            InvokeSource.ON_THIS_DAY_CARD_BODY
                                        )
                                    )
                                }
                            } else {
                                bottomSheetPresenter.show(
                                    (context as AppCompatActivity).supportFragmentManager,
                                    AddToReadingListDialog.newInstance(
                                        entry.title,
                                        InvokeSource.ON_THIS_DAY_CARD_BODY
                                    )
                                )
                            }
                        }

                        override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                            bottomSheetPresenter.show(
                                (context as AppCompatActivity).supportFragmentManager,
                                MoveToReadingListDialog.newInstance(
                                    page!!.listId,
                                    entry.title,
                                    InvokeSource.ON_THIS_DAY_CARD_BODY
                                )
                            )
                        }
                    }).show(entry)
                    true
                }
            }
        } else {
            binding.eventLayout.page.root.visibility = GONE
        }
    }
}
