package org.wikipedia.descriptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.databinding.FragmentDescriptionEditSuccessBinding;

public class DescriptionEditSuccessFragment extends Fragment implements DescriptionEditSuccessView.Callback {
    private FragmentDescriptionEditSuccessBinding binding;
    private DescriptionEditSuccessView successView;

    public interface Callback {
        void onDismissClick();
    }

    @NonNull public static DescriptionEditSuccessFragment newInstance() {
        return new DescriptionEditSuccessFragment();
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentDescriptionEditSuccessBinding.inflate(inflater, container, false);
        successView = binding.fragmentDescriptionEditSuccessView;

        successView.setCallback(this);
        return binding.getRoot();
    }

    @Override public void onDismissClick() {
        if (callback() != null) {
            callback().onDismissClick();
        }
    }

    @Override public void onDestroyView() {
        successView.setCallback(null);
        binding = null;
        super.onDestroyView();
    }

    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
