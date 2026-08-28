package org.wikipedia.games

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.net.toUri
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityGamesHubBinding
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.NotificationButtonView

class GamesHubActivity : SingleFragmentActivity<GamesHubFragment>() {

    private lateinit var binding: ActivityGamesHubBinding
    private lateinit var notificationButtonView: NotificationButtonView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGamesHubBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.games_hub_activity_title)

        notificationButtonView = NotificationButtonView(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_games_hub_overflow, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val notificationMenuItem = menu.findItem(R.id.menu_notifications)
        if (AccountUtil.isLoggedIn) {
            notificationMenuItem.isVisible = true
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            notificationButtonView.setOnClickListener {
                if (AccountUtil.isLoggedIn) {
                    startActivity(NotificationActivity.newIntent(this))
                }
            }
            notificationButtonView.contentDescription = getString(R.string.notifications_activity_title)
            notificationMenuItem.actionView = notificationButtonView
            notificationMenuItem.expandActionView()
            FeedbackUtil.setButtonTooltip(notificationButtonView)
        } else {
            notificationMenuItem.isVisible = false
        }
        updateNotificationDot(false)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_game_stats -> {
                WikiGamesEvent.submit(action = "game_stats_click", activeInterface = "games_hub")
                val primaryLangCodeSupported = WikiGames.WHICH_CAME_FIRST.isLangSupported(WikipediaApp.instance.languageState.appLanguageCode)
                val intent = MainActivity.newIntent(this)
                    .apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, NavTab.EDITS.code())
                        putExtra(Constants.INTENT_EXTRA_SCROLL_TO_GAMES, primaryLangCodeSupported)
                        if (!WikipediaApp.instance.languageState.appLanguageCode.equals(fragment.viewModel.selectedLanguage, true) || !primaryLangCodeSupported) {
                            putExtra(Constants.INTENT_EXTRA_SNACKBAR_MESSAGE, getString(R.string.activity_tab_snackbar_label))
                        }
                    }
                startActivity(intent)
                finish()
                true
            }
            R.id.menu_learn_more -> {
                WikiGamesEvent.submit(action = "learn_more_click", activeInterface = "games_hub")
                UriUtil.visitInExternalBrowser(this, getString(R.string.games_hub_wiki_url).toUri())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    fun updateNotificationDot(animate: Boolean) {
        if (AccountUtil.isLoggedIn && Prefs.notificationUnreadCount > 0) {
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            if (animate) {
                notificationButtonView.runAnimation()
            }
        } else {
            notificationButtonView.setUnreadCount(0)
        }
    }

    override fun onUnreadNotification() {
        updateNotificationDot(true)
    }

    public override fun createFragment(): GamesHubFragment {
        return GamesHubFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GamesHubActivity::class.java)
        }
    }
}
