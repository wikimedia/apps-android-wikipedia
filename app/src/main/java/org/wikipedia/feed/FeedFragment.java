package org.wikipedia.feed;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.feed.continuereading.ContinueReadingCard;
import org.wikipedia.feed.continuereading.ContinueReadingClient;
import org.wikipedia.feed.continuereading.ContinueReadingCoordinator;
import org.wikipedia.feed.continuereading.LastPageReadTask;
import org.wikipedia.feed.demo.IntegerListCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedView;
import org.wikipedia.history.HistoryEntry;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class FeedFragment extends Fragment
        implements CallbackFragment<CallbackFragment.Callback> {
    @BindView(R.id.fragment_feed_feed) FeedView feedView;
    private Unbinder unbinder;

    @NonNull private final List<Card> cards = new ArrayList<>();
    @NonNull private final ContinueReadingClient client = new ContinueReadingClient();
    @NonNull private final ContinueReadingCoordinator continueReadingCoordinator = new ContinueReadingCoordinator();

    public static FeedFragment newInstance() {
        return new FeedFragment();
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        unbinder = ButterKnife.bind(this, view);
        feedView.set(cards);

        return view;
    }

    @Override public void onResume() {
        super.onResume();
        updateContinueReading();
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
        cards.add(new IntegerListCard());
        feedView.update();
    }

    private void updateContinueReading() {
        client.request(getActivity(), new LastPageReadTask.Callback() {
            @Override public void success(@NonNull HistoryEntry entry) {
                ContinueReadingCard current = continueReadingCoordinator.card();
                continueReadingCoordinator.update(entry, client.lastDismissedTitle());
                ContinueReadingCard next = continueReadingCoordinator.card();

                cards.remove(current);
                if (next != null) {
                    cards.add(next);
                    feedView.update();
                }
            }
        });
    }
}