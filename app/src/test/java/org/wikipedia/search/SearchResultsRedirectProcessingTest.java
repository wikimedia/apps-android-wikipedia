package org.wikipedia.search;

import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResult;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.test.TestRunner;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(TestRunner.class) public class SearchResultsRedirectProcessingTest {

    private List<MwQueryResult.Redirect> redirects;
    private List<MwQueryPage> results;

    @Before public void setUp() throws Throwable {
        TypeToken<List<MwQueryPage>> resultListToken = new TypeToken<List<MwQueryPage>>(){};
        results = GsonUtil.getDefaultGson().fromJson(resultsJson, resultListToken.getType());
        TypeToken<List<MwQueryResult.Redirect>> redirectListToken = new TypeToken<List<MwQueryResult.Redirect>>(){};
        redirects = GsonUtil.getDefaultGson().fromJson(redirectsJson, redirectListToken.getType());
    }

    @Test public void testRedirectHandling() throws Throwable {
        List<MwQueryPage> processedResults = PrefixSearchClient.updateWithRedirectInfo(results, redirects);
        assertThat(processedResults.size(), is(2));
        assertThat(processedResults.get(0).title(), is("Narthecium#Foo"));
        assertThat(processedResults.get(0).redirectFrom(), is("Abama"));
        assertThat(processedResults.get(1).title(), is("Amitriptyline"));
        assertThat(processedResults.get(1).redirectFrom(), is("Abamax"));
    }

    private String redirectsJson = "[\n"
            + "      {\n"
            + "        \"index\": 1,\n"
            + "        \"from\": \"Abama\",\n"
            + "        \"to\": \"Narthecium\",\n"
            + "        \"tofragment\": \"Foo\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"index\": 2,\n"
            + "        \"from\": \"Abamax\",\n"
            + "        \"to\": \"Amitriptyline\"\n"
            + "      }\n"
            + "    ]";

    private String resultsJson = "[\n"
            + "      {\n"
            + "        \"pageid\": 2060913,\n"
            + "        \"ns\": 0,\n"
            + "        \"title\": \"Narthecium\",\n"
            + "        \"index\": 1,\n"
            + "        \"terms\": {\n"
            + "          \"description\": [\n"
            + "            \"genus of plants\"\n"
            + "          ]\n"
            + "        },\n"
            + "        \"thumbnail\": {\n"
            + "          \"source\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Narthecium_ossifragum_01.jpg/240px-Narthecium_ossifragum_01.jpg\",\n"
            + "          \"width\": 240,\n"
            + "          \"height\": 320\n"
            + "        }\n"
            + "      },\n"
            + "      {\n"
            + "        \"pageid\": 583678,\n"
            + "        \"ns\": 0,\n"
            + "        \"title\": \"Amitriptyline\",\n"
            + "        \"index\": 2,\n"
            + "        \"terms\": {\n"
            + "          \"description\": [\n"
            + "            \"chemical compound\",\n"
            + "            \"chemical compound\"\n"
            + "          ]\n"
            + "        },\n"
            + "        \"thumbnail\": {\n"
            + "          \"source\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/Amitriptyline2DACS.svg/318px-Amitriptyline2DACS.svg.png\",\n"
            + "          \"width\": 318,\n"
            + "          \"height\": 320\n"
            + "        }\n"
            + "      }\n"
            + "    ]";

}
