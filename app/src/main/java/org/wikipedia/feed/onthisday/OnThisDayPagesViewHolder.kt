package org.wikipedia.feed.onthisday

import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
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
import org.wikipedia.views.FaceAndColorDetectImageView

class OnThisDayPagesViewHolder(
    private val activity: Activity,
    private val fragmentManager: FragmentManager,
    v: View,
    private val wiki: WikiSite
) : RecyclerView.ViewHolder(v) {

    private var selectedPage: PageSummary? = null
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var image: FaceAndColorDetectImageView? = null

    init {
        DeviceUtil.setContextClickAsLongClick(v)
        this.itemView.setOnClickListener { onBaseViewClicked() }
        this.itemView.setOnLongClickListener { showOverflowMenu(it) }
        image = this.itemView.findViewById(R.id.image)
    }

    fun setFields(page: PageSummary) {
        val description = this.itemView.findViewById<TextView>(R.id.description)
        val title = this.itemView.findViewById<TextView>(R.id.title)

        selectedPage = page
        description.text = page.description
        description.visibility =
            if (TextUtils.isEmpty(page.description)) View.GONE else View.VISIBLE
        title.maxLines = if (TextUtils.isEmpty(page.description)) 2 else 1
        title.text = StringUtil.fromHtml(page.displayTitle)
        setImage(page.thumbnailUrl)
    }

    private fun setImage(url: String?) {
        image?.let {
            if (url == null) {
                it.visibility = View.GONE
            } else {
                it.visibility = View.VISIBLE
                it.loadImage(Uri.parse(url))
            }
        }
    }

    fun onBaseViewClicked() {
        val entry = HistoryEntry(
            selectedPage!!.getPageTitle(wiki),
            HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY
        )
        val sharedElements = TransitionUtil.getSharedElements(activity, image!!)
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
                        AddToReadingListDialog.newInstance(
                            entry.title,
                            InvokeSource.ON_THIS_DAY_ACTIVITY
                        )
                    )
                }
            }

            override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                bottomSheetPresenter.show(
                    fragmentManager,
                    MoveToReadingListDialog.newInstance(
                        page!!.listId,
                        entry.title,
                        InvokeSource.ON_THIS_DAY_ACTIVITY
                    )
                )
            }
        }).show(entry)
        return true
    }
}
