package org.wikipedia.page;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.wikipedia.*;
import org.wikipedia.events.*;
import org.wikipedia.onboarding.OnboardingActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.LangLinksActivity;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.search.FullSearchFragment;
import org.wikipedia.search.SearchArticlesFragment;
import org.wikipedia.settings.PrefKeys;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.theme.ThemeChooserDialog;

public class PageActivity extends ThemedActionBarActivity {
    public static final String ACTION_PAGE_FOR_TITLE = "org.wikipedia.page_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";
    private static final String ZERO_ON_NOTICE_PRESENTED = "org.wikipedia.zero.zeroOnNoticePresented";

    private static final String KEY_LAST_FRAGMENT = "lastFragment";
    private static final String KEY_LAST_FRAGMENT_ARGS = "lastFragmentArgs";

    public static final int ACTIVITY_REQUEST_LANGLINKS = 0;
    public static final int ACTIVITY_REQUEST_EDIT_SECTION = 1;

    private Bus bus;
    private WikipediaApp app;

    private View fragmentContainerView;
    private SearchArticlesFragment searchArticlesFragment;
    private DrawerLayout drawerLayout;
    private NavDrawerFragment fragmentNavdrawer;
    private FindInPageFragment findInPageFragment;

    /**
     * Get the Fragment that is currently at the top of the Activity's backstack.
     * This activity's fragment container will hold multiple fragments stacked onto
     * each other using FragmentManager, and this function will return the current
     * topmost Fragment. It's up to the caller to cast the result to a more specific
     * fragment class, and perform actions on it.
     * @return Fragment at the top of the backstack.
     */
    public Fragment getTopFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_fragment_container);
    }

    /**
     * Get the PageViewFragment that is currently at the top of the Activity's backstack.
     * If the current topmost fragment is not a PageViewFragment, return null.
     * @return The PageViewFragment at the top of the backstack, or null if the current
     * top fragment is not a PageViewFragment.
     */
    public PageViewFragmentInternal getCurPageFragment() {
        Fragment f = getTopFragment();
        if (f instanceof PageViewFragment) {
            return ((PageViewFragment)f).getFragment();
        } else {
            return null;
        }
    }

    private boolean pausedStateOfZero;
    private String pausedXcsOfZero;

    private AlertDialog.Builder alert;

    private ThemeChooserDialog themeChooser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        app = (WikipediaApp) getApplicationContext();
        setTheme(app.getCurrentTheme());
        requestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        bus = app.getBus();
        bus.register(this);

        if (savedInstanceState != null) {
            pausedStateOfZero = savedInstanceState.getBoolean("pausedStateOfZero");
            pausedXcsOfZero = savedInstanceState.getString("pausedXcsOfZero");
        } else if (getIntent().hasExtra("changeTheme")) {
            // we've changed themes!
            pausedStateOfZero = getIntent().getExtras().getBoolean("pausedStateOfZero");
            pausedXcsOfZero = getIntent().getExtras().getString("pausedXcsOfZero");
            if (getIntent().getExtras().containsKey("themeChooserShowing")) {
                if (getIntent().getExtras().getBoolean("themeChooserShowing")) {
                    bus.post(new ShowThemeChooserEvent());
                }
            }
        }

        findInPageFragment = (FindInPageFragment) getSupportFragmentManager().findFragmentById(R.id.find_in_page_fragment);
        searchArticlesFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        fragmentNavdrawer = (NavDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navdrawer);

        fragmentContainerView = findViewById(R.id.content_fragment_container);

        searchArticlesFragment.setDrawerLayout(drawerLayout);

        // If we're coming back from a Theme change, we'll need to "restore" our state based on
        // what's given in our Intent (since there's no way to relaunch the Activity in a way that
        // forces it to save its own instance state)...
        if (getIntent().hasExtra("changeTheme")) {
            String className = getIntent().getExtras().getString(KEY_LAST_FRAGMENT);
            try {
                // instantiate the last fragment that was on top of the backstack before the Activity
                // was closed:
                Fragment f = (Fragment) Class.forName(className).getConstructor().newInstance();
                // if we have arguments for the fragment, even better:
                if (getIntent().getExtras().containsKey(KEY_LAST_FRAGMENT_ARGS)) {
                    f.setArguments(getIntent().getExtras().getBundle(KEY_LAST_FRAGMENT_ARGS));
                }
                // ...and put it on top:
                pushFragment(f);
            } catch (Exception e) {
                //multiple various exceptions may be thrown in the above few lines, so just catch all.
                Log.e("PageActivity", "Error while instantiating fragment.", e);
                //don't let the user see a blank screen, so just request the main page...
                bus.post(new RequestMainPageEvent());
            }
        } else if (savedInstanceState == null) {
            // if there's no savedInstanceState, and we're not coming back from a Theme change,
            // then we must have been launched with an Intent, so... handle it!
            handleIntent(getIntent());
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
            PageTitle title = site.titleForUri(intent.getData());
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

    /**
     * Add a new fragment to the top of the activity's backstack.
     * @param f New fragment to place on top.
     */
    public void pushFragment(Fragment f) {
        drawerLayout.closeDrawer(Gravity.START);
        // if the new fragment is the same class as the current topmost fragment,
        // then just keep the previous fragment there. (unless it's a PageViewFragment)
        // e.g. if the user selected History, and there's already a History fragment on top,
        // then there's no need to load a new History fragment.
        if (getTopFragment() != null
                && (getTopFragment().getClass() == f.getClass())
                && !(f instanceof PageViewFragment)) {
            return;
        }
        int totalFragments = getSupportFragmentManager().getBackStackEntryCount();
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

        // do an animation on the new fragment, but only if there was a previous one before it.
        if (getTopFragment() != null) {
            trans.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
        }

        trans.replace(R.id.content_fragment_container, f, "fragment_" + Integer.toString(totalFragments));
        trans.addToBackStack(null);
        trans.commit();
    }

    /**
     * Remove the fragment that is currently at the top of the backstack, and go back to
     * the previous fragment.
     */
    public void popFragment() {
        getSupportFragmentManager().popBackStack();
    }

    public void searchFullText(final String searchTerm) {
        if (getTopFragment() instanceof FullSearchFragment) {
            ((FullSearchFragment)getTopFragment()).newSearch(searchTerm);
        } else {
            pushFragment(FullSearchFragment.newInstance(searchTerm));
        }
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

        //is the new title the same as what's already being displayed?
        if (getTopFragment() instanceof PageViewFragment) {
            if (((PageViewFragment)getTopFragment()).getFragment().getTitle().equals(title)) {
                return;
            }
        }

        pushFragment(PageViewFragment.newInstance(title, entry));

        app.getSessionFunnel().pageViewed(entry);
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
        if (getCurPageFragment() == null) {
            return;
        }
        // This means the user explicitly chose to save a new saved pages
        app.getFunnelManager().getSavedPagesFunnel(getCurPageFragment().getTitle().getSite()).logSaveNew();

        if (getCurPageFragment().getHistoryEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
            // refreshing a saved page...
            getCurPageFragment().refreshPage(true);
        } else {
            getCurPageFragment().savePage();
        }
    }

    @Subscribe
    public void onSharePageEvent(SharePageEvent event) {
        if (getCurPageFragment() == null) {
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getCurPageFragment().getTitle().getCanonicalUri());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getCurPageFragment().getTitle().getDisplayText());
        shareIntent.setType("text/plain");
        Intent chooser = Intent.createChooser(shareIntent, getResources().getString(R.string.share_via));
        startActivity(chooser);
    }

    @Subscribe
    public void onOtherLanguagesEvent(ShowOtherLanguagesEvent event) {
        if (getCurPageFragment() == null) {
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setClass(this, LangLinksActivity.class);
        shareIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
        shareIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, getCurPageFragment().getTitle());
        startActivityForResult(shareIntent, ACTIVITY_REQUEST_LANGLINKS);
    }

    @Subscribe
    public void onShowToCEvent(ShowToCEvent event) {
        if (getCurPageFragment() == null) {
            return;
        }
        getCurPageFragment().toggleToC(event.getAction());
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
        if (getCurPageFragment() != null && getCurPageFragment().getWebView() != null) {
            getCurPageFragment().updateFontSize();
        }
    }

    @Subscribe
    public void onChangeTheme(ThemeChangeEvent event) {
        Bundle state = new Bundle();
        Intent intent = new Intent(this, PageActivity.class);

        // In order to change our theme, we need to relaunch the activity.
        // There doesn't seem to be a way to relaunch an activity in a way that forces it to save its
        // instance state (and all of its fragments' instance state)... so we need to explicitly save
        // the state that we need, and pass it into the Intent.
        // We'll simply save the last Fragment that was on top of the backstack, as well as its arguments.
        Fragment curFragment = getSupportFragmentManager().findFragmentById(R.id.content_fragment_container);
        state.putString(KEY_LAST_FRAGMENT, curFragment.getClass().getName());
        // if the fragment had arguments, save them too:
        if (curFragment.getArguments() != null) {
            state.putBundle(KEY_LAST_FRAGMENT_ARGS, curFragment.getArguments());
        }

        saveState(state);
        state.putBoolean("changeTheme", true);
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
        if (!(getCurPageFragment() != null && getCurPageFragment().handleBackPressed())) {

            app.getSessionFunnel().backPressed();

            if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                popFragment();
            } else {
                finish();
            }

            searchArticlesFragment.clearErrors();
            searchArticlesFragment.ensureVisible();

        }
    }

    @Subscribe
    public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
        boolean latestWikipediaZeroDisposition = app.getWikipediaZeroHandler().isZeroEnabled();
        String latestCarrierMessage = app.getWikipediaZeroHandler().getCarrierMessage();

        if (pausedStateOfZero && !latestWikipediaZeroDisposition) {
            String title = getString(R.string.zero_charged_verbiage);
            String verbiage = getString(R.string.zero_charged_verbiage_extended);
            makeWikipediaZeroCrouton(R.color.holo_red_dark, android.R.color.white, title);
            fragmentNavdrawer.setupDynamicItems();
            showDialogAboutZero(null, title, verbiage);
        } else if ((!pausedStateOfZero || !pausedXcsOfZero.equals(latestCarrierMessage)) && latestWikipediaZeroDisposition) {
            String title = latestCarrierMessage;
            String verbiage = getString(R.string.zero_learn_more);
            makeWikipediaZeroCrouton(R.color.holo_green_light, android.R.color.black, title);
            fragmentNavdrawer.setupDynamicItems();
            showDialogAboutZero(ZERO_ON_NOTICE_PRESENTED, title, verbiage);
        }
        pausedStateOfZero = latestWikipediaZeroDisposition;
        pausedXcsOfZero = latestCarrierMessage;
    }

    private void makeWikipediaZeroCrouton(int bgcolor, int fgcolor, String verbiage) {
        final int zeroTextSize = 20;
        final float croutonHeight = 192.0f;
        Style style = new Style.Builder()
                .setBackgroundColor(bgcolor)
                .setGravity(Gravity.CENTER)
                // .setTextAppearance-driven font size is not being honored, so we'll do it manually
                // Text size in library is in sp
                .setTextSize(zeroTextSize)
                .setTextColor(fgcolor)
                // Height size in library is in px
                .setHeight((int) Math.floor(croutonHeight * WikipediaApp.getInstance().getScreenDensity()))
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
        boolean latestWikipediaZeroDisposition = app.getWikipediaZeroHandler().isZeroEnabled();
        if (pausedStateOfZero && !latestWikipediaZeroDisposition) {
            bus.post(new WikipediaZeroStateChangeEvent());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pausedStateOfZero = app.getWikipediaZeroHandler().isZeroEnabled();
        pausedXcsOfZero = app.getWikipediaZeroHandler().getCarrierMessage();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    private void saveState(Bundle outState) {
        outState.putBoolean("pausedStateOfZero", pausedStateOfZero);
        outState.putString("pausedXcsOfZero", pausedXcsOfZero);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (bus == null) {
            bus = app.getBus();
            bus.register(this);
            Log.d("Wikipedia", "Registering bus");
        }
        if ((requestCode == ACTIVITY_REQUEST_LANGLINKS && resultCode == LangLinksActivity.ACTIVITY_RESULT_LANGLINK_SELECT)) {
            fragmentContainerView.post(new Runnable() {
                @Override
                public void run() {
                    handleIntent(data);
                }
            });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStop() {
        if (themeChooser != null && themeChooser.isShowing()) {
            themeChooser.dismiss();
        }

        app.getSessionFunnel().persistSession();

        super.onStop();
        bus.unregister(this);
        bus = null;
        Log.d("Wikipedia", "Deregistering bus");
    }
}
