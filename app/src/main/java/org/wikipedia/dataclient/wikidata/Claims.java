package org.wikipedia.dataclient.wikidata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Claims extends MwResponse {
    @Nullable private Map<String, List<Claim>> claims;

    @NonNull public Map<String, List<Claim>> claims() {
        return claims != null ? claims : Collections.emptyMap();
    }

    public static class Claim {
        @Nullable @SerializedName("mainsnak") private MainSnak mainSnak;
        @Nullable private String type;
        @Nullable private String id;
        @Nullable private String rank;

        @Nullable public MainSnak getMainSnak() {
            return mainSnak;
        }
    }

    public static class MainSnak {
        @Nullable @SerializedName("snaktype") private String snakType;
        @Nullable private String property;
        @Nullable private String hash;
        @Nullable @SerializedName("datavalue") private DataValue dataValue;
        @Nullable @SerializedName("datatype") private String dataType;

        @Nullable public DataValue getDataValue() {
            return dataValue;
        }
    }

    public static class DataValue {
        @Nullable private Value value;
        @Nullable private String type;

        @Nullable public Value getValue() {
            return value;
        }
    }

    public static class Value {
        @Nullable private String id;

        @NonNull public String getId() {
            return StringUtils.defaultString(id);
        }
    }
}
