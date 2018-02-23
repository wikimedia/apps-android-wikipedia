package org.wikipedia.feed.mostread;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.feed.mostread.MostReadArticlesActivity.MOST_READ_CARD;

public class MostReadFragment extends Fragment {

    @BindView(R.id.view_most_read_fullscreen_link_card_list) RecyclerView mostReadLinks;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private Unbinder unbinder;

    @NonNull
    public static MostReadFragment newInstance(@NonNull MostReadItemCard card) {
        MostReadFragment instance = new MostReadFragment();
        Bundle args = new Bundle();
        args.putString(MOST_READ_CARD, GsonMarshaller.marshal(card));
        instance.setArguments(args);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_most_read, container, false);
        unbinder = ButterKnife.bind(this, view);
        MostReadListCard card = GsonUnmarshaller.unmarshal(MostReadListCard.class, getActivity().getIntent().getStringExtra(MOST_READ_CARD));

        getAppCompatActivity().getSupportActionBar().setTitle(String.format(getString(R.string.top_on_this_day), card.subtitle()));

        initRecycler();
        mostReadLinks.setAdapter(new RecyclerAdapter(card.items(), new Callback()));
        return view;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private void initRecycler() {
        mostReadLinks.setLayoutManager(new LinearLayoutManager(getContext()));
        mostReadLinks.addItemDecoration(new DrawableItemDecoration(getContext(), R.attr.list_separator_drawable));
        mostReadLinks.setNestedScrollingEnabled(false);
    }

    private static class RecyclerAdapter extends DefaultRecyclerAdapter<MostReadItemCard, ListCardItemView> {
        @Nullable
        private Callback callback;

        RecyclerAdapter(@NonNull List<MostReadItemCard> items, @NonNull Callback callback) {
            super(items);
            this.callback = callback;
        }

        @Override
        public DefaultViewHolder<ListCardItemView> onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DefaultViewHolder<>(new ListCardItemView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder, int position) {
            MostReadItemCard card = item(position);
            holder.getView().setCard(card)
                    .setHistoryEntry(new HistoryEntry(card.pageTitle(),
                            HistoryEntry.SOURCE_FEED_MOST_READ_ACTIVITY)).setCallback(callback);

        }
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private class Callback implements ListCardItemView.Callback {
        @Override
        public void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry) {
            startActivity(PageActivity.newIntentForNewTab(getContext(), entry, entry.getTitle()));
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry) {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    AddToReadingListDialog.newInstance(entry.getTitle(),
                            AddToReadingListDialog.InvokeSource.MOST_READ_ACTIVITY));
        }

        @Override
        public void onRemovePageFromList(@NonNull HistoryEntry entry) {
            // TODO
        }

        @Override
        public void onSharePage(@NonNull HistoryEntry entry) {
            ShareUtil.shareText(getActivity(), entry.getTitle());
        }
    }
}
