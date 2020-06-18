package org.wikipedia.test;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.NamespaceTypeAdapter;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.json.UriTypeAdapter;
import org.wikipedia.json.WikiSiteTypeAdapter;
import org.wikipedia.page.Namespace;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class MockRetrofitTest extends MockWebServerTest {
    private Service apiService;
    private RestService restService;
    private WikiSite wikiSite = WikiSite.forLanguageCode("en");

    protected WikiSite wikiSite() {
        return wikiSite;
    }

    @Override
    @Before
    public void setUp() throws Throwable {
        super.setUp();
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGson()))
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

    private Gson getGson() {
        return new GsonBuilder()
                .registerTypeHierarchyAdapter(Uri.class, new UriTypeAdapter().nullSafe())
                .registerTypeHierarchyAdapter(Namespace.class, new NamespaceTypeAdapter().nullSafe())
                .registerTypeAdapter(WikiSite.class, new WikiSiteTypeAdapter().nullSafe())
                .registerTypeAdapterFactory(new PostProcessingTypeAdapter())
                .create();
    }
}
