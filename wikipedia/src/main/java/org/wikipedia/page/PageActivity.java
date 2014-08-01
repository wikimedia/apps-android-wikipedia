package org.wikipedia.page;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.wikipedia.events.*;
import org.wikipedia.onboarding.OnboardingActivity;
import org.wikipedia.savedpages.SavedPagesActivity;
import org.wikipedia.NavDrawerFragment;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.LangLinksActivity;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.search.SearchArticlesFragment;
import org.wikipedia.settings.PrefKeys;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.theme.ThemeChooserDialog;

public class PageActivity extends FragmentActivity {
    public static final String ACTION_PAGE_FOR_TITLE = "org.wikipedia.page_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";
    private static final String ZERO_ON_NOTICE_PRESENTED = "org.wikipedia.zero.zeroOnNoticePresented";

    public static final int ACTIVITY_REQUEST_HISTORY = 0;
    public static final int ACTIVITY_REQUEST_SAVEDPAGES = 1;
    public static final int ACTIVITY_REQUEST_LANGLINKS = 2;

    private Bus bus;
    private WikipediaApp app;

    private SearchArticlesFragment searchArticlesFragment;
    private DrawerLayout drawerLayout;
    private NavDrawerFragment fragmentNavdrawer;
    private FindInPageFragment findInPageFragment;

    /**
     * Container that will hold our WebViews, and animate between them.
     */
    private PageFragmentPager fragmentPager;

    private PageViewFragment curPageFragment;
    public PageViewFragment getCurPageFragment() {
        return curPageFragment;
    }

    private PageFragmentAdapter fragmentAdapter;

    /**
     * Lightweight back-stack of history items
     */
    private BackStack backStack;

    private boolean pausedStateOfZero;
    private String pausedXcsOfZero;

    private AlertDialog.Builder alert;

    private ThemeChooserDialog themeChooser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        app = (WikipediaApp) getApplicationContext();
        setTheme(app.getCurrentTheme());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        bus = app.getBus();
        bus.register(this);

        if (savedInstanceState != null) {
            pausedStateOfZero = savedInstanceState.getBoolean("pausedStateOfZero");
            pausedXcsOfZero = savedInstanceState.getString("pausedXcsOfZero");
            if (savedInstanceState.containsKey("backStack")) {
                backStack = savedInstanceState.getParcelable("backStack");
            }
        } else if (getIntent().hasExtra("changeTheme")) {
            // we've changed themes!
            pausedStateOfZero = getIntent().getExtras().getBoolean("pausedStateOfZero");
            pausedXcsOfZero = getIntent().getExtras().getString("pausedXcsOfZero");
            backStack = getIntent().getExtras().getParcelable("backStack");
            if (getIntent().getExtras().containsKey("themeChooserShowing")) {
                if (getIntent().getExtras().getBoolean("themeChooserShowing")) {
                    bus.post(new ShowThemeChooserEvent());
                }
            }
        } else {
            backStack = new BackStack();
        }

        findInPageFragment = (FindInPageFragment) getSupportFragmentManager().findFragmentById(R.id.find_in_page_fragment);
        searchArticlesFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        fragmentNavdrawer = (NavDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navdrawer);

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

        if (savedInstanceState == null && !getIntent().hasExtra("changeTheme")) {
            // Don't do this if we are just rotating the phone, or changing themes
            handleIntent(getIntent());
        }

        // if we've changed themes, then restore the previous position in the Pager,
        // and remove the changeTheme flag from the Intent. (Apparently, some devices
        // actually preserve the Extras, and pass them to future intents...)
        if (getIntent().hasExtra("changeTheme")) {
            fragmentPager.setCurrentItem(getIntent().getExtras().getInt("fragmentPagerItem"));
            getIntent().removeExtra("changeTheme");
        }

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(this).run();

