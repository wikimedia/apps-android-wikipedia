package org.wikipedia.page;

import android.content.*;
import android.net.*;
import android.os.*;
import com.squareup.otto.*;
import org.wikipedia.*;
import org.wikipedia.events.*;
import org.wikipedia.history.*;
import org.wikipedia.interlanguage.*;
import org.wikipedia.recurring.*;
import org.wikipedia.search.*;

import android.app.AlertDialog;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;
import org.wikipedia.settings.SettingsActivity;

public class PageActivity extends ActionBarActivity {
    public static final String ACTION_PAGE_FOR_TITLE = "org.wikipedia.page_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";
    private static final String ZERO_ON_NOTICE_PRESENTED = "org.wikipedia.zero.zeroOnNoticePresented";
    private static final String ZERO_OFF_NOTICE_PRESENTED = "org.wikipedia.zero.zeroOffNoticePresented";

    private Bus bus;
    private WikipediaApp app;

    private SearchArticlesFragment searchAriclesFragment;
    private DrawerLayout drawerLayout;

    private PageViewFragment curPageFragment;

    private boolean pausedStateOfZero;
    private String pausedXcsOfZero;
    private static final int MESSAGE_START_SCREEN = 1;
    private AlertDialog.Builder alert;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        app = ((WikipediaApp)getApplicationContext());

        if (savedInstanceState != null) {
            pausedStateOfZero = savedInstanceState.getBoolean("pausedStateOfZero");
            pausedXcsOfZero = savedInstanceState.getString("pausedXcsOfZero");
        }

        bus = app.getBus();
        bus.register(this);

        searchAriclesFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        searchAriclesFragment.setDrawerLayout(drawerLayout);

        if (savedInstanceState == null) {
            // Don't do this if we are just rotating the phone
            Intent intent = getIntent();
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Site site = new Site(intent.getData().getAuthority());
                PageTitle title = site.titleForInternalLink(intent.getData().getPath());
                HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);
                bus.post(new NewWikiPageNavigationEvent(title, historyEntry));
            } else if (ACTION_PAGE_FOR_TITLE.equals(intent.getAction())) {
                PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
                HistoryEntry historyEntry = intent.getParcelableExtra(EXTRA_HISTORYENTRY);
                bus.post(new NewWikiPageNavigationEvent(title, historyEntry));
            }
        }

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(this).run();
    }

    private void displayNewPage(PageTitle title, HistoryEntry entry) {
        PageViewFragment pageFragment = new PageViewFragment(title, entry, R.id.search_fragment);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.content_frame, pageFragment)
                .addToBackStack(null)
                .commit();
        this.curPageFragment = pageFragment;
    }

    @Subscribe
    public void onNewWikiPageNavigationEvent(NewWikiPageNavigationEvent event) {
        displayNewPage(event.getTitle(), event.getHistoryEntry());
    }

    @Subscribe
    public void onPageSaveEvent(SavePageEvent event) {
        curPageFragment.savePage();
    }

    @Subscribe
    public void onSharePageEvent(SharePageEvent event) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, curPageFragment.getTitle().getDisplayText() + " " + curPageFragment.getTitle().getCanonicalUri());
        shareIntent.setType("text/plain");
        startActivity(shareIntent);
    }

    @Subscribe
    public void onOtherLanguagesEvent(ShowOtherLanguagesEvent event) {
        Intent shareIntent = new Intent();
        shareIntent.setClass(this, LangLinksActivity.class);
        shareIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
        shareIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, curPageFragment.getTitle());
        startActivity(shareIntent);
    }

    @Subscribe
    public void onShowToCEvent(ShowToCEvent event) {
        curPageFragment.showToC();
    }
    @Override
    public void onBackPressed() {
        if ((curPageFragment != null && !curPageFragment.handleBackPressed())
                && !searchAriclesFragment.handleBackPressed()) {
            if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                // Everything we could pop has been popped....
                finish();
            } else {
                getSupportFragmentManager().popBackStack();
            }
        }
    }

    @Subscribe
    public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
        boolean latestWikipediaZeroDisposition = WikipediaApp.getWikipediaZeroDisposition();

        if (pausedStateOfZero && !latestWikipediaZeroDisposition) {
            String verbiage = getString(R.string.zero_charged_verbiage);
            Toast.makeText(app, verbiage, Toast.LENGTH_LONG).show();
            showDialogAboutZero(ZERO_OFF_NOTICE_PRESENTED, verbiage);
        } else if ((!pausedStateOfZero || !pausedXcsOfZero.equals(WikipediaApp.getXcs())) && latestWikipediaZeroDisposition) {
            String verbiage = WikipediaApp.getCarrierMessage();
            Toast.makeText(app, verbiage, Toast.LENGTH_LONG).show();
            showDialogAboutZero(ZERO_ON_NOTICE_PRESENTED, verbiage);
        }
        pausedStateOfZero = latestWikipediaZeroDisposition;
        pausedXcsOfZero = WikipediaApp.getXcs();
    }

    private void showDialogAboutZero(String prefsKey, String verbiage) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (!prefs.getBoolean(prefsKey, false)) {
            prefs.edit().putBoolean(prefsKey, true).commit();

            alert = new AlertDialog.Builder(this);
            alert.setTitle(verbiage);
            alert.setMessage(getString(R.string.zero_learn_more));
            alert.setPositiveButton(getString(R.string.zero_learn_more_learn_more), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    visitExternalLink(Uri.parse(getString(R.string.zero_webpage_url)));
                }
            });
            alert.setNegativeButton(getString(R.string.zero_learn_more_no_thanks), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            alert.create().show();
        }
    }

    @Subscribe
    public void onWikipediaZeroInterstitialEvent(final WikipediaZeroInterstitialEvent event) {
        alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.zero_interstitial_title));
        alert.setMessage(getString(R.string.zero_interstitial_leave_app));
        alert.setPositiveButton(getString(R.string.zero_interstitial_continue), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                visitExternalLink(event.getUri());
            }
        });
        alert.setNegativeButton(getString(R.string.zero_interstitial_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.setNeutralButton(getString(R.string.nav_item_preferences), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                visitSettings();
            }
        });
        alert.create().show();
    }

    private void visitSettings() {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), SettingsActivity.class);
        startActivity(intent);
    }

    private void visitExternalLink(Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bus == null) {
            bus = app.getBus();
            bus.register(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean latestWikipediaZeroDispostion = WikipediaApp.getWikipediaZeroDisposition();
        if (WikipediaApp.isWikipediaZeroDevmodeOn() && pausedStateOfZero && !latestWikipediaZeroDispostion) {
            bus.post(new WikipediaZeroStateChangeEvent());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pausedStateOfZero = WikipediaApp.getWikipediaZeroDisposition();
        pausedXcsOfZero = WikipediaApp.getXcs();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("pausedStateOfZero", pausedStateOfZero);
        outState.putString("pausedXcsOfZero", pausedXcsOfZero);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
        bus = null;
    }
}
