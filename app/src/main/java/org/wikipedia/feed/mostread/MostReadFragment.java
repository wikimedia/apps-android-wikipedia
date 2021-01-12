package org.wikipedia.feed.mostread;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.MoveToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBehaviorsUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.TabUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.Constants.InvokeSource.MOST_READ_ACTIVITY;
import static org.wikipedia.feed.mostread.MostReadArticlesActivity.MOST_READ_CARD;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_most_read, container, false);
        unbinder = ButterKnife.bind(this, view);
        MostReadListCard card = GsonUnmarshaller.unmarshal(MostReadListCard.class, requireActivity().getIntent().getStringExtra(MOST_READ_CARD));

        getAppCompatActivity().getSupportActionBar().setTitle(String.format(getString(R.string.top_read_activity_title), card.subtitle()));
        setConditionalLayoutDirection(view, card.wikiSite().languageCode());

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
        mostReadLinks.addItemDecoration(new DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable));
        mostReadLinks.setNestedScrollingEnabled(false);
    }

    private static class RecyclerAdapter extends DefaultRecyclerAdapter<MostReadItemCard, ListCardItemView> {
        @Nullable
        private Callback callback;

        RecyclerAdapter(@NonNull List<MostReadItemCard> items, @NonNull Callback callback) {
            super(items);
            this.callback = callback;
        }

        @NonNull
        @Override
        public DefaultViewHolder<ListCardItemView> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DefaultViewHolder<>(new ListCardItemView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder<ListCardItemView> holder, int position) {
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
        public void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry, boolean openInNewBackgroundTab) {
            if (openInNewBackgroundTab) {
                TabUtil.openInNewBackgroundTab(entry);
                FeedbackUtil.showMessage(requireActivity(), R.string.article_opened_in_background_tab);
            } else {
                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()));
            }
        }

        @Override
        public void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry, @NonNull Pair<View, String>[] sharedElements) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), sharedElements);
            Intent intent = PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle());
            if (sharedElements.length > 0) {
                intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true);
            }
            startActivity(intent, DimenUtil.isLandscape(requireContext()) || sharedElements.length == 0 ? null : options.toBundle());
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry, boolean addToDefault) {
            if (addToDefault) {
                ReadingListBehaviorsUtil.INSTANCE.addToDefaultList(requireActivity(), entry.getTitle(), MOST_READ_ACTIVITY,
                        readingListId -> onMovePageToList(readingListId, entry));
            } else {
                bottomSheetPresenter.show(getChildFragmentManager(),
                        AddToReadingListDialog.newInstance(entry.getTitle(), MOST_READ_ACTIVITY));
            }
        }

        @Override
        public void onMovePageToList(long sourceReadingListId, @NonNull HistoryEntry entry) {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    MoveToReadingListDialog.newInstance(sourceReadingListId, entry.getTitle(), MOST_READ_ACTIVITY));
        }
    }
}
