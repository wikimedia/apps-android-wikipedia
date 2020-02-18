package org.wikipedia.descriptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.wikipedia.R;
import org.wikipedia.onboarding.OnboardingFragment;
import org.wikipedia.onboarding.OnboardingPageView;

public class DescriptionEditTutorialFragment extends OnboardingFragment {
    @NonNull public static DescriptionEditTutorialFragment newInstance() {
        return new DescriptionEditTutorialFragment();
    }

    @Override
    protected FragmentStateAdapter getAdapter() {
        return new DescriptionEditTutorialPagerAdapter(this);
    }

    @Override
    protected int getDoneButtonText() {
        return R.string.description_edit_tutorial_button_label_start_editing;
    }

    class DescriptionEditTutorialPagerAdapter extends FragmentStateAdapter {
        DescriptionEditTutorialPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @Override public int getItemCount() {
            return DescriptionEditTutorialPage.size();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new ItemFragment(DescriptionEditTutorialPage.of(position).getLayout());
        }
    }

    public static class ItemFragment extends Fragment {
        private int layoutId;

        ItemFragment(int layoutId) {
            this.layoutId = layoutId;
        }

        @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            OnboardingPageView view = (OnboardingPageView) inflater.inflate(layoutId, container, false);
            view.setCallback(new OnboardingPageView.DefaultCallback());
            return view;
        }
    }
}
