package org.wikipedia.dataclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.captcha.Captcha;
import org.wikipedia.dataclient.mwapi.CreateAccountResponse;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.SiteMatrix;
import org.wikipedia.dataclient.mwapi.page.MwMobileViewPageLead;
import org.wikipedia.dataclient.mwapi.page.MwMobileViewPageRemaining;
import org.wikipedia.dataclient.mwapi.page.MwQueryPageSummary;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.edit.Edit;
import org.wikipedia.edit.preview.EditPreview;
import org.wikipedia.login.LoginClient;
import org.wikipedia.search.PrefixSearchResponse;
import org.wikipedia.wikidata.Entities;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit service layer for all API interactions, including regular MediaWiki and RESTBase.
 */
public interface Service {
    String WIKIPEDIA_URL = "https://wikipedia.org/";
    String WIKIDATA_URL = "https://www.wikidata.org/";
    String COMMONS_URL = "https://commons.wikimedia.org/";
    String META_URL = "https://meta.wikimedia.org/";

    String MW_API_PREFIX = "w/api.php?format=json&formatversion=2&errorformat=plaintext&";

    String MW_PAGE_SECTIONS_URL = MW_API_PREFIX + "action=mobileview&prop="
                                                + "text|sections&onlyrequestedsections=1&sections=1-"
                                                + "&sectionprop=toclevel|line|anchor&noheadings=";

    int PREFERRED_THUMB_SIZE = 320;

    // ------- MobileView page content -------

    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     * @return a Retrofit Call which provides the populated MwMobileViewPageLead object in #success
     */
     /*
      Here's the rationale for this API call:
      We request 10 sentences from the lead section, and then re-parse the text using our own
      sentence parsing logic to end up with 2 sentences for the link preview. We trust our
      parsing logic more than TextExtracts because it's better-tailored to the user's
      Locale on the client side. For example, the TextExtracts extension incorrectly treats
      abbreviations like "i.e.", "B.C.", "Jr.", etc. as separate sentences, whereas our parser
      will leave those alone.

      Also, we no longer request "excharacters" from TextExtracts, since it has an issue where
      it's liable to return content that lies beyond the lead section, which might include
      unparsed wikitext, which we certainly don't want.
    */
    @Headers("x-analytics: preview=1")
    @GET(MW_API_PREFIX + "action=query&redirects=&converttitles="
            + "&prop=extracts|pageimages|pageprops&exsentences=5&piprop=thumbnail|name"
            + "&pilicense=any&explaintext=&pithumbsize=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryPageSummary> getSummary(@Nullable @Header("Referer") String referrerUrl,
                                                 @NonNull @Query("titles") String title,
                                                 @Nullable @Query("uselang") String useLang);

    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     * @param leadImageWidth one of the bucket widths for the lead image
     */
    @Headers("x-analytics: pageview=1")
    @GET(MW_API_PREFIX + "action=mobileview&prop="
            + "text|sections|languagecount|thumb|image|id|namespace|revision"
            + "|description|lastmodified|normalizedtitle|displaytitle|protection"
            + "|editable|pageprops&pageprops=wikibase_item"
            + "&sections=0&sectionprop=toclevel|line|anchor&noheadings=")
    @NonNull Observable<Response<MwMobileViewPageLead>> getLeadSection(@Nullable @Header("Cache-Control") String cacheControl,
                                                                       @Nullable @Header(OfflineCacheInterceptor.SAVE_HEADER) String saveHeader,
                                                                       @Nullable @Header("Referer") String referrerUrl,
                                                                       @NonNull @Query("page") String title,
                                                                       @Query("thumbwidth") int leadImageWidth,
                                                                       @Nullable @Query("uselang") String useLang);

    /**
     * Gets the remaining sections of a given title.
     *
     * @param title the page title to be used including prefix
     */
    @GET(MW_PAGE_SECTIONS_URL)
    @NonNull Observable<Response<MwMobileViewPageRemaining>> getRemainingSections(@Nullable @Header("Cache-Control") String cacheControl,
                                                                                  @Nullable @Header(OfflineCacheInterceptor.SAVE_HEADER) String saveHeader,
                                                                                  @NonNull @Query("page") String title,
                                                                                  @Nullable @Query("uselang") String useLang);
    /**
     * TODO: remove this if we find a way to get the request url before the observable object being executed
     * Gets the remaining sections request url of a given title.
     *
     * @param title the page title to be used including prefix
     */
    @GET(MW_PAGE_SECTIONS_URL)
    @NonNull Call<MwMobileViewPageRemaining> getRemainingSectionsUrl(@Nullable @Header("Cache-Control") String cacheControl,
                                                                      @Nullable @Header(OfflineCacheInterceptor.SAVE_HEADER) String saveHeader,
                                                                      @NonNull @Query("page") String title,
                                                                      @Nullable @Query("uselang") String useLang);

