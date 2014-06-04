package org.wikipedia.page;

import android.support.v4.app.FixedFragmentStatePagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;
import org.wikipedia.R;

import java.util.List;

public class PageFragmentAdapter extends FixedFragmentStatePagerAdapter {
    private SparseArray<PageViewFragment> fragmentArray;
    private BackStack backStack;

    public PageFragmentAdapter(FragmentManager fm, BackStack backStack) {
        super(fm);
        this.backStack = backStack;
        fragmentArray = new SparseArray<PageViewFragment>();
    }

    @Override
    public Fragment getItem(int position) {
        PageViewFragment f = fragmentArray.get(position);
        if (f == null) {
            f = new PageViewFragment(position,
                    backStack.getStack().get(position).title,
                    backStack.getStack().get(position).historyEntry,
                    R.id.search_fragment);

            fragmentArray.put(position, f);
        }
        return f;
    }

    public PageViewFragment getFragmentAt(int position) {
        return fragmentArray.get(position);
    }

    @Override
    public int getCount() {
        return backStack.size();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return fragmentArray.get(position).getTitle().getDisplayText();
    }

    /**
     * Remove the specified fragment without saving its state.
     * @param position Position at which to remove the fragment.
     */
    public void removeFragment(int position) {
        PageViewFragment fragment = fragmentArray.get(position);
        fragment.setSaveState(PageViewFragment.SAVE_STATE_NONE);
        fragmentArray.delete(position);
        backStack.getStack().remove(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        PageViewFragment fragment = fragmentArray.get(position);
        if (fragment != null) {
            fragment.setSaveState(PageViewFragment.SAVE_STATE_TITLE);
        }
        super.destroyItem(container, position, object);
    }

    @Override
    public int getItemPosition(Object item) {
        if (fragmentArray.indexOfValue((PageViewFragment) item) == -1) {
            return FragmentStatePagerAdapter.POSITION_NONE;
        }
        return FragmentStatePagerAdapter.POSITION_UNCHANGED;
    }

    /**
     * Rebuilds this adapter's inner catalog of Fragments, useful for when
     * the activity is restored from a saved state.
     * @param activity Calling activity.
     */
    public void onResume(PageActivity activity) {
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            if (f instanceof PageViewFragment) {
                fragmentArray.put(((PageViewFragment) f).getPagerIndex(), (PageViewFragment)f);
            }
        }
    }
}
