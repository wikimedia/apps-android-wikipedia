package org.wikipedia.nearby;

import android.content.Context;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import java.util.Locale;

/**
 * Actual work to search for nearby pages.
 */
public class NearbyFetchTask extends ApiTask<NearbyResult> {
    public static final int MAX_RADIUS = 10000;
    /** max number of results */
    private static final String LIMIT = "100";
    private final double latitude;
    private final double longitude;
    private final double radius;

    public NearbyFetchTask(Context context, Site site, double latitude, double longitude, double radius) {
        super(((WikipediaApp) context.getApplicationContext()).getAPIForSite(site));
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius < MAX_RADIUS ? radius : MAX_RADIUS;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("prop", "coordinates|pageimages|pageterms")
                .param("colimit", LIMIT)
                .param("piprop", "thumbnail") // so response doesn't contain unused "pageimage" prop
                .param("pithumbsize", Integer.toString(Constants.PREFERRED_THUMB_SIZE))
                .param("pilimit", LIMIT)
                .param("wbptterms", "description")
                .param("generator", "geosearch")
                .param("ggscoord", String.format(Locale.ROOT, "%f|%f", latitude, longitude))
                .param("ggsradius", Double.toString(radius))
                .param("ggslimit", LIMIT)
                .param("format", "json")
                .param("continue", ""); // to avoid warning about new continuation syntax
    }

    /*

    https://test.wikipedia.org/wiki/Special:ApiSandbox#action=query&prop=coordinates&format=json&colimit=10&generator=geosearch&ggscoord=37.786688999999996%7C-122.3994771999999&ggsradius=10000&ggslimit=10
    API: /w/api.php?action=query&prop=coordinates&format=json&colimit=10&generator=geosearch&ggscoord=37.786688999999996%7C-122.3994771999999&ggsradius=10000&ggslimit=10

    // returns data formatted as follows:
{
    "query": {
        "pages": {
            "44175": {
                "pageid": 44175,
                "ns": 0,
                "title": "San Francisco",
                "coordinates": [
                    {
                        "lat": 37.7793,
                        "lon": -122.419,
                        "primary": "",
                        "globe": "earth"
                    }
                ]
            },
            "74129": {
                "pageid": 74129,
                "ns": 0,
                "title": "Page which has geodata",
                "coordinates": [
                    {
                        "lat": 37.787,
                        "lon": -122.4,
                        "primary": "",
                        "globe": "earth"
                    }
                ]
            }
        }
    }
}
*/


    @Override
    public NearbyResult processResult(ApiResult result) throws Throwable {
        try {
            JSONObject jsonObject = result.asObject();
            return new NearbyResult(jsonObject);
        } catch (ApiException e) {
            // TODO: find a better way to deal with empty results
            if (e.getCause().getMessage().startsWith("Value []")) {
                return new NearbyResult();
            } else {
                throw e;
            }
        }
    }
}
