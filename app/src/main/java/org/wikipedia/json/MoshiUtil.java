package org.wikipedia.json;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import org.wikipedia.dataclient.wikidata.Claims;

import java.util.Date;

public class MoshiUtil {
    private static final PolymorphicJsonAdapterFactory<Claims.DataValue> DATA_VALUE_ADAPTER_FACTORY
            = PolymorphicJsonAdapterFactory.of(Claims.DataValue.class, "type")
            .withSubtype(Claims.StringValue.class, Claims.DataValue.Type.STRING.getValue())
            .withSubtype(Claims.GlobeCoordinateValue.class, Claims.DataValue.Type.GLOBE_COORDINATE.getValue())
            .withSubtype(Claims.MonolingualTextValue.class, Claims.DataValue.Type.MONOLINGUAL_TEXT.getValue())
            .withSubtype(Claims.TimeValue.class, Claims.DataValue.Type.TIME.getValue())
            .withSubtype(Claims.EntityIdValue.class, Claims.DataValue.Type.WIKIBASE_ENTITY_ID.getValue());

    private static final Moshi DEFAULT_MOSHI_BUILDER = new Moshi.Builder()
            .add(Date.class, new Rfc3339DateJsonAdapter())
            .add(DATA_VALUE_ADAPTER_FACTORY)
            .build();

    @NonNull
    public static Moshi getDefaultMoshi() {
        return DEFAULT_MOSHI_BUILDER;
    }
}
