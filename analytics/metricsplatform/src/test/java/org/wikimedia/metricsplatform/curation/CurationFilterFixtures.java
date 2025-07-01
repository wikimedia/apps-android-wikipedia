package org.wikimedia.metricsplatform.curation;

import org.wikimedia.metricsplatform.json.GsonHelper;
import org.wikimedia.metricsplatform.config.CurationFilter;

import com.google.gson.Gson;

public final class CurationFilterFixtures {
    private CurationFilterFixtures() {
        // Utility class, should never be instantiated
    }

    public static CurationFilter curationFilter() {
        Gson gson = GsonHelper.getGson();
        String curationFilterJson = "{\"page_id\":{\"less_than\":500,\"not_equals\":42},\"page_namespace_text\":" +
            "{\"equals\":\"Talk\"},\"user_is_logged_in\":{\"equals\":true},\"user_edit_count_bucket\":" +
            "{\"in\":[\"100-999 edits\",\"1000+ edits\"]},\"user_groups\":{\"contains_all\":" +
            "[\"user\",\"autoconfirmed\"],\"does_not_contain\":\"sysop\"}}";
        return gson.fromJson(curationFilterJson, CurationFilter.class);
    }
}
