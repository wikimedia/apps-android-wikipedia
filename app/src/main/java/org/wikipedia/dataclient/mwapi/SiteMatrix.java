package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import org.wikipedia.json.GsonUtil;

import java.util.ArrayList;
import java.util.List;

public class SiteMatrix extends MwResponse {
    @SuppressWarnings("unused,NullableProblems") @NonNull private JsonObject sitematrix;

    public JsonObject siteMatrix() {
        return sitematrix;
    }

    @SuppressWarnings("unused,NullableProblems")
    public class SiteInfo {
        @NonNull
        private String code;
        @NonNull
        private String name;
        @NonNull
        private String localname;

        @NonNull
        public String code() {
            return code;
        }

        @NonNull
        public String name() {
            return name;
        }

        @NonNull
        public String localName() {
            return localname;
        }
    }

    public static List<SiteInfo> getSites(@NonNull SiteMatrix siteMatrix) {
        List<SiteInfo> sites = new ArrayList<>();
        // We have to parse the Json manually because the list of SiteInfo objects
        // contains a "count" member that prevents it from being able to deserialize
        // as a list automatically.
        for (String key : siteMatrix.siteMatrix().keySet()) {
            if (key.equals("count")) {
                continue;
            }
            SiteInfo info = GsonUtil.getDefaultGson().fromJson(siteMatrix.siteMatrix().get(key), SiteInfo.class);
            if (info != null) {
                sites.add(info);
            }
        }
        return sites;
    }
}
