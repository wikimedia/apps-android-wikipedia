package org.wikipedia.feed.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.feed.model.Card;
import org.wikipedia.test.ViewTest;
import org.wikipedia.theme.Theme;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.wikipedia.feed.view.FeedAdapter.Callback;
import static org.wikipedia.test.ViewTest.LayoutDirection.LOCALE;

public class ListCardViewTest extends ViewTest {
    private ListCardView<Card> subject;

    @Before public void setUp() {
        setUp(WIDTH_DP_S, LOCALE, FONT_SCALES[0], Theme.LIGHT);
        subject = new Subject(ctx());
    }

    @Theory public void testSetCallback(@TestedOn(ints = {0, 1}) int nonnullHeader,
                                        @TestedOn(ints = {0, 1}) int nonnullCallback) {
        CardHeaderView header = nonnullHeader == 0 ? null : mock(CardHeaderView.class);
        if (header != null) {
            subject.header(header);
        }

        Callback callback = nonnullCallback == 0 ? null : mock(FeedAdapter.Callback.class);
        subject.setCallback(callback);
        assertThat(subject.getCallback(), is(callback));
        if (header != null) {
            verify(header).setCallback(eq(callback));
        }
    }

    @Theory public void testSet(@TestedOn(ints = {0, 1}) int nonnull) {
        Adapter<?> adapter = nonnull == 0 ? null : mock(Adapter.class);
        subject.set(adapter);
        //noinspection rawtypes
        assertThat(subject.recyclerView.getAdapter(), is((Adapter) adapter));
    }

    @Theory public void testUpdate(@TestedOn(ints = {0, 1}) int nonnull) {
        Adapter<?> adapter = nonnull == 0 ? null : spy(new NullAdapter());
        subject.set(adapter);
        subject.update();
        if (adapter != null) {
            verify(adapter).notifyDataSetChanged();
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