package org.wikipedia.nearby;

import android.content.Context;
import android.location.Location;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Actual work to search for nearby pages.
 */
public class NearbyFetchTask extends ApiTask<List<NearbyPage>> {
    /** search radius in meters. 10 km is the maximum the API allows. */
    private static final String RADIUS = "10000";
    /** max number of results */
    private static final String LIMIT = "50";
    /** requested thumbnail size in pixel */
    private static final String THUMBNAIL_WIDTH = "144";
    private final WikipediaApp app;
    private final Location location;

    public NearbyFetchTask(Context context, Site site, Location location) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp) context.getApplicationContext()).getAPIForSite(site)
        );
        this.app = (WikipediaApp) context.getApplicationContext();
        this.location = location;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("prop", "coordinates|pageimages")
                .param("colimit", LIMIT)
                .param("pithumbsize", THUMBNAIL_WIDTH)
                .param("pilimit", LIMIT)
                .param("generator", "geosearch")
                .param("ggscoord", locationParam(location))
                .param("ggsradius", RADIUS)
                .param("ggslimit", LIMIT)
                .param("format", "json");
    }

    private String locationParam(Location location) {
        return String.format(Locale.ROOT, "%f|%f", location.getLatitude(), location.getLongitude());
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
    public List<NearbyPage> processResult(ApiResult result) throws Throwable {
        ArrayList<NearbyPage> list = new ArrayList<NearbyPage>();

        try {
            if (result.asObject().has("error")) {
                JSONObject errorJSON = result.asObject().optJSONObject("error");
                throw new NearbyFetchException(errorJSON.optString("code"), errorJSON.optString("info"));
            }

            final JSONObject pagesMap = result.asObject().optJSONObject("query").optJSONObject("pages");
            Iterator iterator = pagesMap.keys();

            while (iterator.hasNext()) {
                NearbyPage newPage = new NearbyPage(pagesMap.getJSONObject((String) iterator.next()));
                list.add(newPage);
            }
        } catch (ApiException e) {
            // TODO: find a better way to deal with empty results
            if (!e.getCause().getMessage().startsWith("Value []")) {
                throw e;
            }
        }

        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            Utils.processHeadersForZero(app, result);
        }

        return list;
    }
}
