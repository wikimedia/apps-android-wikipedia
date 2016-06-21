package org.wikipedia.feed.demo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.model.ListCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// todo: [Feed] remove.
public class IntegerListCard extends ListCard<IntegerListItemCard> {
    public IntegerListCard() {
        super(newItems());
    }

    @NonNull @Override public String title() {
        return "In the news";
    }

    @Nullable @Override public String subtitle() {
        return "Friday, April 08";
    }

    @Nullable @Override public String footer() {
        return "More news from Fri, April 08";
    }

    private static List<IntegerListItemCard> newItems() {
        final int minSize = 1;
        final int range = 4;
        final int size = minSize + new Random().nextInt(range);
        List<IntegerListItemCard> items = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            items.add(new IntegerListItemCard(i));
        }
        return items;
    }
}
