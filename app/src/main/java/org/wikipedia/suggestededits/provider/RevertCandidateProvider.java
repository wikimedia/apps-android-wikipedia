package org.wikipedia.suggestededits.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.json.GsonUtil;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class RevertCandidateProvider {

    // A line with the same content in both revisions, included to provide context when viewing the diff. The API returns up to two context lines around each change.
    public static final int DIFF_TYPE_LINE_WITH_SAME_CONTENT = 0;

    // A line included in the to revision but not in the from revision.
    public static final int DIFF_TYPE_LINE_ADDED = 1;

    // A line included in the from revision but not in the to revision.
    public static final int DIFF_TYPE_LINE_REMOVED = 2;

    // A line containing text that differs between the two revisions. (For changes to paragraph location as well as content, see type 5.)
    public static final int DIFF_TYPE_LINE_WITH_DIFF = 3;

    // When a paragraph's location differs between the two revisions, a type 4 object represents the location in the from revision.
    public static final int DIFF_TYPE_PARAGRAPH_MOVED_FROM = 4;

    // When a paragraph's location differs between the two revisions, a type 5 object represents the location in the to revision. This type can also include word-level differences between the two revisions.
    public static final int DIFF_TYPE_PARAGRAPH_MOVED_TO = 5;

    public static final int HIGHLIGHT_TYPE_ADD = 0;
    public static final int HIGHLIGHT_TYPE_DELETE = 1;


    public static class RevertCandidate {
        private long id;
        private long timestamp;
        private boolean nonbot;
        private String comment;
        private String wikiRevId;
        private String title;
        private String wiki;
        private String user;

        private Revision revision;
        private JsonElement ores;

        public String getTitle() {
            return StringUtils.defaultString(title);
        }

        public String getComment() {
            return StringUtils.defaultString(comment);
        }

        public long getRevFrom() {
            return revision.oldRev;
        }

        public long getRevTo() {
            return revision.newRev;
        }

        @Nullable
        public OresResult getOres() {
            if (ores != null && !(ores instanceof JsonArray)) {
                return GsonUtil.getDefaultGson().fromJson(ores, OresResult.class);
            }
            return null;
        }
    }

    public static class Revision {
        @SerializedName("old") private long oldRev;
        @SerializedName("new") private long newRev;
    }

    public static class OresResult {
        private OresItem damaging;
        private OresItem goodfaith;

        // TODO: articlequality
        // TODO: draftquality

        public float getDamagingProb() {
            return damaging != null ? damaging.trueProb : 0f;
        }
    }

    public static class OresItem {
        @SerializedName("true") private float trueProb;
        @SerializedName("false") private float falseProb;
    }

    public static class DiffResponse {
        // TODO: from
        // TODO: to
        private List<DiffItem> diff;

        public List<DiffItem> getDiffs() {
            return diff != null ? diff : Collections.emptyList();
        }
    }

    public static class DiffItem {
        private int type;
        private int lineNumber;
        private String text;
        private DiffOffset offset;
        private List<HighlightRange> highlightRanges;

        public int getType() {
            return type;
        }

        public String getText() {
            return StringUtils.defaultString(text);
        }

        public List<HighlightRange> getHighlightRanges() {
            return highlightRanges != null ? highlightRanges : Collections.emptyList();
        }
    }

    public static class DiffOffset {
        private int from;
        private int to;
    }

    public static class HighlightRange {
        private int start;
        private int length;
        private int type;

        public int getStart() {
            return start;
        }

        public int getLength() {
            return length;
        }

        public int getType() {
            return type;
        }
    }

    public static WikiLoopService getWikiLoopService() {
        Retrofit r = new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient())
                .baseUrl("http://battlefield2.wikiloop.org/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
        return r.create(WikiLoopService.class);
    }

    public static RestService getService(WikiSite wiki) {
        Retrofit r = new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient())
                .baseUrl(wiki.url() + "/w/rest.php/v1/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
        return r.create(RestService.class);
    }

    public interface WikiLoopService {
        @GET("api/recentchanges/list")
        @NonNull
        Observable<List<RevertCandidate>> getRevertCandidates(@NonNull @Query("wiki") String wiki,
                                                              @Query("timestamp") long timestamp,
                                                              @NonNull @Query("direction") String direction,
                                                              @Query("limit") int limit);
    }

    public interface RestService {
        @GET("revision/{oldRev}/compare/{newRev}")
        @NonNull
        Observable<DiffResponse> getDiff(@Path("oldRev") long oldRev,
                                         @Path("newRev") long newRev);
    }
}
