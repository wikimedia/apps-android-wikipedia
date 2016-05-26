package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.MarginItemDecoration;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ListCardView extends CardView {
    @BindView(R.id.view_list_card_list) RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;

    public ListCardView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_list_card, this);
        ButterKnife.bind(this);
        initRecycler();
    }

    public void set(@NonNull ListCard card) {
        recyclerAdapter = new RecyclerAdapter(card.items());
        recyclerView.setAdapter(recyclerAdapter);
    }

    public void update() {
        recyclerAdapter.notifyDataSetChanged();
    }

    private void initRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_list_card_item_margin));
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(),
                R.drawable.divider));
        recyclerAdapter = new RecyclerAdapter(Collections.<Integer>emptyList());
        recyclerView.setAdapter(recyclerAdapter);
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<DefaultViewHolder<ListCardItemView>> {
        @NonNull private final List<Integer> items;

        RecyclerAdapter(@NonNull List<Integer> items) {
            this.items = items;
        }

        @Override public DefaultViewHolder<ListCardItemView> onCreateViewHolder(ViewGroup parent,
                                                                                int viewType) {
            return new DefaultViewHolder<>(new ListCardItemView(getContext()));
        }

        @Override public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder,
                                               int position) {
            // TODO: [Feed] replace.
            holder.getView().setTitle(items.get(position) + " Fine-tuned Universe");
            holder.getView().setSubtitle("Proposition");
        }

        @Override public int getItemCount() {
            return items.size();
        }
    }
}