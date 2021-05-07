package org.wikipedia.feed.onthisday

import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.databinding.ItemOnThisDayPagesBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.MoveToReadingListDialog
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.*

class OnThisDayPagesViewHolder(
    private val activity: Activity,
    private val fragmentManager: FragmentManager,
    v: View,
    private val wiki: WikiSite
) : RecyclerView.ViewHolder(v) {

    private var binding: ItemOnThisDayPagesBinding? = null
    private var selectedPage: PageSummary? = null
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    init {
        DeviceUtil.setContextClickAsLongClick(v)
        this.itemView.setOnClickListener { onBaseViewClicked() }
        this.itemView.setOnLongClickListener { showOverflowMenu(it) }
        binding = ItemOnThisDayPagesBinding.inflate(LayoutInflater.from(activity), this.itemView as ViewGroup, false)
    }

    fun setFields(page: PageSummary) {
        selectedPage = page
        binding!!.description.text = page.description
        binding!!.description.visibility =
            if (TextUtils.isEmpty(page.description)) View.GONE else View.VISIBLE
        binding!!.title.maxLines = if (TextUtils.isEmpty(page.description)) 2 else 1
        binding!!.title.text = StringUtil.fromHtml(page.displayTitle)
        setImage(page.thumbnailUrl)
    }

    private fun setImage(url: String?) {
        if (url == null) {
            binding!!.image.visibility = View.GONE
        } else {
            binding!!.image.visibility = View.VISIBLE
            binding!!.image.loadImage(Uri.parse(url))
        }
    }

    fun onBaseViewClicked() {
        val entry = HistoryEntry(
            selectedPage!!.getPageTitle(wiki),
            HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY
        )
        val sharedElements = TransitionUtil.getSharedElements(activity, binding!!.image)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *sharedElements)
        val intent = PageActivity.newIntentForNewTab(activity, entry, entry.title)
        if (sharedElements.isNotEmpty()) {
            intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true)
        }
        activity.startActivity(
            intent,
            if (DimenUtil.isLandscape(activity) || sharedElements.isEmpty()) null else options.toBundle()
        )
    }

    fun showOverflowMenu(anchorView: View?): Boolean {
        val entry = HistoryEntry(
            selectedPage!!.getPageTitle(wiki),
            HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY
        )
        LongPressMenu(anchorView!!, true, object : LongPressMenu.Callback {
            override fun onOpenLink(entry: HistoryEntry) {
                PageActivity.newIntentForNewTab(activity, entry, entry.title)
            }

            override fun onOpenInNewTab(entry: HistoryEntry) {
                TabUtil.openInNewBackgroundTab(entry)
                FeedbackUtil.showMessage(activity, R.string.article_opened_in_background_tab)
            }

            override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                if (addToDefault) {
                    ReadingListBehaviorsUtil.addToDefaultList(
                        activity, entry.title, InvokeSource.NEWS_ACTIVITY
                    ) { readingListId: Long ->
                        bottomSheetPresenter.show(
                            fragmentManager,
                            MoveToReadingListDialog.newInstance(
                                readingListId,
                                entry.title,
                                InvokeSource.ON_THIS_DAY_ACTIVITY
                            )
                        )
                    }
                } else {
                    bottomSheetPresenter.show(
                        fragmentManager,
                        AddToReadingListDialog.newInstance(entry.title, InvokeSource.ON_THIS_DAY_ACTIVITY)
                    )
                }
            }

            override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                bottomSheetPresenter.show(
                    fragmentManager,
                    MoveToReadingListDialog.newInstance(page!!.listId, entry.title, InvokeSource.ON_THIS_DAY_ACTIVITY)
                )
            }
        }).show(entry)
        return true
    }
}
