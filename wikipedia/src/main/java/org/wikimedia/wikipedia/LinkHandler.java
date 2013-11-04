package org.wikimedia.wikipedia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.squareup.otto.Bus;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles any html links coming from a {@link PageViewFragment}
 */
public class LinkHandler implements CommunicationBridge.JSEventListener {
    private final Context context;
    private final CommunicationBridge bridge;
    private final Bus bus;

    public static class NewWikiPageNavigationEvent {
        private final PageTitle title;

        public NewWikiPageNavigationEvent(PageTitle title) {
            this.title = title;
        }

        public PageTitle getTitle() {
            return title;
        }
    }

    public LinkHandler(Context context, CommunicationBridge bridge) {
        this.context = context;
        this.bridge = bridge;
        this.bus = ((WikipediaApp)context.getApplicationContext()).getBus();

        this.bridge.addListener("linkClicked", this);
    }

    private void handleExternalLink(String href) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(href));
        context.startActivity(intent);
    }

    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            String href = messagePayload.getString("href");
            if (href.startsWith("//")) {
                // That's a protocol specific link! Make it https!
                href = "https:" + href;
            }
            Log.d("Wikipedia", "Link clicked was " + href);
            if (href.startsWith("/wiki/")) {
                // TODO: Handle fragments
                String pageName = href.replace("/wiki/", "");
                bus.post(new NewWikiPageNavigationEvent(new PageTitle(null, pageName)));
            } else {
                // Assume everything else is an external link... for now!
                handleExternalLink(href);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
