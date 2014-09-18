package org.wikipedia.beta.editing;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONObject;

public class AbuseFilterEditResult extends EditingResult {
    public static final int TYPE_WARNING = 1;
    public static final int TYPE_ERROR = 2;


    private final String code;
    private final String warning;

    public AbuseFilterEditResult(JSONObject result) {
        super("Failure");
        this.code = result.optString("code");
        this.warning = result.optString("warning");
    }

    protected AbuseFilterEditResult(Parcel in) {
        super(in);
        code = in.readString();
        warning = in.readString();
    }

    public String getCode() {
        return code;
    }

    public String getWarning() {
        return warning;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(code);
        dest.writeString(warning);
    }

    public int getType() {
        if (code.startsWith("abusefilter-warning")) {
            return TYPE_WARNING;
        } else if (code.startsWith("abusefilter-disallowed")) {
            return TYPE_ERROR;
        } else {
            throw new RuntimeException("Unknown abusefilter response!");
        }
    }

    public static final Parcelable.Creator<AbuseFilterEditResult> CREATOR
            = new Parcelable.Creator<AbuseFilterEditResult>() {
        public AbuseFilterEditResult createFromParcel(Parcel in) {
            return new AbuseFilterEditResult(in);
        }

        public AbuseFilterEditResult[] newArray(int size) {
            return new AbuseFilterEditResult[size];
        }
    };
}
