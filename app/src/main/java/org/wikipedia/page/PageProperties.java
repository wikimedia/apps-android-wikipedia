package org.wikipedia.page;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.dataclient.page.Protection;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.UriUtil;

import java.util.Date;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.util.DateUtil.iso8601DateParse;

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
public class PageProperties implements Parcelable {
    private final int pageId;
    @NonNull private final Namespace namespace;
    private final long revisionId;
    private final Date lastModified;
    private final String displayTitleText;
    private String editProtectionStatus;
    private final boolean isMainPage;
    /** Nullable URL with no scheme. For example, foo.bar.com/ instead of http://foo.bar.com/. */
    @Nullable private final String leadImageUrl;
    @Nullable private final String leadImageName;
    private final int leadImageWidth;
    private final int leadImageHeight;
    @Nullable private final Location geo;
    @Nullable private final String wikiBaseItem;
    @Nullable private final String descriptionSource;
    @Nullable private Protection protection;

    /**
     * True if the user who first requested this page can edit this page
     * FIXME: This is not a true page property, since it depends on current user.
     */
    private boolean canEdit;

    /**
     * Side note: Should later be moved out of this class but I like the similarities with
     * PageProperties(JSONObject).
     */
    public PageProperties(@NonNull PageSummary pageSummary) {
        pageId = pageSummary.getPageId();
        namespace = pageSummary.getNamespace();
        revisionId = pageSummary.getRevision();
        displayTitleText = defaultString(pageSummary.getDisplayTitle());
        geo = pageSummary.getGeo();
        lastModified = new Date();
        leadImageName = UriUtil.decodeURL(StringUtils.defaultString(pageSummary.getLeadImageName()));
        leadImageUrl = pageSummary.getThumbnailUrl() != null
                ? UriUtil.resolveProtocolRelativeUrl(ImageUrlUtil.getUrlForPreferredSize(pageSummary.getThumbnailUrl(), DimenUtil.calculateLeadImageWidth())) : null;
        leadImageWidth = pageSummary.getThumbnailWidth();
        leadImageHeight = pageSummary.getThumbnailHeight();
        String lastModifiedText = pageSummary.getTimestamp();
        if (lastModifiedText != null) {
            lastModified.setTime(iso8601DateParse(lastModifiedText).getTime());
        }
        // assume formatversion=2 is used so we get real booleans from the API

        isMainPage = pageSummary.getType().equals(PageSummary.TYPE_MAIN_PAGE);
        wikiBaseItem = pageSummary.getWikiBaseItem();
        descriptionSource = pageSummary.getDescriptionSource();
    }

    /**
     * Constructor to be used when building a Page from a compilation. Initializes the title and
     * namespace fields, and explicitly disables editing. All other fields initialized to defaults.
     * @param title Title to which these properties apply.
     */
    public PageProperties(@NonNull PageTitle title, boolean isMainPage) {
        pageId = 0;
        namespace = title.namespace();
        revisionId = 0;
        displayTitleText = title.getDisplayText();
        geo = null;
        editProtectionStatus = "";
        leadImageUrl = null;
        leadImageName = "";
        leadImageWidth = 0;
        leadImageHeight = 0;
        lastModified = new Date();
        canEdit = false;
        this.isMainPage = isMainPage;
        wikiBaseItem = null;
        descriptionSource = null;
    }

    public int getPageId() {
        return pageId;
    }

    @NonNull public Namespace getNamespace() {
        return namespace;
    }

