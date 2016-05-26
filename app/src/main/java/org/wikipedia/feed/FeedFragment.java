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
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.feed.view.FeedView;

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

    @NonNull private final List<ListCard> cards = new ArrayList<>();

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

    @Override public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override @Nullable public Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    // TODO: [Feed] remove.
    @OnClick(R.id.fragment_feed_add_card) void addCard() {
        cards.add(new ListCard());
        feedView.update();
    }
}
