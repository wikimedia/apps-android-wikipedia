package org.wikipedia.json;

import android.location.Location;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import org.json.JSONObject;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.wikidata.Claims;
import org.wikipedia.page.GeoJsonAdapter;
import org.wikipedia.page.Namespace;

import java.util.Date;

public final class MoshiUtil {
    private static final PolymorphicJsonAdapterFactory<Claims.DataValue> DATA_VALUE_ADAPTER_FACTORY
            = PolymorphicJsonAdapterFactory.of(Claims.DataValue.class, "type")
            .withSubtype(Claims.StringValue.class, Claims.DataValue.Type.STRING.getValue())
            .withSubtype(Claims.GlobeCoordinateValue.class, Claims.DataValue.Type.GLOBE_COORDINATE.getValue())
            .withSubtype(Claims.MonolingualTextValue.class, Claims.DataValue.Type.MONOLINGUAL_TEXT.getValue())
            .withSubtype(Claims.TimeValue.class, Claims.DataValue.Type.TIME.getValue())
            .withSubtype(Claims.EntityIdValue.class, Claims.DataValue.Type.WIKIBASE_ENTITY_ID.getValue());

    private static final Moshi DEFAULT_MOSHI_BUILDER = new Moshi.Builder()
            .add(Date.class, new Rfc3339DateJsonAdapter())
            .add(Location.class, new GeoJsonAdapter().nullSafe())
            .add(Uri.class, new UriJsonAdapter().nullSafe())
            .add(Namespace.class, new NamespaceJsonAdapter().nullSafe())
            .add(WikiSite.class, new WikiSiteJsonAdapter().nullSafe())
            .add(SharedPreferenceCookieManager.class, new CookieManagerJsonAdapter().nullSafe())
            .add(JSONObject.class, new JSONObjectAdapter().nullSafe())
            .add(DATA_VALUE_ADAPTER_FACTORY)
            .addLast(new RequiredFieldsCheckOnReadJsonAdapterFactory())
            .build();

    @NonNull
    public static Moshi getDefaultMoshi() {
        return DEFAULT_MOSHI_BUILDER;
    }

    private MoshiUtil() { }
}
