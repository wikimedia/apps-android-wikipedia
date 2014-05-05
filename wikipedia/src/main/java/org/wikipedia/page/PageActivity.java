package org.wikipedia.page;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.support.v4.widget.*;
import android.support.v7.app.*;
import android.util.*;
import android.view.*;
import com.squareup.otto.*;
import de.keyboardsurfer.android.widget.crouton.*;
import org.wikipedia.*;
import org.wikipedia.analytics.*;
import org.wikipedia.events.*;
import org.wikipedia.history.*;
import org.wikipedia.interlanguage.*;
import org.wikipedia.recurring.*;
import org.wikipedia.search.*;
import org.wikipedia.settings.*;
import org.wikipedia.staticdata.*;

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

    private AlertDialog.Builder alert;

    private ReadingActionFunnel readingActionFunnel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        app = ((WikipediaApp)getApplicationContext());

        if (savedInstanceState != null) {
            pausedStateOfZero = savedInstanceState.getBoolean("pausedStateOfZero");
            pausedXcsOfZero = savedInstanceState.getString("pausedXcsOfZero");
            if (savedInstanceState.containsKey("curPageFragment")) {
                curPageFragment = (PageViewFragment) getSupportFragmentManager().getFragment(savedInstanceState, "curPageFragment");
                curPageFragment.show();
            }
        }

        bus = app.getBus();
        bus.register(this);

        readingActionFunnel = new ReadingActionFunnel(app);

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
            } else {
                // Unrecognized, let us load the main page!
                // FIXME: Design something better for this?
                bus.post(new RequestMainPageEvent());
            }
        }

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(this).run();
    }

    private void displayNewPage(PageTitle title, HistoryEntry entry) {
        readingActionFunnel.logSomethingHappened(title.getSite());
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        }
        if(title.isSpecial()) {
            Utils.visitInExternalBrowser(this, Uri.parse(title.getMobileUri()));
            return;
        }
        PageViewFragment pageFragment = new PageViewFragment(title, entry, R.id.search_fragment);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.content_frame, pageFragment, title.getCanonicalUri())
                .addToBackStack(title.getCanonicalUri())
                .commit();

        if (curPageFragment != null) {
            curPageFragment.hide();
        }
        curPageFragment = pageFragment;
    }

    @Subscribe
    public void onRequestMainPageEvent(RequestMainPageEvent event) {
        PageTitle title = new PageTitle(MainPageNameData.valueFor(app.getPrimaryLanguage()), app.getPrimarySite());
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_MAIN_PAGE);
        displayNewPage(title, historyEntry);
        Log.d("Wikipedia", "Doing for " + title);
    }

    @Subscribe
    public void onNewWikiPageNavigationEvent(NewWikiPageNavigationEvent event) {
        displayNewPage(event.getTitle(), event.getHistoryEntry());
    }

    @Subscribe
    public void onPageSaveEvent(BookmarkPageEvent event) {
        curPageFragment.bookmarkPage();
    }

    @Subscribe
    public void onSharePageEvent(SharePageEvent event) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, curPageFragment.getTitle().getCanonicalUri());
        shareIntent.putExtra(Intent.EXTRA_TITLE, curPageFragment.getTitle().getDisplayText());
        shareIntent.setType("text/plain");
        Intent chooser = Intent.createChooser(shareIntent, getResources().getString(R.string.share_via));
        startActivity(chooser);
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
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (!searchAriclesFragment.handleBackPressed()
                && !(curPageFragment != null && curPageFragment.handleBackPressed())) {
            if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                // Everything we could pop has been popped....
                finish();
            } else {
                getSupportFragmentManager().popBackStackImmediate();
                String tag = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
                curPageFragment = (PageViewFragment) getSupportFragmentManager().findFragmentByTag(tag);
                curPageFragment.show();
            }
        }
    }

    @Subscribe
    public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
        boolean latestWikipediaZeroDisposition = WikipediaApp.getWikipediaZeroDisposition();

        if (pausedStateOfZero && !latestWikipediaZeroDisposition) {
            String verbiage = getString(R.string.zero_charged_verbiage);
            makeWikipediaZeroCrouton(R.color.holo_red_dark, android.R.color.white, verbiage);
            showDialogAboutZero(ZERO_OFF_NOTICE_PRESENTED, verbiage);
        } else if ((!pausedStateOfZero || !pausedXcsOfZero.equals(WikipediaApp.getXcs())) && latestWikipediaZeroDisposition) {
            String verbiage = WikipediaApp.getCarrierMessage();
            makeWikipediaZeroCrouton(R.color.holo_green_light, android.R.color.black, verbiage);
            showDialogAboutZero(ZERO_ON_NOTICE_PRESENTED, verbiage);
        }
        pausedStateOfZero = latestWikipediaZeroDisposition;
        pausedXcsOfZero = WikipediaApp.getXcs();
    }

    private void makeWikipediaZeroCrouton(int bgcolor, int fgcolor, String verbiage) {
        Style style = new Style.Builder()
                .setBackgroundColor(bgcolor)
                .setGravity(Gravity.CENTER)
                // .setTextAppearance-driven font size is not being honored, so we'll do it manually
                // Text size in library is in sp
                .setTextSize(20)
                .setTextColor(fgcolor)
                // Height size in library is in px
                .setHeight((int) Math.floor(192.0 * WikipediaApp.SCREEN_DENSITY))
                .build();

        Crouton.makeText(this, verbiage, style, R.id.zero_crouton_container).show();
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
            alert.setNegativeButton(getString(R.string.zero_learn_more_dismiss), new DialogInterface.OnClickListener() {
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
            Log.d("Wikipedia", "Registering bus");
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
        if (curPageFragment != null) {
            getSupportFragmentManager().putFragment(outState, "curPageFragment", curPageFragment);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
        bus = null;
        Log.d("Wikipedia", "Deregistering bus");
    }

}
