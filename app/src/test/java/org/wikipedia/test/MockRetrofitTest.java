package org.wikipedia.test;

import org.junit.Before;
import org.wikipedia.dataclient.Service;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class MockRetrofitTest extends MockWebServerTest {
    private Service apiService;

    @Override
    @Before
    public void setUp() throws Throwable {
        super.setUp();
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(server().getUrl())
                .build();
        apiService = retrofit.create(Service.class);
    }

    protected Service getApiService() {
        return apiService;
    }
}
