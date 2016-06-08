package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.bottomcontent.MainPageReadMoreTopicTask;
import org.wikipedia.util.log.L;

import java.io.IOException;

public class BecauseYouReadTopicTask extends MainPageReadMoreTopicTask {
    public interface Callback {
        void success(@NonNull final PageTitle title, @NonNull final MwQueryResponse<BecauseYouReadClient.Pages> pages);
    }
    @NonNull private final Callback cb;

    public BecauseYouReadTopicTask(@NonNull final Context context, @NonNull final Callback cb) {
        super(context);
        this.cb = cb;
    }

    @Override
    public void onFinish(@NonNull final PageTitle title) {
        try {
            new BecauseYouReadClient(title.getSite()).get(title.getText(),
                    new BecauseYouReadClient.BecauseYouReadCallback() {
                @Override
                public void success(MwQueryResponse<BecauseYouReadClient.Pages> pages) {
                    cb.success(title, pages);
                }

                @Override
                public void failure(Throwable t) {
                    L.w("Error fetching 'because you read' suggestions", t);
                }
            });
        } catch (IOException e) {
            L.w("Error fetching 'because you read' suggestions", e);
        }
    }

    @Override
    public void onCatch(Throwable caught) {
        L.w("Error getting history entry for 'because you read' card", caught);
    }
}
