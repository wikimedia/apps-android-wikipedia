package org.wikipedia.feed;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedView;
import org.wikipedia.settings.Prefs;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class FeedFragment extends Fragment implements CallbackFragment<CallbackFragment.Callback> {
    @BindView(R.id.fragment_feed_feed) FeedView feedView;
    @BindView(R.id.feed_collapsing_toolbar_layout) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.feed_toolbar) Toolbar toolbar;
    private Unbinder unbinder;
    private WikipediaApp app;
    private FeedCoordinator coordinator;

    public interface Callback extends CallbackFragment.Callback {
        void onAddToolbar(Toolbar toolbar);
        void onRemoveToolbar(Toolbar toolbar);
    }

    public static FeedFragment newInstance() {
        return new FeedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        coordinator = new FeedCoordinator(getContext());
        Prefs.pageLastShown(0);
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        unbinder = ButterKnife.bind(this, view);
        feedView.set(coordinator.getCards());

        if (getCallback() != null) {
            getCallback().onAddToolbar(toolbar);
        }

        coordinator.setFeedUpdateListener(new FeedCoordinator.FeedUpdateListener() {
            @Override
            public void update(List<Card> cards) {
                if (isAdded()) {
                    feedView.update();
                }
            }
        });

        coordinator.more(app.getSite());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override public void onDestroyView() {
        if (getCallback() != null) {
            getCallback().onRemoveToolbar(toolbar);
        }
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_feed, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_feed_search:

                // TODO: remove
                coordinator.more(app.getSite());

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override @Nullable public Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
