package org.wikipedia.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;

/**
 * Handles any reference links coming from a {@link PageFragment}
 */
public abstract class ReferenceHandler implements CommunicationBridge.JSEventListener {

    /**
     * Called when a reference link was clicked.
     */
    protected abstract void onReferenceClicked(@NonNull String refHtml, @Nullable String refLinkText);

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            onReferenceClicked(messagePayload.getString("ref"), messagePayload.optString("linkText"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
