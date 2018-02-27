package org.wikipedia.readinglist.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.json.annotations.Required;

import java.text.Normalizer;
import java.util.List;

public class SyncedReadingLists {

    @SuppressWarnings("unused,NullableProblems") @Nullable private List<RemoteReadingList> lists;
    @SuppressWarnings("unused,NullableProblems") @Nullable private List<RemoteReadingListEntry> entries;
    @SuppressWarnings("unused,NullableProblems") @Nullable private String next;

    public SyncedReadingLists() { }

    public SyncedReadingLists(@NonNull List<RemoteReadingList> lists, @NonNull List<RemoteReadingListEntry> entries) {
        this.lists = lists;
        this.entries = entries;
    }

    @Nullable public List<RemoteReadingList> getLists() {
        return lists;
    }

    @Nullable public List<RemoteReadingListEntry> getEntries() {
        return entries;
    }

    @Nullable public String getContinueStr() {
        return next;
    }

    public static class RemoteReadingList {
        @SuppressWarnings("unused") @Required private long id;
        @SuppressWarnings("unused") @SerializedName("default") private boolean isDefault;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String name;
        @SuppressWarnings("unused") @Nullable private String description;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String created;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String updated;
        @SuppressWarnings("unused") private boolean deleted;

        public RemoteReadingList() { }

        public RemoteReadingList(@NonNull String name, @Nullable String description) {
            this.name = Normalizer.normalize(name, Normalizer.Form.NFC);
            this.description = Normalizer.normalize(StringUtils.defaultString(description), Normalizer.Form.NFC);
        }

        public long id() {
            return id;
        }

        @NonNull public String name() {
            return Normalizer.normalize(name, Normalizer.Form.NFC);
        }

        @NonNull public String description() {
            return Normalizer.normalize(StringUtils.defaultString(description), Normalizer.Form.NFC);
        }

        public boolean isDefault() {
            return isDefault;
        }

        public boolean isDeleted() {
            return deleted;
        }

        @NonNull public String updatedDate() {
            return updated;
        }
    }

    public static class RemoteReadingListEntry {
        @SuppressWarnings("unused") private long id;
        @SuppressWarnings("unused") private long listId;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String project;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String title;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String created;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String updated;
        @SuppressWarnings("unused") @Nullable private RbPageSummary summary;
        @SuppressWarnings("unused") private boolean deleted;

        public RemoteReadingListEntry() { }

        public RemoteReadingListEntry(@NonNull String project, @NonNull String title) {
            this.project = Normalizer.normalize(project, Normalizer.Form.NFC);
            this.title = Normalizer.normalize(title, Normalizer.Form.NFC);
        }

        public long id() {
            return id;
        }

        public long listId() {
            return listId;
        }

        @NonNull public String project() {
            return Normalizer.normalize(project, Normalizer.Form.NFC);
        }

        @NonNull public String title() {
            return Normalizer.normalize(title, Normalizer.Form.NFC);
        }

        @NonNull public String updatedDate() {
            return updated;
        }

        @Nullable public RbPageSummary summary() {
            return summary;
        }

        public boolean isDeleted() {
            return deleted;
        }
    }

    public static class RemoteReadingListEntryBatch {
        @SuppressWarnings("unused") private RemoteReadingListEntry[] batch;

        public RemoteReadingListEntryBatch() { }

        public RemoteReadingListEntryBatch(@NonNull List<RemoteReadingListEntry> entries) {
            this.batch = entries.toArray(new RemoteReadingListEntry[]{});
        }
    }

    public class RemoteIdResponse {
        @SuppressWarnings("unused") @Required private long id;

        public long id() {
            return id;
        }
    }

    public class RemoteIdResponseBatch {
        @SuppressWarnings("unused") @Required private RemoteIdResponse[] batch;

        public RemoteIdResponse[] batch() {
            return batch;
        }
    }
}
