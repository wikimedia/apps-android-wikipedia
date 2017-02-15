package org.wikipedia.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentToolbarActivity;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.util.log.L;

public class MainActivity extends SingleFragmentToolbarActivity<MainFragment>
        implements MainFragment.Callback {

    private WikipediaApp app;
    private Bus bus;
    private MainActivity.EventBusMethods busMethods;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getApplicationContext();
        busMethods = new EventBusMethods();
        registerBus();
    }

    @Override protected MainFragment createFragment() {
        return MainFragment.newInstance();
    }

    @Override
    public void onTabChanged(@NonNull NavTab tab) {
        if (tab.equals(NavTab.EXPLORE)) {
            getToolbarWordmark().setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle("");
        } else {
            getToolbarWordmark().setVisibility(View.GONE);
            getSupportActionBar().setTitle(tab.text());
        }
    }

    @Override
    public void onSearchOpen() {
        getToolbar().setVisibility(View.GONE);
        setStatusBarColor(android.R.color.black);
    }

    @Override
    public void onSearchClose(boolean shouldFinishActivity) {
        getToolbar().setVisibility(View.VISIBLE);
        setStatusBarColor(R.color.dark_blue);
        if (shouldFinishActivity) {
            finish();
        }
    }

    @NonNull
    @Override
    public View getOverflowMenuAnchor() {
        View view = getToolbar().findViewById(R.id.menu_overflow_button);
        return view == null ? getToolbar() : view;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        getFragment().handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (getFragment().onBackPressed()) {
            return;
        }
        finish();
    }

    @Override
    public void onDestroy() {
        unregisterBus();
        super.onDestroy();
    }

    private void registerBus() {
        bus = app.getBus();
        bus.register(busMethods);
        L.d("Registered bus.");
    }

    private void unregisterBus() {
        bus.unregister(busMethods);
        bus = null;
        L.d("Unregistered bus.");
    }

    private class EventBusMethods {

        @Subscribe
        public void onChangeTheme(ThemeChangeEvent event) {
            MainActivity.this.recreate();
        }
    }
}
