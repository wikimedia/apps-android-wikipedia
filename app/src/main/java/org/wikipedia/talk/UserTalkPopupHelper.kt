package org.wikipedia.talk

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.FeedbackUtil

@SuppressLint("RestrictedApi")
object UserTalkPopupHelper {

    fun show(activity: AppCompatActivity, bottomSheetPresenter: ExclusiveBottomSheetPresenter,
             title: PageTitle, anchorView: View) {
        val pos = IntArray(2)
        anchorView.getLocationInWindow(pos)
        show(activity, bottomSheetPresenter, title, pos[0], pos[1])
    }

    fun show(activity: AppCompatActivity, bottomSheetPresenter: ExclusiveBottomSheetPresenter,
             title: PageTitle, x: Int, y: Int) {
        if (title.namespace() == Namespace.USER_TALK || title.namespace() == Namespace.TALK) {
            activity.startActivity(TalkTopicsActivity.newIntent(activity, title, Constants.InvokeSource.TALK_ACTIVITY))
        } else if (title.namespace() == Namespace.USER) {
            val rootView = activity.window.decorView
            val anchorView = View(activity)
            anchorView.x = (x - rootView.left).toFloat()
            anchorView.y = (y - rootView.top).toFloat()
            (rootView as ViewGroup).addView(anchorView)

            val helper = getPopupHelper(activity, title, anchorView)
            helper.setOnDismissListener {
                rootView.removeView(anchorView)
            }

            helper.show()
        } else {
            bottomSheetPresenter.show(activity.supportFragmentManager,
                    LinkPreviewDialog.newInstance(HistoryEntry(title, HistoryEntry.SOURCE_TALK_TOPIC), null))
        }
    }

    private fun getPopupHelper(activity: Activity, title: PageTitle, anchorView: View): MenuPopupHelper {
        val builder = MenuBuilder(activity)
        activity.menuInflater.inflate(R.menu.menu_user_talk_popup, builder)
        builder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.menu_user_profile_page -> {
                        val entry = HistoryEntry(title, HistoryEntry.SOURCE_TALK_TOPIC)
                        activity.startActivity(PageActivity.newIntentForNewTab(activity, entry, title))
                    }
                    R.id.menu_user_talk_page -> {
                        val newTitle = PageTitle(UserTalkAliasData.valueFor(title.wikiSite.languageCode), title.text, title.wikiSite)
                        activity.startActivity(TalkTopicsActivity.newIntent(activity, newTitle, Constants.InvokeSource.TALK_ACTIVITY))
                    }
                    R.id.menu_user_contributions_page -> {
                        FeedbackUtil.showUserContributionsPage(activity, title.text, title.wikiSite.languageCode)
                    }
                }
                return true
            }

            override fun onMenuModeChange(menu: MenuBuilder) { }
        })
        val helper = MenuPopupHelper(activity, builder, anchorView)
        helper.setForceShowIcon(true)
        return helper
    }
}
