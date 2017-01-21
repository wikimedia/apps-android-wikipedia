package org.wikipedia.edit;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

class EditAbuseFilterResult extends EditResult {
    static final int TYPE_WARNING = 1;
    static final int TYPE_ERROR = 2;

    @Nullable private final String code;
    @Nullable private final String info;
    @Nullable private final String warning;

    EditAbuseFilterResult(@Nullable String code, @Nullable String info, @Nullable String warning) {
        super("Failure");
        this.code = code;
        this.info = info;
        this.warning = warning;
    }

    private EditAbuseFilterResult(Parcel in) {
        super(in);
        code = in.readString();
        info = in.readString();
        warning = in.readString();
    }

    @Nullable public String getCode() {
        return code;
    }

    @Nullable public String getInfo() {
        return info;
    }

    @Nullable public String getWarning() {
        return warning;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(code);
        dest.writeString(info);
        dest.writeString(warning);
    }

    public int getType() {
        if (code != null && code.startsWith("abusefilter-warning")) {
            return TYPE_WARNING;
        } else if (code != null && code.startsWith("abusefilter-disallowed")) {
            return TYPE_ERROR;
        } else if (info != null && info.startsWith("Hit AbuseFilter")) {
            // This case is here because, unfortunately, an admin can create an abuse filter which
            // emits an arbitrary error code over the API.
            // TODO: More properly handle the case where the AbuseFilter throws an arbitrary error.
            // Oh, and, you know, also fix the AbuseFilter API to not throw arbitrary error codes.
            return TYPE_ERROR;
        } else {
            // We have no understanding of what kind of abuse filter response we got. It's safest
            // to simply treat these as an error.
            return TYPE_ERROR;
        }
    }

    public static final Parcelable.Creator<EditAbuseFilterResult> CREATOR
            = new Parcelable.Creator<EditAbuseFilterResult>() {
        @Override
        public EditAbuseFilterResult createFromParcel(Parcel in) {
            return new EditAbuseFilterResult(in);
        }

        @Override
        public EditAbuseFilterResult[] newArray(int size) {
            return new EditAbuseFilterResult[size];
        }
    };
}
