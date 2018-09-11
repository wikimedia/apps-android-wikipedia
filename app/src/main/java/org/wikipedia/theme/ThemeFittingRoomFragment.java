package org.wikipedia.theme;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.WebViewInvalidateEvent;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class ThemeFittingRoomFragment extends Fragment {
    @BindView(R.id.theme_test_image) FaceAndColorDetectImageView testImage;
    @BindView(R.id.theme_test_title) TextView testTitle;
    @BindView(R.id.theme_test_text) TextView testText;
    private Unbinder unbinder;
    private CompositeDisposable disposables = new CompositeDisposable();

    @NonNull public static ThemeFittingRoomFragment newInstance() {
        return new ThemeFittingRoomFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_theme_fitting_room, container, false);
        unbinder = ButterKnife.bind(this, view);

        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));

        testImage.loadImage(R.drawable.w_nav_mark);
        updateTextSize();
        return view;
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        unbinder.unbind();
        unbinder = null;
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
        public void accept(Object event) throws Exception {
            if (event instanceof ChangeTextSizeEvent) {
                updateTextSize();
                testText.post(() -> WikipediaApp.getInstance().getBus().post(new WebViewInvalidateEvent()));
            }
        }
    }
}
