package org.wikipedia.page;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.*;
import android.util.*;
import android.view.*;
import com.squareup.otto.*;
import de.keyboardsurfer.android.widget.crouton.*;
import org.wikipedia.*;
import org.wikipedia.analytics.*;
import org.wikipedia.bookmarks.BookmarksActivity;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.events.*;
import org.wikipedia.history.*;
import org.wikipedia.interlanguage.*;
import org.wikipedia.pageimages.PageImageSaveTask;
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

    public static final int ACTIVITY_REQUEST_HISTORY = 0;
    public static final int ACTIVITY_REQUEST_BOOKMARKS = 1;
    public static final int ACTIVITY_REQUEST_LANGLINKS = 2;

    private Bus bus;
    private WikipediaApp app;

    private SearchArticlesFragment searchArticlesFragment;
    private DrawerLayout drawerLayout;

    /**
     * Container that will hold our WebViews, and animate between them.
     */
    private PageFragmentPager fragmentPager;

    private PageViewFragment curPageFragment;

    private PageFragmentAdapter fragmentAdapter;

    /**
     * Lightweight back-stack of history items
     */
    private BackStack backStack;

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
            if (savedInstanceState.containsKey("backStack")) {
                backStack = savedInstanceState.getParcelable("backStack");
            }
        } else {
            backStack = new BackStack();
        }

        bus = app.getBus();
        bus.register(this);

        readingActionFunnel = new ReadingActionFunnel(app);

        searchArticlesFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        searchArticlesFragment.setDrawerLayout(drawerLayout);

        fragmentPager = (PageFragmentPager) findViewById(R.id.content_pager);
        // disable the default swipe motion to flip pages (we'll be doing it programmatically)
        fragmentPager.setPagingEnabled(false);

        fragmentAdapter = new PageFragmentAdapter(getSupportFragmentManager(), backStack);
        fragmentPager.setAdapter(fragmentAdapter);

        // Set the maximum number of fragments that will be kept in memory.
        // Old Fragments will be automatically destroyed, but their state will be saved,
        // so when the user goes back, they will be recreated.
        fragmentPager.setOffscreenPageLimit(calculateMaxFragments());

        if (savedInstanceState == null) {
            // Don't do this if we are just rotating the phone
            handleIntent(getIntent());
        }

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(this).run();
    }

    private void handleIntent(Intent intent) {
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

    private int calculateMaxFragments() {
        // calculate the maximum number of WebViews to keep in memory, based on VM size
        ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        int memMegs = activityManager.getMemoryClass();
        // allow up to 7MB for the app itself, 3 MB for spikes by WebView allocations,
        // and 2 MB for each WebView stored in memory.
        int maxFragments = (memMegs - 7 - 3) / 2;
        if (maxFragments <= 0) {
            // make sure there's at least one, for really low-memory devices.
            maxFragments = 1;
        } else if (maxFragments > 6) {
            // more than this will probably break rendering.
            maxFragments = 6;
        }
        Log.d("PageActivity", "Maximum Fragments in memory: " + maxFragments);
        return maxFragments;
    }

    private void displayNewPage(final PageTitle title, final HistoryEntry entry) {
        readingActionFunnel.logSomethingHappened(title.getSite());
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        }
        if (title.isSpecial()) {
            Utils.visitInExternalBrowser(this, Uri.parse(title.getMobileUri()));
            return;
        }

        // Add history entry now
        new HistorySaveTask(entry).execute();

        // animate the new fragment into place
        // then hide the previous fragment.
        final PageViewFragment prevFragment = curPageFragment;
        fragmentPager.setOnAnimationListener(new PageFragmentPager.OnAnimationListener() {
            @Override
            public void OnAnimationFinished() {
                if (prevFragment != null) {
                    prevFragment.hide();
                }
                fragmentPager.setOnAnimationListener(null);
            }
        });

        backStack.getStack().add(new BackStackItem(title, entry, 0));

        fragmentAdapter.notifyDataSetChanged();
        fragmentPager.setCurrentItem(backStack.size() - 1);

        curPageFragment = (PageViewFragment)fragmentAdapter.getItem(fragmentPager.getCurrentItem());

        Log.d("PageActivity", "pageBackStack has " + backStack.size() + " items");
    }

    /**
     * Saving a history item needs to be in its own task, since the operation may
     * actually block for several seconds, and should not be on the main thread.
     */
    private class HistorySaveTask extends SaneAsyncTask<Void> {
        private final HistoryEntry entry;
        public HistorySaveTask(HistoryEntry entry) {
            super(SINGLE_THREAD);
            this.entry = entry;
        }

        @Override
        public Void performTask() throws Throwable {
            app.getPersister(HistoryEntry.class).persist(entry);
            return null;
        }

        @Override
        public void onCatch(Throwable caught) {
            Log.d("HistorySaveTask", caught.getMessage());
        }
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
        startActivityForResult(shareIntent, ACTIVITY_REQUEST_LANGLINKS);
    }

    @Subscribe
    public void onShowToCEvent(ShowToCEvent event) {
        curPageFragment.toggleToC();
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (!searchArticlesFragment.handleBackPressed()
                && !(curPageFragment != null && curPageFragment.handleBackPressed())) {
            if (backStack.size() <= 1) {
                // Everything we could pop has been popped....
                finish();
            } else {

                // don't do anything if we're in the middle of an animation (looks better)
                if (fragmentPager.isAnimating()) {
                    return;
                }

                // let the Pager finish its animation, then remove the fragment that was moved off.
                fragmentPager.setOnAnimationListener(new PageFragmentPager.OnAnimationListener() {
                    @Override
                    public void OnAnimationFinished() {
                        fragmentAdapter.removeFragment(backStack.size() - 1);

                        fragmentAdapter.notifyDataSetChanged();
                        fragmentPager.setOnAnimationListener(null);
                    }
                });

                fragmentPager.setCurrentItem(fragmentPager.getCurrentItem() - 1);
                curPageFragment = fragmentAdapter.getFragmentAt(fragmentPager.getCurrentItem());
                curPageFragment.show();

                searchArticlesFragment.clearErrors();
                searchArticlesFragment.ensureVisible();
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
                    Utils.visitInExternalBrowser(PageActivity.this, Uri.parse(getString(R.string.zero_webpage_url)));
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
                Utils.visitInExternalBrowser(PageActivity.this, event.getUri());
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
        boolean latestWikipediaZeroDisposition = WikipediaApp.getWikipediaZeroDisposition();
        if (WikipediaApp.isWikipediaZeroDevmodeOn() && pausedStateOfZero && !latestWikipediaZeroDisposition) {
            bus.post(new WikipediaZeroStateChangeEvent());
        }
        fragmentAdapter.onResume(this);
        if (curPageFragment != null) {
            //refresh the current fragment's state (ensures correct state of overflow menu)
            curPageFragment.show();
        }
        // if we're just being resumed from a saved state, then sync curPageFragment
        // with the correct item in the pager.
        if (curPageFragment == null && fragmentAdapter.getCount() > 0) {
            curPageFragment = (PageViewFragment)fragmentAdapter.getItem(fragmentPager.getCurrentItem());
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
        outState.putParcelable("backStack", backStack);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
        bus = null;
        Log.d("Wikipedia", "Deregistering bus");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (bus == null) {
            bus = app.getBus();
            bus.register(this);
            Log.d("Wikipedia", "Registering bus");
        }
        if ((requestCode == ACTIVITY_REQUEST_HISTORY && resultCode == HistoryActivity.ACTIVITY_RESULT_HISTORY_SELECT)
            || (requestCode == ACTIVITY_REQUEST_BOOKMARKS && resultCode == BookmarksActivity.ACTIVITY_RESULT_BOOKMARK_SELECT)
            || (requestCode == ACTIVITY_REQUEST_LANGLINKS && resultCode == LangLinksActivity.ACTIVITY_RESULT_LANGLINK_SELECT)) {
            handleIntent(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
