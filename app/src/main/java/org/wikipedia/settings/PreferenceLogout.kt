package org.wikipedia.settings

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
class PreferenceLogout : Preference {
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context) : super(ctx)

    var activity: Activity? = null

    init {
        widgetLayoutResource = R.layout.view_preference_logout
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.findViewById<TextView>(R.id.accountName).text = AccountUtil.userName
        holder.itemView.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            activity?.let {
                AlertDialog.Builder(it)
                    .setMessage(R.string.logout_prompt)
                    .setNegativeButton(R.string.logout_dialog_cancel_button_text, null)
                    .setPositiveButton(R.string.preference_title_logout) { _, _ ->
                        WikipediaApp.getInstance().logOut()
                        Prefs.setReadingListsLastSyncTime(null)
                        Prefs.setReadingListSyncEnabled(false)
                        Prefs.setSuggestedEditsHighestPriorityEnabled(false)
                        it.setResult(SettingsActivity.ACTIVITY_RESULT_LOG_OUT)
                        it.finish()
                    }.show()
            }
        }
    }
}
