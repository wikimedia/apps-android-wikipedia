package org.wikipedia.theme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.databinding.FragmentThemeFittingRoomBinding;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.WebViewInvalidateEvent;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class ThemeFittingRoomFragment extends Fragment {
    private FragmentThemeFittingRoomBinding binding;
    private TextView testTitle;
    private TextView testText;

    private CompositeDisposable disposables = new CompositeDisposable();

    @NonNull public static ThemeFittingRoomFragment newInstance() {
        return new ThemeFittingRoomFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentThemeFittingRoomBinding.inflate(inflater, container, false);

        testText = binding.themeTestText;
        testTitle = binding.themeTestTitle;

        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));

        binding.themeTestImage.loadImage(R.drawable.w_nav_mark);
        updateTextSize();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        binding = null;
        super.onDestroyView();
    }

    private void updateTextSize() {
        final float titleMultiplier = 1.6f;
        float fontSize = WikipediaApp.getInstance().getFontSize(requireActivity().getWindow());
        testText.setTextSize(fontSize);
        testTitle.setTextSize(fontSize * titleMultiplier);
    }

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof ChangeTextSizeEvent) {
                updateTextSize();
                testText.post(() -> WikipediaApp.getInstance().getBus().post(new WebViewInvalidateEvent()));
            }
        }
    }
}