        if (showOnboarding()) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
    }

    /**
     * @return true if
     * (1:) the app was launched from the launcher (and not another app, like the browser) AND
     * (2:) none of the onboarding screen buttons had been clicked AND
     * (3:) the user is not logged in
     */
    private boolean showOnboarding() {
        return (getIntent() == null || Intent.ACTION_MAIN.equals(getIntent().getAction()))
                && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PrefKeys.getOnboard(), false)
                && !app.getUserInfoStorage().isLoggedIn();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
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
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        }
        findInPageFragment.clear();
        findInPageFragment.hide();
        if (title.isSpecial()) {
            Utils.visitInExternalBrowser(this, Uri.parse(title.getMobileUri()));
            return;
        }

        // hold on... is this the same page that's already being displayed?
        if (curPageFragment != null && curPageFragment.getTitle().equals(title)) {
            return;
        }

        // animate the new fragment into place
        // then hide the previous fragment.
        final PageViewFragment prevFragment = curPageFragment;
        fragmentPager.setOnAnimationListener(new PageFragmentPager.OnAnimationListener() {
            @Override
            public void onAnimationFinished() {
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
    public void onPageSaveEvent(SavePageEvent event) {
        if (curPageFragment == null) {
            return;
        }
        // This means the user explicitly chose to save a new saved pages
        app.getFunnelManager().getSavedPagesFunnel(curPageFragment.getTitle().getSite()).logSaveNew();

        if (curPageFragment.getHistoryEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
            // refreshing a saved page...
            curPageFragment.refreshPage(true);
        } else {
            curPageFragment.savePage();
        }
    }

    @Subscribe
    public void onSharePageEvent(SharePageEvent event) {
        if (curPageFragment == null) {
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, curPageFragment.getTitle().getCanonicalUri());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, curPageFragment.getTitle().getDisplayText());
        shareIntent.setType("text/plain");
        Intent chooser = Intent.createChooser(shareIntent, getResources().getString(R.string.share_via));
        startActivity(chooser);
    }

    @Subscribe
    public void onOtherLanguagesEvent(ShowOtherLanguagesEvent event) {
        if (curPageFragment == null) {
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setClass(this, LangLinksActivity.class);
        shareIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
        shareIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, curPageFragment.getTitle());
        startActivityForResult(shareIntent, ACTIVITY_REQUEST_LANGLINKS);
    }

    @Subscribe
    public void onShowToCEvent(ShowToCEvent event) {
        if (curPageFragment == null) {
            return;
        }
        curPageFragment.toggleToC();
    }

    @Subscribe
    public void onFindInPage(FindInPageEvent event) {
        findInPageFragment.show();
    }

    @Subscribe
    public void onShowThemeChooser(ShowThemeChooserEvent event) {
        if (themeChooser == null) {
            themeChooser = new ThemeChooserDialog(this);
        }
        themeChooser.show();
    }

    @Subscribe
    public void onChangeTextSize(ChangeTextSizeEvent event) {
        if (curPageFragment != null && curPageFragment.getWebView() != null) {
            curPageFragment.updateFontSize();
        }
    }

    @Subscribe
    public void onChangeTheme(ThemeChangeEvent event) {
        Bundle state = new Bundle();
        Intent intent = new Intent(this, PageActivity.class);
        saveState(state);
        state.putBoolean("changeTheme", true);
        state.putInt("fragmentPagerItem", fragmentPager.getCurrentItem());
        if (themeChooser != null) {
            state.putBoolean("themeChooserShowing", themeChooser.isShowing());
        }
        finish();
        intent.putExtras(state);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (findInPageFragment.handleBackPressed()) {
            return;
        }
        if (searchArticlesFragment.handleBackPressed()) {
            return;
        }
        if (!(curPageFragment != null && curPageFragment.handleBackPressed())) {
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
                    public void onAnimationFinished() {
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
            String title = getString(R.string.zero_charged_verbiage);
            String verbiage = getString(R.string.zero_charged_verbiage_extended);
            makeWikipediaZeroCrouton(R.color.holo_red_dark, android.R.color.white, title);
            fragmentNavdrawer.setupDynamicItems();
            showDialogAboutZero(null, title, verbiage);
        } else if ((!pausedStateOfZero || !pausedXcsOfZero.equals(WikipediaApp.getXcs())) && latestWikipediaZeroDisposition) {
            String title = WikipediaApp.getCarrierMessage();
            String verbiage = getString(R.string.zero_learn_more);
            makeWikipediaZeroCrouton(R.color.holo_green_light, android.R.color.black, title);
            fragmentNavdrawer.setupDynamicItems();
            showDialogAboutZero(ZERO_ON_NOTICE_PRESENTED, title, verbiage);
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
                .setHeight((int) Math.floor(192.0 * WikipediaApp.getInstance().getScreenDensity()))
                .build();

        Crouton.makeText(this, verbiage, style, R.id.zero_crouton_container).show();
    }

    private void showDialogAboutZero(final String prefsKey, String title, String message) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (prefsKey == null || !prefs.getBoolean(prefsKey, false)) {
            if (prefsKey != null) {
                prefs.edit().putBoolean(prefsKey, true).commit();
            }

            alert = new AlertDialog.Builder(this);
            alert.setMessage(Html.fromHtml("<b>" + title + "</b><br/><br/>" + message));
            if (prefsKey != null) {
                alert.setPositiveButton(getString(R.string.zero_learn_more_learn_more), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Utils.visitInExternalBrowser(PageActivity.this, Uri.parse(getString(R.string.zero_webpage_url)));
                    }
                });
            }
            alert.setNegativeButton(getString(R.string.zero_learn_more_dismiss), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog ad = alert.create();
            ad.show();
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
        AlertDialog ad = alert.create();
        ad.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            drawerLayout.openDrawer(Gravity.START);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        saveState(outState);
    }

    private void saveState(Bundle outState) {
        outState.putBoolean("pausedStateOfZero", pausedStateOfZero);
        outState.putString("pausedXcsOfZero", pausedXcsOfZero);
        outState.putParcelable("backStack", backStack);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (bus == null) {
            bus = app.getBus();
            bus.register(this);
            Log.d("Wikipedia", "Registering bus");
        }
        if ((requestCode == ACTIVITY_REQUEST_HISTORY && resultCode == HistoryActivity.ACTIVITY_RESULT_HISTORY_SELECT)
            || (requestCode == ACTIVITY_REQUEST_SAVEDPAGES && resultCode == SavedPagesActivity.ACTIVITY_RESULT_SAVEDPAGE_SELECT)
            || (requestCode == ACTIVITY_REQUEST_LANGLINKS && resultCode == LangLinksActivity.ACTIVITY_RESULT_LANGLINK_SELECT)) {
            handleIntent(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStop() {
        if (themeChooser != null && themeChooser.isShowing()) {
            themeChooser.dismiss();
        }

        super.onStop();
        bus.unregister(this);
        bus = null;
        Log.d("Wikipedia", "Deregistering bus");
    }
}
