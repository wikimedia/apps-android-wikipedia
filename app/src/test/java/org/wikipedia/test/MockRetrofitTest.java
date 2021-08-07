package org.wikipedia.test;

import com.squareup.moshi.Moshi;

import org.junit.Before;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.WikiSite;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public abstract class MockRetrofitTest extends MockWebServerTest {
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
        return new Moshi.Builder().build();
    }
}
