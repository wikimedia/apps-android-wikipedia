package org.wikipedia.editing;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONObject;

public class AbuseFilterEditResult extends EditingResult {
    public static final int TYPE_WARNING = 1;
    public static final int TYPE_ERROR = 2;

    private final String code;
    private final String info;
    private final String warning;

    public AbuseFilterEditResult(JSONObject result) {
        super("Failure");
        this.code = result.optString("code");
        this.info = result.optString("info");
        this.warning = result.optString("warning");
    }

    protected AbuseFilterEditResult(Parcel in) {
        super(in);
        code = in.readString();
        info = in.readString();
        warning = in.readString();
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public String getWarning() {
        return warning;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(code);
        dest.writeString(info);
        dest.writeString(warning);
    }

    public int getType() {
        if (code.startsWith("abusefilter-warning")) {
            return TYPE_WARNING;
        } else if (code.startsWith("abusefilter-disallowed")) {
            return TYPE_ERROR;
        } else if (info.startsWith("Hit AbuseFilter")) {
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

    public static final Parcelable.Creator<AbuseFilterEditResult> CREATOR
            = new Parcelable.Creator<AbuseFilterEditResult>() {
        @Override
        public AbuseFilterEditResult createFromParcel(Parcel in) {
            return new AbuseFilterEditResult(in);
        }

        @Override
        public AbuseFilterEditResult[] newArray(int size) {
            return new AbuseFilterEditResult[size];
        }
    };
}
