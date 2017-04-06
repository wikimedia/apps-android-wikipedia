package org.wikipedia.feed.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.wikipedia.feed.model.Card;
import org.wikipedia.test.theories.TestedOnBool;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static android.support.v7.widget.RecyclerView.AdapterDataObserver;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.wikipedia.feed.view.FeedAdapter.Callback;

public class ListCardViewTest extends ViewTest {
    private ListCardView<Card> subject;

    @Before public void setUp() {
        setUp(WIDTH_DP_S, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
        subject = new Subject(ctx());
    }

    @Theory public void testSetCallback(@TestedOnBool boolean nullHeader,
                                        @TestedOnBool boolean nullCallback) {
        CardHeaderView header = nullHeader ? null : mock(CardHeaderView.class);
        if (header != null) {
            subject.header(header);
        }

        Callback callback = nullCallback ? null : mock(FeedAdapter.Callback.class);
        subject.setCallback(callback);
        assertThat(subject.getCallback(), is(callback));
        if (header != null) {
            verify(header).setCallback(eq(callback));
        }
    }

    @Theory public void testSet(@TestedOnBool boolean nul) {
        Adapter<?> adapter = nul ? null : mock(Adapter.class);
        subject.set(adapter);
        //noinspection rawtypes
        assertThat(subject.recyclerView.getAdapter(), is((Adapter) adapter));
    }

    @Theory public void testUpdate(@TestedOnBool boolean nul) {
        Adapter<?> adapter = nul ? null : new NullAdapter();
        subject.set(adapter);

        AdapterDataObserver observer = mock(AdapterDataObserver.class);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }

        subject.update();

        if (adapter != null) {
            adapter.unregisterAdapterDataObserver(observer);
            verify(observer).onChanged();
        }
    }

    @Test public void testHeader() {
        View header = mock(View.class);
        subject.header(header);
        assertThat(subject.headerView, is(header));
    }

    @Test public void testLargeHeader() {
        View header = mock(View.class);
        subject.largeHeader(header);
        assertThat(subject.largeHeaderView, is(header));
    }

    public static class NullAdapter extends Adapter<RecyclerView.ViewHolder> {
        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        }

        @Override public int getItemCount() {
            return 0;
        }
    }

    private static class Subject extends ListCardView<Card> {
        Subject(Context context) {
            super(context);
        }
    }
}