    // ------- Search -------

    @GET(MW_API_PREFIX + "action=query&prop=pageimages&piprop=thumbnail"
            + "&converttitles=&pilicense=any&pithumbsize=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getPageImages(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&redirects="
            + "&converttitles=&prop=description|pageimages&piprop=thumbnail"
            + "&pilicense=any&generator=prefixsearch&gpsnamespace=0&list=search&srnamespace=0"
            + "&srwhat=text&srinfo=suggestion&srprop=&sroffset=0&srlimit=1&pithumbsize=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<PrefixSearchResponse> prefixSearch(@Query("gpssearch") String title,
                                                     @Query("gpslimit") int maxResults,
                                                     @Query("srsearch") String repeat);

    @GET(MW_API_PREFIX + "action=query&converttitles="
            + "&prop=description|pageimages|pageprops&ppprop=mainpage|disambiguation"
            + "&generator=search&gsrnamespace=0&gsrwhat=text"
            + "&gsrinfo=&gsrprop=redirecttitle&piprop=thumbnail&pilicense=any&pithumbsize="
            + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> fullTextSearch(@Query("gsrsearch") String searchTerm,
                                                  @Query("gsrlimit") int gsrLimit,
                                                  @Query("continue") String cont,
                                                  @Query("gsroffset") String gsrOffset);

    @GET(MW_API_PREFIX + "action=query&prop=coordinates|description|pageimages"
            + "&colimit=50&piprop=thumbnail&pilicense=any"
            + "&generator=geosearch&ggslimit=50&pithumbsize=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> nearbySearch(@NonNull @Query("ggscoord") String coord,
                                                @Query("ggsradius") double radius);


    // ------- Miscellaneous -------

    @GET(MW_API_PREFIX + "action=fancycaptchareload")
    @NonNull Observable<Captcha> getNewCaptcha();

    @GET(MW_API_PREFIX + "action=query&prop=langlinks&lllimit=500&redirects=&converttitles=")
    @NonNull Observable<MwQueryResponse> getLangLinks(@NonNull @Query("titles") String title);

    @GET(MW_API_PREFIX + "action=query&prop=description")
    @NonNull Observable<MwQueryResponse> getDescription(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo&iiprop=extmetadata")
    @NonNull Observable<MwQueryResponse> getImageExtMetadata(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname")
    @NonNull Observable<SiteMatrix> getSiteMatrix();

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo")
    @NonNull Observable<MwQueryResponse> getSiteInfo();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=0&grnlimit=50&prop=pageprops|description")
    @NonNull Observable<MwQueryResponse> getRandomWithPageProps();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&grnlimit=50"
            + "&prop=description|imageinfo&iiprop=timestamp|user|url&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getRandomWithImageInfo();

    @GET(MW_API_PREFIX + "action=query&prop=categories&clprop=hidden&cllimit=500")
    @NonNull Observable<MwQueryResponse> getCategories(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&list=categorymembers&cmlimit=500")
    @NonNull Observable<MwQueryResponse> getCategoryMembers(@NonNull @Query("cmtitle") String title,
                                                            @Nullable @Query("cmcontinue") String continueStr);


    // ------- CSRF, Login, and Create Account -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    @NonNull Call<MwQueryResponse> getCsrfToken();

    @SuppressWarnings("checkstyle:parameternumber")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=createaccount&createmessageformat=html")
    @NonNull Observable<CreateAccountResponse> postCreateAccount(@NonNull @Field("username") String user,
                                                           @NonNull @Field("password") String pass,
                                                           @NonNull @Field("retype") String retype,
                                                           @NonNull @Field("createtoken") String token,
                                                           @NonNull @Field("createreturnurl") String returnurl,
                                                           @Nullable @Field("email") String email,
                                                           @Nullable @Field("captchaId") String captchaId,
                                                           @Nullable @Field("captchaWord") String captchaWord);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=login")
    @NonNull Call<MwQueryResponse> getLoginToken();

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    @NonNull Call<LoginClient.LoginResponse> postLogIn(@Field("username") String user, @Field("password") String pass,
                                                       @Field("logintoken") String token, @Field("loginreturnurl") String url);

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    @NonNull Call<LoginClient.LoginResponse> postLogIn(@Field("username") String user, @Field("password") String pass,
                                                       @Field("retype") String retypedPass, @Field("OATHToken") String twoFactorCode,
                                                       @Field("logintoken") String token,
                                                       @Field("logincontinue") boolean loginContinue);

    @GET(MW_API_PREFIX + "action=query&meta=authmanagerinfo|tokens&amirequestsfor=create&type=createaccount")
    @NonNull Observable<MwQueryResponse> getAuthManagerInfo();

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&list=users&usprop=groups|cancreate")
    @NonNull Observable<MwQueryResponse> getUserInfo(@Query("ususers") @NonNull String userName);


    // ------- Notifications -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notformat=model&notlimit=max")
    @NonNull Observable<MwQueryResponse> getAllNotifications(@Query("notwikis") @Nullable String wikiList,
                                              @Query("notfilter") @Nullable String filter,
                                              @Query("notcontinue") @Nullable String continueStr);

    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=echomarkread")
    @NonNull Observable<MwQueryResponse> markRead(@Field("token") @NonNull String token, @Field("list") @Nullable String readList, @Field("unreadlist") @Nullable String unreadList);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notprop=list&notfilter=!read&notlimit=1")
    @NonNull Observable<MwQueryResponse> getLastUnreadNotification();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=unreadnotificationpages&unplimit=max&unpwikis=*")
    @NonNull Observable<MwQueryResponse> getUnreadNotificationWikis();


    // ------- User Options -------

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=options")
    @NonNull Observable<MwQueryResponse> getUserOptions();

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=options")
    @NonNull Observable<MwPostResponse> postUserOption(@Field("token") @NonNull String token,
                                                 @Query("optionname") @NonNull String key,
                                                 @Query("optionvalue") @Nullable String value);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=options")
    @NonNull Observable<MwPostResponse> deleteUserOption(@Field("token") @NonNull String token,
                                                   @Query("change") @NonNull String key);


    // ------- Editing -------

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=content|timestamp&rvlimit=1&converttitles=")
    @NonNull Call<MwQueryResponse> getWikiTextForSection(@NonNull @Query("titles") String title, @Query("rvsection") int section);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=parse&prop=text&sectionpreview=&pst=&mobileformat=")
    @NonNull Call<EditPreview> postEditPreview(@NonNull @Field("title") String title,
                                               @NonNull @Field("text") String text);

    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=edit&nocreate=")
    @SuppressWarnings("checkstyle:parameternumber")
    @NonNull Call<Edit> postEditSubmit(@NonNull @Field("title") String title,
                                       @Field("section") int section,
                                       @NonNull @Field("summary") String summary,
                                       @Nullable @Field("assert") String user,
                                       @NonNull @Field("text") String text,
                                       @Nullable @Field("basetimestamp") String baseTimeStamp,
                                       @NonNull @Field("token") String token,
                                       @Nullable @Field("captchaid") String captchaId,
                                       @Nullable @Field("captchaword") String captchaWord);


    // ------- Wikidata -------

    @GET(MW_API_PREFIX + "action=wbgetentities&props=labels&languagefallback=1")
    @NonNull Call<Entities> getWikidataLabels(@Query("ids") @NonNull String idList,
                                              @Query("languages") @NonNull String langList);

    @GET(MW_API_PREFIX + "action=wbgetentities&props=descriptions|labels|sitelinks")
    @NonNull Observable<Entities> getWikidataLabelsAndDescriptions(@Query("ids") @NonNull String idList);

    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=wbsetdescription&errorlang=uselang")
    @FormUrlEncoded
    Observable<MwPostResponse> postDescriptionEdit(@NonNull @Field("language") String language,
                                                   @NonNull @Field("uselang") String useLang,
                                                   @NonNull @Field("site") String site,
                                                   @NonNull @Field("title") String title,
                                                   @NonNull @Field("value") String newDescription,
                                                   @NonNull @Field("token") String token,
                                                   @Nullable @Field("assert") String user);

    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=wbsetlabel&errorlang=uselang")
    @FormUrlEncoded
    Observable<MwPostResponse> postLabelEdit(@NonNull @Field("language") String language,
                                             @NonNull @Field("uselang") String useLang,
                                             @NonNull @Field("site") String site,
                                             @NonNull @Field("title") String title,
                                             @NonNull @Field("value") String newDescription,
                                             @NonNull @Field("token") String token,
                                             @Nullable @Field("assert") String user);
}
