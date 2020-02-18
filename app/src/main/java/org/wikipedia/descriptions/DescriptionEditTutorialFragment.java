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
            return ItemFragment.newInstance(position);
        }
    }

    public static class ItemFragment extends Fragment {
        public static ItemFragment newInstance(int position) {
            ItemFragment instance = new ItemFragment();
            Bundle args = new Bundle();
            args.putInt("position", position);
            instance.setArguments(args);
            return instance;
        }

        @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            int position = getArguments().getInt("position", 0);
            OnboardingPageView view = (OnboardingPageView) inflater.inflate(DescriptionEditTutorialPage.of(position).getLayout(), container, false);
            view.setCallback(new OnboardingPageView.DefaultCallback());
            return view;
        }
    }
}
