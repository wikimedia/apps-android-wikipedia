package org.wikipedia.readinglist.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.model.BaseModel;
import org.wikipedia.page.Namespace;
import org.wikipedia.readinglist.page.database.ReadingListPageDiskTable;
import org.wikipedia.readinglist.page.database.ReadingListPageHttpTable;
import org.wikipedia.readinglist.page.database.ReadingListPageTable;
import org.wikipedia.util.ValidateUtil;

import java.util.Collections;
import java.util.Set;

public class ReadingListPageRow extends BaseModel {
    public static final ReadingListPageTable DATABASE_TABLE = new ReadingListPageTable();
    public static final ReadingListPageHttpTable HTTP_DATABASE_TABLE = new ReadingListPageHttpTable();
    public static final ReadingListPageDiskTable DISK_DATABASE_TABLE = new ReadingListPageDiskTable();

    @NonNull private final String key;
    @NonNull private final Set<String> listKeys;
    // todo: remove @SerializedName if not pickled
    @SerializedName("site") @NonNull private final WikiSite wiki;
    @NonNull private final Namespace namespace;
    @NonNull private final String title;
    @Nullable private Long diskPageRevision;
    private final long mtime;
    private long atime;
    @Nullable private String thumbnailUrl;
    @Nullable private String description;

    // The size in bytes for an offline page and all page resources downloaded by
    // SavedPageSyncService. Null or 0 if DiskStatus.ONLINE or not yet downloaded. Outdated if
    // the saved page cache size is later exceeded and resources are evicted. Written to by
    // SavedPageSyncService.
    // @see ReadingListPageContract.PageCol.PHYSICAL_SIZE
    // @see ReadingListPageContract.PageCol.LOGICAL_SIZE
    @Nullable private final Long physicalSize;
    // The size on disk in bytes.
    @Nullable private final Long logicalSize;

    public static Builder<?> builder() {
        //noinspection rawtypes
        return new Builder();
    }

    @NonNull public String key() {
        return key;
    }

    @NonNull public WikiSite wikiSite() {
        return wiki;
    }

    /** @return an empty list if the page is to be deleted. */
    @NonNull public Set<String> listKeys() {
        return Collections.unmodifiableSet(listKeys);
    }

    public void addListKey(@NonNull String listKey) {
        listKeys.add(listKey);
    }

    public void removeListKey(@NonNull String listKey) {
        listKeys.remove(listKey);
    }

    @NonNull public Namespace namespace() {
        return namespace;
    }

    @NonNull public String title() {
        return title;
    }

    @Nullable public Long diskPageRevision() {
        return diskPageRevision;
    }

    /** @return the time of last modification in seconds since epoch. */
    public long mtime() {
        return mtime;
    }

    /** @return the time of last access in seconds since epoch. */
    public long atime() {
        return atime;
    }

    public void touch() {
        atime = System.currentTimeMillis();
    }

    @Nullable public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(@Nullable String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable public String description() {
        return description;
    }

    @Nullable public Long physicalSize() {
        return physicalSize;
    }

    @Nullable public Long logicalSize() {
        return logicalSize;
    }

    protected ReadingListPageRow(@NonNull Builder<?> builder) {
        key = builder.key;
        listKeys = new ArraySet<>(builder.listKeys);
        wiki = builder.wiki;
        namespace = builder.namespace;
        title = builder.title;
        diskPageRevision = builder.diskPageRevision;
        mtime = builder.mtime;
        atime = builder.atime;
        thumbnailUrl = builder.thumbnailUrl;
        description = builder.description;
        physicalSize = builder.physicalSize;
        logicalSize = builder.logicalSize;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<Clazz extends Builder<Clazz>> {
        private String key;
        private Set<String> listKeys = new ArraySet<>();
        private WikiSite wiki;
        private Namespace namespace;
        private String title;
        private Long diskPageRevision;
        private Long mtime;
        private Long atime;
        private String thumbnailUrl;
        private String description;
        private Long physicalSize;
        private Long logicalSize;

        public Clazz copy(@NonNull ReadingListPageRow copy) {
            return key(copy.key)
                    .listKeys(copy.listKeys)
                    .site(copy.wiki)
                    .namespace(copy.namespace)
                    .title(copy.title)
                    .mtime(copy.mtime)
                    .atime(copy.atime)
                    .thumbnailUrl(copy.thumbnailUrl)
                    .description(copy.description)
                    .physicalSize(copy.physicalSize)
                    .logicalSize(copy.logicalSize);
        }

        public Clazz key(@NonNull String key) {
            this.key = key;
            return (Clazz) this;
        }

        public Clazz listKeys(@NonNull String listKey) {
            listKeys = new ArraySet<>();
            listKeys.add(listKey);
            return (Clazz) this;
        }

        public Clazz listKeys(@NonNull Set<String> listKeys) {
            this.listKeys = new ArraySet<>(listKeys);
            return (Clazz) this;
        }

        public Clazz site(@NonNull WikiSite wiki) {
            this.wiki = wiki;
            return (Clazz) this;
        }

        public Clazz namespace(@NonNull Namespace namespace) {
            this.namespace = namespace;
            return (Clazz) this;
        }

        public Clazz title(@NonNull String title) {
            this.title = title;
            return (Clazz) this;
        }

        public Clazz diskPageRevision(long diskPageRevision) {
            this.diskPageRevision = diskPageRevision;
            return (Clazz) this;
        }

        public Clazz mtime(long mtime) {
            this.mtime = mtime;
            return (Clazz) this;
        }

        public Clazz atime(long atime) {
            this.atime = atime;
            return (Clazz) this;
        }

        public Clazz thumbnailUrl(@Nullable String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return (Clazz) this;
        }

        public Clazz description(@Nullable String description) {
            this.description = description;
            return (Clazz) this;
        }

        public Clazz physicalSize(@Nullable Long physicalSize) {
            this.physicalSize = physicalSize;
            return (Clazz) this;
        }

        public Clazz logicalSize(@Nullable Long logicalSize) {
            this.logicalSize = logicalSize;
            return (Clazz) this;
        }

        public ReadingListPageRow build() {
            validate();
            return new ReadingListPageRow(this);
        }

        // TODO: listKeys allows empty currently. Should we permit it? It means delete.
        protected void validate() {
            ValidateUtil.noNullElements(key, wiki, namespace, title, mtime, atime);
        }
    }
}