    public long getRevisionId() {
        return revisionId;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getDisplayTitle() {
        return displayTitleText;
    }

    @Nullable
    public Location getGeo() {
        return geo;
    }

    public String getEditProtectionStatus() {
        return editProtectionStatus;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean isMainPage() {
        return isMainPage;
    }

    /**
     * @return Nullable URL with no scheme. For example, foo.bar.com/ instead of
     *         http://foo.bar.com/.
     */
    @Nullable
    public String getLeadImageUrl() {
        return leadImageUrl;
    }

    @Nullable
    public String getLeadImageName() {
        return leadImageName;
    }

    public int getLeadImageWidth() {
        return leadImageWidth;
    }

    public int getLeadImageHeight() {
        return leadImageHeight;
    }

    @Nullable
    public String getWikiBaseItem() {
        return wikiBaseItem;
    }

    @Nullable
    public String getDescriptionSource() {
        return descriptionSource;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void setProtection(@Nullable Protection protection) {
        this.protection = protection;
        this.editProtectionStatus = protection != null ? protection.getFirstAllowedEditorRole() : null;
        this.canEdit = (TextUtils.isEmpty(editProtectionStatus) || isLoggedInUserAllowedToEdit());
    }

    private boolean isLoggedInUserAllowedToEdit() {
        return protection != null && AccountUtil.isMemberOf(protection.getEditRoles());
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(pageId);
        parcel.writeInt(namespace.code());
        parcel.writeLong(revisionId);
        parcel.writeLong(lastModified.getTime());
        parcel.writeString(displayTitleText);
        parcel.writeValue(geo);
        parcel.writeString(editProtectionStatus);
        parcel.writeInt(canEdit ? 1 : 0);
        parcel.writeInt(isMainPage ? 1 : 0);
        parcel.writeString(leadImageUrl);
        parcel.writeString(leadImageName);
        parcel.writeInt(leadImageWidth);
        parcel.writeInt(leadImageHeight);
        parcel.writeString(wikiBaseItem);
        parcel.writeString(descriptionSource);
    }

    private PageProperties(Parcel in) {
        pageId = in.readInt();
        namespace = Namespace.of(in.readInt());
        revisionId = in.readLong();
        lastModified = new Date(in.readLong());
        displayTitleText = in.readString();
        geo = (Location) in.readValue(Location.class.getClassLoader());
        editProtectionStatus = in.readString();
        canEdit = in.readInt() == 1;
        isMainPage = in.readInt() == 1;
        leadImageUrl = in.readString();
        leadImageName = in.readString();
        leadImageWidth = in.readInt();
        leadImageHeight = in.readInt();
        wikiBaseItem = in.readString();
        descriptionSource = in.readString();
    }

    public static final Parcelable.Creator<PageProperties> CREATOR
            = new Parcelable.Creator<PageProperties>() {
        @Override
        public PageProperties createFromParcel(Parcel in) {
            return new PageProperties(in);
        }

        @Override
        public PageProperties[] newArray(int size) {
            return new PageProperties[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PageProperties that = (PageProperties) o;

        return pageId == that.pageId
                && namespace == that.namespace
                && revisionId == that.revisionId
                && lastModified.equals(that.lastModified)
                && displayTitleText.equals(that.displayTitleText)
                && ((geo == null || that.geo == null) || (geo.getLatitude() == that.geo.getLatitude() && geo.getLongitude() == that.geo.getLongitude()))
                && canEdit == that.canEdit
                && isMainPage == that.isMainPage
                && TextUtils.equals(editProtectionStatus, that.editProtectionStatus)
                && TextUtils.equals(leadImageUrl, that.leadImageUrl)
                && TextUtils.equals(leadImageName, that.leadImageName)
                && leadImageWidth == that.leadImageWidth
                && leadImageHeight == that.leadImageHeight
                && TextUtils.equals(wikiBaseItem, that.wikiBaseItem);
    }

    @Override
    public int hashCode() {
        int result = lastModified.hashCode();
        result = 31 * result + displayTitleText.hashCode();
        result = 31 * result + (geo != null ? geo.hashCode() : 0);
        result = 31 * result + (editProtectionStatus != null ? editProtectionStatus.hashCode() : 0);
        result = 31 * result + (isMainPage ? 1 : 0);
        result = 31 * result + (leadImageUrl != null ? leadImageUrl.hashCode() : 0);
        result = 31 * result + (leadImageName != null ? leadImageName.hashCode() : 0);
        result = 31 * result + leadImageWidth;
        result = 31 * result + leadImageHeight;
        result = 31 * result + (wikiBaseItem != null ? wikiBaseItem.hashCode() : 0);
        result = 31 * result + (canEdit ? 1 : 0);
        result = 31 * result + pageId;
        result = 31 * result + namespace.code();
        result = 31 * result + (int) revisionId;
        return result;
    }
}
