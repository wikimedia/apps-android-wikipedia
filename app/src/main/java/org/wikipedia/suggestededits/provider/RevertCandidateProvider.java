package org.wikipedia.suggestededits.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.json.GsonUtil;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class RevertCandidateProvider {

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

    public static WikiLoopService getWikiLoopService() {
        Retrofit r = new Retrofit.Builder()
                .client(OkHttpConnectionFactory.getClient())
                .baseUrl("http://battlefield.wikiloop.org/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
        return r.create(WikiLoopService.class);
    }

    public interface WikiLoopService {
        @GET("api/recentchanges/list")
        @NonNull
        Observable<List<RevertCandidate>> getRevertCandidates(@NonNull @Query("wiki") String wiki,
                                                              @Query("timestamp") long timestamp,
                                                              @NonNull @Query("direction") String direction,
                                                              @Query("limit") int limit);
    }
}
