package org.wikipedia.settings

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleWebViewActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.StringUtil

@Suppress("unused")
class LogoutPreference : Preference {
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    var activity: Activity? = null

    init {
        layoutResource = R.layout.view_preference_logout
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false
        holder.itemView.findViewById<TextView>(R.id.accountName).text = AccountUtil.userName
        holder.itemView.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            activity?.let {
                MaterialAlertDialogBuilder(it)
                    .setMessage(R.string.logout_prompt)
                    .setNegativeButton(R.string.logout_dialog_cancel_button_text, null)
                    .setPositiveButton(R.string.preference_title_logout) { _, _ ->
                        WikipediaApp.instance.logOut()
                        Prefs.readingListsLastSyncTime = null
                        Prefs.isReadingListSyncEnabled = false
                        Prefs.isSuggestedEditsHighestPriorityEnabled = false
                        it.setResult(SettingsActivity.ACTIVITY_RESULT_LOG_OUT)
                        it.finish()
                    }.show()
            }
        }
        holder.itemView.findViewById<View>(R.id.accountVanishButton).setOnClickListener {
            activity?.let {
                MaterialAlertDialogBuilder(it, R.style.AlertDialogTheme_Icon_Delete)
                    .setIcon(R.drawable.ic_person_remove)
                    .setTitle(R.string.account_vanish_request_confirm_title)
                    .setMessage(StringUtil.fromHtml(it.getString(R.string.account_vanish_request_confirm)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.account_vanish_request_title) { _, _ ->
                        it.finish()
                        it.startActivity(SingleWebViewActivity.newIntent(it, it.getString(R.string.account_vanish_url), isWebForm = true))
                    }.show()
            }
        }

        // TODO: remove when ready
        holder.itemView.findViewById<View>(R.id.accountVanishButton).isVisible = ReleaseUtil.isPreProdRelease
    }
}
