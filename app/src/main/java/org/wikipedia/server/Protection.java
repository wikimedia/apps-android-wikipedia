package org.wikipedia.server;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.wikipedia.util.log.L;

import java.lang.reflect.Type;

/** Protection settings for a page */
public class Protection {
    private String[] edit;

    public Protection() {
        this.edit = new String[]{};
    }

    public Protection(String[] edit) {
        this.edit = edit;
    }

    // TODO should send them all, but callers need to be updated, too, (future patch)
    @Nullable
    public String getFirstAllowedEditorRole() {
        if (edit.length > 0) {
            return edit[0];
        }
        return null;
    }

    /**
     * Need a custom Deserializer since the mediawiki API provides an inconsistent API.
     * Sometimes it returns an object, and other times when it's empty it returns an empty
     * array. See https://phabricator.wikimedia.org/T69054
     */
    public static class Deserializer implements JsonDeserializer<Protection> {
        /**
         * Gson invokes this call-back method during deserialization when it encounters a field
         * of the specified type.
         * <p>In the implementation of this call-back method, you should consider invoking
         * {@link JsonDeserializationContext#deserialize(JsonElement, Type)} method to create
         * objects for any non-trivial field of the returned object. However, you should never
         * invoke it on the the same type passing {@code json} since that will cause an infinite
         * loop (Gson will call your call-back method again).
         *
         * @param jsonEl The Json data being deserialized
         * @param typeOfT The type of the Object to deserialize to
         * @param jdc The deserialization context
         * @return a deserialized object of the specified type typeOfT which is
         * a subclass of {@code T}
         * @throws JsonParseException if json is not in the expected format of {@code typeofT}
         */
        @Override
        public Protection deserialize(JsonElement jsonEl, Type typeOfT,
                                      JsonDeserializationContext jdc)
                throws JsonParseException {
            if (jsonEl.isJsonArray()) {
                JsonArray array = jsonEl.getAsJsonArray();
                if (array.size() != 0) {
                    L.w("Unexpected array size " + array.toString());
                }
            } else {
                JsonElement editEl = jsonEl.getAsJsonObject().get("edit");
                if (editEl != null) {
                    JsonArray editorRolesJsonArray = editEl.getAsJsonArray();
                    String[] editorRoles = new String[editorRolesJsonArray.size()];
                    for (int i = 0; i < editorRolesJsonArray.size(); i++) {
                        editorRoles[i] = editorRolesJsonArray.get(i).getAsString();
                    }
                    return new Protection(editorRoles);
                }
            }
            return new Protection();
        }
    }
}