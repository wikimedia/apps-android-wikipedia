package org.wikipedia.feed;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class FeedFragment extends Fragment implements CallbackFragment<CallbackFragment.Callback> {
    @BindView(R.id.fragment_feed_feed) FeedView feedView;
    private Unbinder unbinder;
    private WikipediaApp app;
    private FeedCoordinator coordinator;

    public static FeedFragment newInstance() {
        return new FeedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        coordinator = new FeedCoordinator(getContext());
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        unbinder = ButterKnife.bind(this, view);
        feedView.set(coordinator.getCards());

        coordinator.setFeedUpdateListener(new FeedCoordinator.FeedUpdateListener() {
            @Override
            public void update(List<Card> cards) {
                feedView.update();
            }
        });

        coordinator.more(app.getSite());

        return view;
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override @Nullable public Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    // TODO: [Feed] remove.
    @OnClick(R.id.fragment_feed_add_card) void addCard() {
        coordinator.more(app.getSite());
        feedView.update();
    }

}
