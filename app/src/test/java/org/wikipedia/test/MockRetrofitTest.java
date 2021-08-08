package org.wikipedia.test;

import android.location.Location;
import android.net.Uri;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import org.junit.Before;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.wikidata.Claims;
import org.wikipedia.json.CookieManagerJsonAdapter;
import org.wikipedia.json.NamespaceJsonAdapter;
import org.wikipedia.json.UriJsonAdapter;
import org.wikipedia.json.WikiSiteJsonAdapter;
import org.wikipedia.page.GeoJsonAdapter;
import org.wikipedia.page.Namespace;

import java.util.Date;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public abstract class MockRetrofitTest extends MockWebServerTest {
    private static final PolymorphicJsonAdapterFactory<Claims.DataValue> DATA_VALUE_ADAPTER_FACTORY
            = PolymorphicJsonAdapterFactory.of(Claims.DataValue.class, "type")
            .withSubtype(Claims.StringValue.class, Claims.DataValue.Type.STRING.getValue())
            .withSubtype(Claims.GlobeCoordinateValue.class, Claims.DataValue.Type.GLOBE_COORDINATE.getValue())
            .withSubtype(Claims.MonolingualTextValue.class, Claims.DataValue.Type.MONOLINGUAL_TEXT.getValue())
            .withSubtype(Claims.TimeValue.class, Claims.DataValue.Type.TIME.getValue())
            .withSubtype(Claims.EntityIdValue.class, Claims.DataValue.Type.WIKIBASE_ENTITY_ID.getValue());

    private Service apiService;
    private RestService restService;
    private final WikiSite wikiSite = WikiSite.forLanguageCode("en");

    protected WikiSite getWikiSite() {
        return wikiSite;
    }

    @Override
    @Before
    public void setUp() throws Throwable {
        super.setUp();
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
                .baseUrl(server().getUrl())
                .build();
        apiService = retrofit.create(Service.class);
        restService = retrofit.create(RestService.class);
    }

    protected Service getApiService() {
        return apiService;
    }

    protected RestService getRestService() {
        return restService;
    }

    private Moshi getMoshi() {
        return new Moshi.Builder()
                .add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
                .add(Location.class, new GeoJsonAdapter().nullSafe())
                .add(Uri.class, new UriJsonAdapter().nullSafe())
                .add(Namespace.class, new NamespaceJsonAdapter().nullSafe())
                .add(WikiSite.class, new WikiSiteJsonAdapter().nullSafe())
                .add(SharedPreferenceCookieManager.class, new CookieManagerJsonAdapter().nullSafe())
                .add(DATA_VALUE_ADAPTER_FACTORY)
                .build();
    }
}
