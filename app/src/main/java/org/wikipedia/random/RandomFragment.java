package org.wikipedia.random;

import android.content.DialogInterface;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.RandomizerFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class RandomFragment extends Fragment {
    @BindView(R.id.random_item_pager) ViewPager randomPager;
    @BindView(R.id.random_next_button) FloatingActionButton nextButton;
    @BindView(R.id.random_save_button) ImageView saveButton;
    @BindView(R.id.random_back_button) View backButton;
    private Unbinder unbinder;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private boolean saveButtonState;
    private ViewPagerListener viewPagerListener = new ViewPagerListener();
    @Nullable private RandomizerFunnel funnel;

    @NonNull
    public static RandomFragment newInstance() {
        return new RandomFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_random, container, false);
        unbinder = ButterKnife.bind(this, view);
        FeedbackUtil.setToolbarButtonLongPressToast(nextButton, saveButton);

        randomPager.setOffscreenPageLimit(2);
        randomPager.setAdapter(new RandomItemAdapter((AppCompatActivity) getActivity()));
        randomPager.setPageTransformer(true, new RandomPagerTransformer());
        randomPager.addOnPageChangeListener(viewPagerListener);

        updateSaveShareButton();
        updateBackButton(0);
        if (savedInstanceState != null && randomPager.getCurrentItem() == 0 && getTopTitle() != null) {
            updateSaveShareButton(getTopTitle());
        }

        funnel = new RandomizerFunnel(WikipediaApp.getInstance(), WikipediaApp.getInstance().getWikiSite(),
                getActivity().getIntent().getIntExtra(RandomActivity.INVOKE_SOURCE_EXTRA, 0));
        return view;
    }

    @Override
    public void onDestroyView() {
        randomPager.removeOnPageChangeListener(viewPagerListener);
        unbinder.unbind();
        unbinder = null;
        if (funnel != null) {
            funnel.done();
            funnel = null;
        }
        super.onDestroyView();
    }

    @OnClick(R.id.random_next_button) void onNextClick() {
        if (nextButton.getDrawable() instanceof Animatable) {
            ((Animatable) nextButton.getDrawable()).start();
        }
        viewPagerListener.setNextPageSelectedAutomatic();
        randomPager.setCurrentItem(randomPager.getCurrentItem() + 1, true);
        if (funnel != null) {
            funnel.clickedForward();
        }
    }

    @OnClick(R.id.random_back_button) void onBackClick() {
        viewPagerListener.setNextPageSelectedAutomatic();
        if (randomPager.getCurrentItem() > 0) {
            randomPager.setCurrentItem(randomPager.getCurrentItem() - 1, true);
            if (funnel != null) {
                funnel.clickedBack();
            }
        }
    }

    @OnClick(R.id.random_save_button) void onSaveShareClick() {
        PageTitle title = getTopTitle();
        if (title == null) {
            return;
        }
        if (saveButtonState) {
            new ReadingListBookmarkMenu(saveButton, new ReadingListBookmarkMenu.Callback() {
                @Override
                public void onAddRequest(@Nullable ReadingListPage page) {
                    onAddPageToList(title);
                }

                @Override
                public void onDeleted(@Nullable ReadingListPage page) {
                    FeedbackUtil.showMessage(getActivity(),
                            getString(R.string.reading_list_item_deleted, title.getDisplayText()));
                    updateSaveShareButton(title);
                }
            }).show(title);
        } else {
            onAddPageToList(title);
        }
    }

    public void onSelectPage(@NonNull PageTitle title) {
        startActivity(PageActivity.newIntentForNewTab(getActivity(),
                new HistoryEntry(title, HistoryEntry.SOURCE_RANDOM), title));
    }

    public void onAddPageToList(@NonNull PageTitle title) {
        bottomSheetPresenter.show(getChildFragmentManager(),
                AddToReadingListDialog.newInstance(title,
                        AddToReadingListDialog.InvokeSource.RANDOM_ACTIVITY,
                        (DialogInterface dialogInterface) -> updateSaveShareButton(title)));
    }

    @SuppressWarnings("magicnumber")
    private void updateBackButton(int pagerPosition) {
        backButton.setClickable(pagerPosition != 0);
        backButton.setAlpha(pagerPosition == 0 ? 0.5f : 1f);
    }

    private void updateSaveShareButton(@NonNull PageTitle title) {
        CallbackTask.execute(() -> ReadingListDbHelper.instance().findPageInAnyList(title), new CallbackTask.DefaultCallback<ReadingListPage>() {
            @Override
            public void success(ReadingListPage page) {
                saveButtonState = page != null;
                saveButton.setImageResource(saveButtonState
                        ? R.drawable.ic_bookmark_white_24dp : R.drawable.ic_bookmark_border_white_24dp);
            }
        });
    }

    @SuppressWarnings("magicnumber")
    public void updateSaveShareButton() {
        RandomItemFragment f = getTopChild();
        boolean enable = f != null && f.isLoadComplete();
        saveButton.setClickable(enable);
        saveButton.setAlpha(enable ? 1f : 0.5f);
    }

    public void onChildLoaded() {
        updateSaveShareButton();
    }

    @Nullable private PageTitle getTopTitle() {
        RandomItemFragment f = getTopChild();
        return f == null ? null : f.getTitle();
    }

    @Nullable private RandomItemFragment getTopChild() {
        FragmentManager fm = getFragmentManager();
        for (Fragment f : fm.getFragments()) {
            if (f instanceof RandomItemFragment
                    && ((RandomItemFragment) f).getPagerPosition() == randomPager.getCurrentItem()) {
                return ((RandomItemFragment) f);
            }
        }
        return null;
    }

    private class RandomItemAdapter extends FragmentPagerAdapter{

        RandomItemAdapter(AppCompatActivity activity) {
            super(activity.getSupportFragmentManager());
        }

        @Override
        public int getCount() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Fragment getItem(int position) {
            RandomItemFragment f = RandomItemFragment.newInstance();
            f.setPagerPosition(position);
            return f;
        }
    }

    private class RandomPagerTransformer implements ViewPager.PageTransformer {
        @SuppressWarnings("magicnumber")
        @Override
        public void transformPage(View view, float position) {
            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setRotation(0f);
                view.setTranslationX(0);
            } else if (position <= 0) { // [-1,0]
                float factor = position * 45f;
                view.setRotation(factor);
                view.setTranslationX((view.getWidth() * position / 2));
                view.setAlpha(1f);
            } else if (position <= 1) { // (0,1]
                // keep it in place (undo the default translation)
                view.setTranslationX(-(view.getWidth() * position));
                // but move it slightly down
                view.setTranslationY(DimenUtil.roundedDpToPx(12f) * position);
                // and make it translucent
                view.setAlpha(1f - position * 0.5f);
                view.setRotation(0f);
            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setRotation(0f);
                view.setTranslationX(0);
            }
        }
    }

    private class ViewPagerListener implements ViewPager.OnPageChangeListener {
        private int prevPosition;
        private boolean nextPageSelectedAutomatic;

        void setNextPageSelectedAutomatic() {
            nextPageSelectedAutomatic = true;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            updateBackButton(position);
            PageTitle title = getTopTitle();
            if (title != null) {
                updateSaveShareButton(title);
            }
            if (!nextPageSelectedAutomatic && funnel != null) {
                if (position > prevPosition) {
                    funnel.swipedForward();
                } else if (position < prevPosition) {
                    funnel.swipedBack();
                }
            }
            nextPageSelectedAutomatic = false;
            prevPosition = position;
            updateSaveShareButton();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}
