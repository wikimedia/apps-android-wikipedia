package org.wikipedia.feed.searchbar;

import android.content.Context;
import android.view.LayoutInflater;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewSearchBarBinding;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;

public class SearchCardView extends DefaultFeedCardView<SearchCard> {
    public interface Callback {
        void onSearchRequested();
        void onVoiceSearchRequested();
    }

    public SearchCardView(Context context) {
        super(context);

        final ViewSearchBarBinding binding = ViewSearchBarBinding.inflate(LayoutInflater.from(context));

        binding.searchContainer.setOnClickListener(v -> {
            if (getCallback() != null) {
                getCallback().onSearchRequested();
            }
        });
        binding.voiceSearchButton.setOnClickListener(v -> {
            if (getCallback() != null) {
                getCallback().onVoiceSearchRequested();
            }
        });

        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.searchItemBackground));
        FeedbackUtil.setToolbarButtonLongPressToast(findViewById(R.id.voice_search_button));
    }
}
