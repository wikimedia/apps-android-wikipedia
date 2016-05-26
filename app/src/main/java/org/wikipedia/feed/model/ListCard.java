package org.wikipedia.feed.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ListCard {
    // TODO: [Feed] fill in model data.

    private final List<Integer> items;

    public ListCard() {
        final int minSize = 1;
        final int range = 4;
        final int size = minSize + new Random().nextInt(range);
        @SuppressWarnings("checkstyle:hiddenfield") List<Integer> items = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            items.add(i);
        }
        this.items = Collections.unmodifiableList(items);
    }

    public List<Integer> items() {
        return items;
    }
}