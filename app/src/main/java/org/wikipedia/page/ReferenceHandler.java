package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles any reference links coming from a {@link PageFragment}
 */
public abstract class ReferenceHandler implements CommunicationBridge.JSEventListener {

    /**
     * Called when a reference link was clicked.
     */
    protected abstract void onReferenceClicked(int selectedIndex, @NonNull List<Reference> adjacentReferences);

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            int selectedIndex = messagePayload.getInt("selectedIndex");
            JSONArray referencesGroup = messagePayload.getJSONArray("referencesGroup");
            List<Reference> adjacentReferencesList = new ArrayList<>();
            for (int i = 0; i < referencesGroup.length(); i++) {
                JSONObject reference = (JSONObject) referencesGroup.get(i);
                adjacentReferencesList.add(new Reference(StringUtils.defaultString(reference.optString("text")),
                        StringUtils.defaultString(reference.optString("html"))));
            }

            onReferenceClicked(selectedIndex, adjacentReferencesList);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public class Reference {
        private String linkText;
        private String linkHtml;

        Reference(@NonNull String linkText, @NonNull String href) {
            this.linkText = linkText;
            this.linkHtml = href;
        }

        @NonNull public String getLinkText() {
            return linkText;
        }

        @NonNull public String getLinkHtml() {
            return linkHtml;
        }
    }
}
