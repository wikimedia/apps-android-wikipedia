package org.wikipedia.page;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.wikipedia.PageTitle;
import org.wikipedia.history.HistoryEntry;

/**
 * Fragment that displays a single Page (WebView plus ToC drawer).
 * This is an "empty shell" of a fragment that passes all the actual logic to an internal
 * PageViewFragmentInternal class, and here's why:
 *
 * When a fragment is replaced by another fragment, and placed onto the backstack, the old
 * fragment's View is destroyed, but the actual Fragment object remains in memory (so that
 * the fragment View may be rebuilt if the user goes "back" to it, without having to use
 * savedInstanceState).  The problem is that our PageViewFragment is a very heavy object
 * (in terms of actual bytecode), and that's what eats up a lot of memory when multiple
 * PageViewFragments are stacked on top of each other.
 *
 * So, I've separated all the "heavy" code into a PageViewFragmentInternal object, and made
 * an extremely light PageViewFragment object. The heavy object has all the same methods expected
 * from a Fragment, and becomes a puppet of the light object.  The crucial point comes when the
 * fragment's View is destroyed:  the light object sets its instance of the heavy object to null,
 * thus freeing all of that memory.
 *
 * The light fragment maintains a set of arguments, so that the heavy object can be recreated
 * if the user goes back to it.
 */
public class PageViewFragment extends Fragment {
    private static final String KEY_TITLE = "title";
    private static final String KEY_CURRENT_HISTORY_ENTRY = "currentHistoryEntry";
    private static final String KEY_SCROLL_Y = "scrollY";

    private PageViewFragmentInternal fragment;

    /**
     * Access the underlying PageViewFragmentInternal object, for interacting with its
     * WebView and page manipulation functions.
     * @return Underlying object responsible for rendering the page. May be null if this
     * fragment's View has been destroyed.
     */
    public PageViewFragmentInternal getFragment() {
        return fragment;
    }

    /**
     * Factory method for creating new instances of the fragment.
     * @param title Title of the page to be displayed.
     * @param entry HistoryEntry associated with this page.
     * @return New instance of this fragment.
     */
    public static PageViewFragment newInstance(PageTitle title, HistoryEntry entry) {
        PageViewFragment f = new PageViewFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_TITLE, title);
        args.putParcelable(KEY_CURRENT_HISTORY_ENTRY, entry);
        args.putInt(KEY_SCROLL_Y, 0);
        f.setArguments(args);
        return f;
    }

    public PageViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragment = new PageViewFragmentInternal(this,
                (PageTitle)getArguments().getParcelable(KEY_TITLE),
                (HistoryEntry)getArguments().getParcelable(KEY_CURRENT_HISTORY_ENTRY),
                getArguments().getInt(KEY_SCROLL_Y));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        if (fragment == null) {
            //this implies that we previously disposed of our internal fragment object, and
            //now need to recreate it, based on the state that we kept.
            fragment = new PageViewFragmentInternal(this,
                    (PageTitle)getArguments().getParcelable(KEY_TITLE),
                    (HistoryEntry)getArguments().getParcelable(KEY_CURRENT_HISTORY_ENTRY),
                    getArguments().getInt(KEY_SCROLL_Y));
        }
        return fragment.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        if (fragment != null) {
            // update our arguments, so that we'll be able to precisely recreate
            // the internal object based on them.
            getArguments().putInt(KEY_SCROLL_Y, fragment.getScrollY());

            // let the internal object do any cleanup of its own.
            fragment.onDestroyView();

            // the key to everything: dispose of the internal object, thus freeing a whole bunch of memory,
            // and keeping just a minimal amount of data on the back-stack.
            fragment = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (fragment == null) {
            return;
        }
        fragment.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (fragment == null) {
            return;
        }
        fragment.onActivityResult(requestCode, resultCode, data);
    }
}
