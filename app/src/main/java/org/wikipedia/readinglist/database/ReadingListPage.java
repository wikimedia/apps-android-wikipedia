package org.wikipedia.readinglist.database;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;

public class ReadingListPage {
    public static final int STATUS_QUEUE_FOR_SAVE = 0;
    public static final int STATUS_SAVED = 1;
    public static final int STATUS_QUEUE_FOR_DELETE = 2;
    public static final int STATUS_QUEUE_FOR_FORCED_SAVE = 3;

    public static final ReadingListPageTable DATABASE_TABLE = new ReadingListPageTable();

    private long id;
    private long listId;
    @NonNull private final WikiSite wiki;
    @NonNull private final Namespace namespace;
    @NonNull private String displayTitle;
    @NonNull private final String apiTitle;
    @Nullable private String description;
    @Nullable private String thumbUrl;

    private long mtime;
    private long atime;

    private boolean offline;
    private int status;
    private long sizeBytes;
    private String lang;
    private long revId;
    private long remoteId;

    private transient int downloadProgress;
    private transient boolean selected;
    @Nullable private transient String accentAndCaseInvariantTitle;

    public ReadingListPage(@NonNull WikiSite wiki, @NonNull Namespace namespace,
                           @NonNull String displayTitle, @NonNull String apiTitle, long listId) {
        this.wiki = wiki;
        this.namespace = namespace;
        this.displayTitle = displayTitle;
        this.apiTitle = TextUtils.isEmpty(apiTitle) ? displayTitle : apiTitle;
        this.listId = listId;
    }

    public ReadingListPage(@NonNull PageTitle title) {
        this.wiki = title.getWikiSite();
        this.namespace = title.namespace();
        this.displayTitle = title.getDisplayText();
        this.apiTitle = title.getPrefixedText();
        this.thumbUrl = title.getThumbUrl();
        this.description = title.getDescription();
        listId = -1;
        offline = Prefs.isDownloadingReadingListArticlesEnabled();
        status = STATUS_QUEUE_FOR_SAVE;
        long now = System.currentTimeMillis();
        mtime = now;
        atime = now;
    }

    public static PageSummary toPageSummary(@NonNull ReadingListPage page) {
        return new PageSummary(page.displayTitle, page.apiTitle, page.description, page.description, page.thumbUrl, page.lang);
    }

    public static PageTitle toPageTitle(@NonNull ReadingListPage page) {
        return new PageTitle(page.apiTitle(), page.wiki(), page.thumbUrl(), page.description(), page.title());
    }

    public long id() {
        return id;
    }
    public void id(long id) {
        this.id = id;
    }

    public long listId() {
        return listId;
    }
    public void listId(long listId) {
        this.listId = listId;
    }

    @NonNull public WikiSite wiki() {
        return wiki;
    }
    @NonNull public Namespace namespace() {
        return namespace;
    }
    @NonNull public String title() {
        return displayTitle;
    }
    public void title(String title) {
        this.displayTitle = title;
    }

    @NonNull public String apiTitle() {
        return apiTitle;
    }

    @NonNull public String accentAndCaseInvariantTitle() {
        if (accentAndCaseInvariantTitle == null) {
            accentAndCaseInvariantTitle = StringUtils.stripAccents(displayTitle).toLowerCase();
        }
        return accentAndCaseInvariantTitle;
    }

    @Nullable public String description() {
        return description;
    }
    public void description(@Nullable String description) {
        this.description = description;
    }

    @Nullable public String thumbUrl() {
        return thumbUrl;
    }
    public void thumbUrl(@Nullable String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public long mtime() {
        return mtime;
    }
    public void mtime(long mtime) {
        this.mtime = mtime;
    }

    public long atime() {
        return atime;
    }
    public void atime(long atime) {
        this.atime = atime;
    }

    public void touch() {
        atime = System.currentTimeMillis();
    }

    public long sizeBytes() {
        return sizeBytes;
    }
    public void sizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public long revId() {
        return revId;
    }
    public void revId(long revId) {
        this.revId = revId;
    }

    @NonNull public String lang() {
        return lang;
    }
    public void lang(String lang) {
        this.lang = lang;
    }

    public long remoteId() {
        return remoteId;
    }
    public void remoteId(long remoteId) {
        this.remoteId = remoteId;
    }

    public int status() {
        return status;
    }
    public void status(long status) {
        this.status = (int) status;
    }

    public boolean selected() {
        return selected;
    }
    public void selected(boolean selected) {
        this.selected = selected;
    }

    public boolean offline() {
        return offline;
    }
    public void offline(boolean offline) {
        this.offline = offline;
    }

    public boolean saving() {
        return offline && (status == STATUS_QUEUE_FOR_SAVE || status == STATUS_QUEUE_FOR_FORCED_SAVE);
    }

    public void downloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public int downloadProgress() {
        return downloadProgress;
    }
}
