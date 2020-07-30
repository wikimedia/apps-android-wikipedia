package org.wikipedia.dataclient.wikidata;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwResponse;
import org.wikipedia.json.GsonUtil;

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
        @Nullable private JsonElement value;
        @Nullable private String type;

        @NonNull public String getValue() {
            if (value != null) {
                if ("string".equals(type) && value.isJsonPrimitive()) {
                    return value.getAsString();
                } else if ("wikibase-entityid".equals(type) && value.isJsonObject()) {
                    return GsonUtil.getDefaultGson().fromJson(value, EntityIdValue.class).getId();
                } else if ("time".equals(type) && value.isJsonObject()) {
                    return GsonUtil.getDefaultGson().fromJson(value, TimeValue.class).getTime();
                } else if ("monolingualtext".equals(type) && value.isJsonObject()) {
                    return GsonUtil.getDefaultGson().fromJson(value, MonolingualTextValue.class).getText();
                } else if ("globecoordinate".equals(type) && value.isJsonObject()) {
                    return GsonUtil.getDefaultGson().fromJson(value, GlobeCoordinateValue.class).getLocation().toString();
                }
            }
            return "";
        }
    }

    public static class EntityIdValue {
        @Nullable private String id;

        @NonNull public String getId() {
            return StringUtils.defaultString(id);
        }
    }

    public static class TimeValue {
        @Nullable private String time;
        private int timezone;
        private int before;
        private int after;
        private int precision;

        @NonNull public String getTime() {
            return StringUtils.defaultString(time);
        }
    }

    public static class MonolingualTextValue {
        @Nullable private String text;
        @Nullable private String language;

        @NonNull public String getText() {
            return StringUtils.defaultString(text);
        }
    }

    public static class GlobeCoordinateValue {
        private double latitude;
        private double longitude;
        private double altitude;
        private double precision;

        @NonNull public Location getLocation() {
            Location loc = new Location("");
            loc.setLatitude(latitude);
            loc.setLongitude(longitude);
            loc.setAltitude(altitude);
            return loc;
        }
    }
}
