package org.wikipedia.dataclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;

import org.wikipedia.captcha.Captcha;
import org.wikipedia.dataclient.mwapi.CreateAccountResponse;
import org.wikipedia.dataclient.mwapi.MwParseResponse;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse;
import org.wikipedia.dataclient.mwapi.SiteMatrix;
import org.wikipedia.dataclient.watch.WatchPostResponse;
import org.wikipedia.dataclient.wikidata.Claims;
import org.wikipedia.dataclient.wikidata.Entities;
import org.wikipedia.dataclient.wikidata.EntityPostResponse;
import org.wikipedia.dataclient.wikidata.Search;
import org.wikipedia.edit.Edit;
import org.wikipedia.login.LoginClient;
import org.wikipedia.search.PrefixSearchResponse;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
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
    String URL_FRAGMENT_FROM_COMMONS = "/wikipedia/commons/";

    String MW_API_PREFIX = "w/api.php?format=json&formatversion=2&errorformat=html&errorsuselocal=1&";

    int PREFERRED_THUMB_SIZE = 320;

    // Maximum cache time for site-specific data, and other things not likely to change very often.
    int SITE_INFO_MAXAGE = 86400;


    // ------- Search -------

    @GET(MW_API_PREFIX + "action=query&prop=pageimages&piprop=thumbnail"
            + "&converttitles=&pilicense=any&pithumbsize=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getPageImages(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&redirects="
            + "&converttitles=&prop=description|pageimages|info&piprop=thumbnail"
            + "&pilicense=any&generator=prefixsearch&gpsnamespace=0&list=search&srnamespace=0"
            + "&inprop=varianttitles"
            + "&srwhat=text&srinfo=suggestion&srprop=&sroffset=0&srlimit=1&pithumbsize=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<PrefixSearchResponse> prefixSearch(@Query("gpssearch") String title,
                                                     @Query("gpslimit") int maxResults,
                                                     @Query("srsearch") String repeat);

    @GET(MW_API_PREFIX + "action=query&converttitles="
            + "&prop=description|pageimages|pageprops|info&ppprop=mainpage|disambiguation"
            + "&generator=search&gsrnamespace=0&gsrwhat=text"
            + "&inprop=varianttitles"
            + "&gsrinfo=&gsrprop=redirecttitle&piprop=thumbnail&pilicense=any&pithumbsize="
            + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> fullTextSearch(@Query("gsrsearch") String searchTerm,
                                                  @Query("gsrlimit") int gsrLimit,
                                                  @Query("continue") String cont,
                                                  @Query("gsroffset") String gsrOffset);


    // ------- Miscellaneous -------

    @GET(MW_API_PREFIX + "action=fancycaptchareload")
    @NonNull Observable<Captcha> getNewCaptcha();

    @GET(MW_API_PREFIX + "action=query&prop=langlinks&lllimit=500&redirects=&converttitles=")
    @NonNull Observable<MwQueryResponse> getLangLinks(@NonNull @Query("titles") String title);

    @GET(MW_API_PREFIX + "action=query&prop=description&redirects=1")
    @NonNull Observable<MwQueryResponse> getDescription(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&prop=info|description&inprop=varianttitles&redirects=1")
    @NonNull Observable<MwQueryResponse> getInfoByPageId(@NonNull @Query("pageids") String pageIds);

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo|imagelabels&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getImageInfo(@NonNull @Query("titles") String titles,
                                                      @NonNull @Query("iiextmetadatalanguage") String lang);

    @GET(MW_API_PREFIX + "action=query&prop=videoinfo|imagelabels&viprop=timestamp|user|url|mime|extmetadata|derivatives&viurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getVideoInfo(@NonNull @Query("titles") String titles,
                                                      @NonNull @Query("viextmetadatalanguage") String lang);

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&prop=info&inprop=protection&uiprop=groups")
    @NonNull Observable<MwQueryResponse> getProtectionInfo(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    @NonNull Observable<SiteMatrix> getSiteMatrix();

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    @NonNull Observable<MwQueryResponse> getSiteInfo();

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1")
    @NonNull Observable<MwParseResponse> parsePage(@NonNull @Query("page") String pageTitle);

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1")
    @NonNull Observable<MwParseResponse> parseText(@NonNull @Query("text") String text);

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1&mainpage=1")
    @NonNull Observable<MwParseResponse> parseTextForMainPage(@NonNull @Query("page") String mainPageTitle);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=0&grnlimit=50&prop=pageprops|description")
    @NonNull Observable<MwQueryResponse> getRandomWithPageProps();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&grnlimit=100&prop=imagelabels")
    @NonNull Observable<MwQueryResponse> getRandomWithImageLabels();

    @GET(MW_API_PREFIX + "action=query&prop=description|pageimages")
    @NonNull Observable<MwQueryResponse> getImagesAndThumbnails(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&prop=categories&clprop=hidden&cllimit=500")
    @NonNull Observable<MwQueryResponse> getCategories(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&list=categorymembers&cmlimit=500")
    @NonNull Observable<MwQueryResponse> getCategoryMembers(@NonNull @Query("cmtitle") String title,
                                                            @Nullable @Query("cmcontinue") String continueStr);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&grnlimit=10&prop=description|imageinfo|revisions&rvprop=ids|timestamp|flags|comment|user|content&rvslots=mediainfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getRandomWithImageInfo();

    @GET(MW_API_PREFIX + "action=query&generator=unreviewedimagelabels&guillimit=10&prop=imagelabels|imageinfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getImagesWithUnreviewedLabels(@NonNull @Query("uselang") String lang);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=options")
    @NonNull Observable<MwPostResponse> postSetOptions(@NonNull @Field("change") String change,
                                                       @NonNull @Field("token") String token);

    @GET(MW_API_PREFIX + "action=streamconfigs&format=json&constraints=destination_event_service=eventgate-analytics-external")
    @NonNull Observable<MwStreamConfigsResponse> getStreamConfigs();

    // ------- CSRF, Login, and Create Account -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    @NonNull Call<MwQueryResponse> getCsrfTokenCall();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    @NonNull Observable<MwQueryResponse> getCsrfToken();

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
    @NonNull Observable<JsonElement> getLoginToken();

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    @NonNull Observable<LoginClient.LoginResponse> postLogIn(@Field("username") String user,
                                                             @Field("password") String pass,
                                                             @Field("logintoken") String token,
                                                             @Field("loginreturnurl") String url);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    @NonNull Observable<LoginClient.LoginResponse> postLogIn(@Field("username") String user,
                                                             @Field("password") String pass,
                                                             @Field("retype") String retypedPass,
                                                             @Field("OATHToken") String twoFactorCode,
                                                             @Field("logintoken") String token,
                                                             @Field("logincontinue") boolean loginContinue);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=logout")
    @NonNull Observable<MwPostResponse> postLogout(@NonNull @Field("token") String token);

    @GET(MW_API_PREFIX + "action=query&meta=authmanagerinfo|tokens&amirequestsfor=create&type=createaccount")
    @NonNull Observable<MwQueryResponse> getAuthManagerInfo();

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=groups|blockinfo|editcount|latestcontrib")
    @NonNull Observable<MwQueryResponse> getUserInfo();

    @GET(MW_API_PREFIX + "action=query&list=users&usprop=groups|cancreate")
    @NonNull Observable<MwQueryResponse> getUserList(@Query("ususers") @NonNull String userNames);


    // ------- Notifications -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notformat=model&notlimit=max")
    @NonNull Observable<MwQueryResponse> getAllNotifications(@Query("notwikis") @Nullable String wikiList,
                                              @Query("notfilter") @Nullable String filter,
                                              @Query("notcontinue") @Nullable String continueStr);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echomarkread")
    @NonNull Observable<MwQueryResponse> markRead(@Field("token") @NonNull String token, @Field("list") @Nullable String readList, @Field("unreadlist") @Nullable String unreadList);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notwikis=*&notprop=list&notfilter=!read&notlimit=1")
    @NonNull Observable<MwQueryResponse> getLastUnreadNotification();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=unreadnotificationpages&unplimit=max&unpwikis=*")
    @NonNull Observable<MwQueryResponse> getUnreadNotificationWikis();

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echopushsubscriptions&command=create&provider=fcm")
    @NonNull Observable<MwQueryResponse> subscribePush(@Field("token") @NonNull String csrfToken,
                                                       @Field("providertoken") @NonNull String providerToken);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echopushsubscriptions&command=delete&provider=fcm")
    @NonNull Observable<MwQueryResponse> unsubscribePush(@Field("token") @NonNull String csrfToken,
                                                         @Field("providertoken") @NonNull String providerToken);

    // ------- Editing -------

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=content|timestamp|ids&rvlimit=1&converttitles=")
    @NonNull Observable<MwQueryResponse> getWikiTextForSection(@NonNull @Query("titles") String title, @Query("rvsection") int section);

    @GET(MW_API_PREFIX + "action=query&prop=revisions|info&rvprop=content|timestamp|ids&rvlimit=1&converttitles=&intestactions=edit&intestactionsdetail=full")
    @NonNull Observable<MwQueryResponse> getWikiTextForSectionWithInfo(@NonNull @Query("titles") String title, @Query("rvsection") int section);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    @NonNull Observable<Edit> postUndoEdit(@NonNull @Field("title") String title,
                                           @Field("undo") long revision,
                                           @NonNull @Field("token") String token);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    @SuppressWarnings("checkstyle:parameternumber")
    @NonNull Observable<Edit> postEditSubmit(@NonNull @Field("title") String title,
                                             @NonNull @Field("section") String section,
                                             @Nullable @Field("sectiontitle") String newSectionTitle,
                                             @NonNull @Field("summary") String summary,
                                             @Nullable @Field("assert") String user,
                                             @Nullable @Field("text") String text,
                                             @Nullable @Field("appendtext") String appendText,
                                             @Field("baserevid") long baseRevId,
                                             @NonNull @Field("token") String token,
                                             @Nullable @Field("captchaid") String captchaId,
                                             @Nullable @Field("captchaword") String captchaWord);

    @GET(MW_API_PREFIX + "action=query&list=usercontribs&ucprop=ids|title|timestamp|comment|size|flags|sizediff|tags&meta=userinfo&uiprop=groups|blockinfo|editcount|latestcontrib")
    @NonNull Observable<MwQueryResponse> getUserContributions(@NonNull @Query("ucuser") String username, @Query("uclimit") int maxCount, @Query("uccontinue") String uccontinue);

    @GET(MW_API_PREFIX + "action=query&prop=pageviews")
    @NonNull Observable<MwQueryResponse> getPageViewsForTitles(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&meta=wikimediaeditortaskscounts|userinfo&uiprop=groups|blockinfo|editcount|latestcontrib")
    @NonNull Observable<MwQueryResponse> getEditorTaskCounts();

    @GET(MW_API_PREFIX + "action=query&generator=wikimediaeditortaskssuggestions&prop=pageprops&gwetstask=missingdescriptions&gwetslimit=3")
    @NonNull Observable<MwQueryResponse> getEditorTaskMissingDescriptions(@NonNull @Query("gwetstarget") String targetLanguage);

    @GET(MW_API_PREFIX + "action=query&generator=wikimediaeditortaskssuggestions&prop=pageprops&gwetstask=descriptiontranslations&gwetslimit=3")
    @NonNull Observable<MwQueryResponse> getEditorTaskTranslatableDescriptions(@NonNull @Query("gwetssource") String sourceLanguage,
                                                                               @NonNull @Query("gwetstarget") String targetLanguage);


    // ------- Wikidata -------

    @GET(MW_API_PREFIX + "action=wbgetentities")
    @NonNull Observable<Entities> getEntitiesByTitle(@Query("titles") @NonNull String titles,
                                                     @Query("sites") @NonNull String sites);

    @GET(MW_API_PREFIX + "action=wbsearchentities&type=item&limit=20")
    @NonNull Observable<Search> searchEntities(@Query("search") @NonNull String searchTerm,
                                               @Query("language") @NonNull String searchLang,
                                               @Query("uselang") @NonNull String resultLang);

    @GET(MW_API_PREFIX + "action=wbgetentities&props=labels&languagefallback=1")
    @NonNull Observable<Entities> getWikidataLabels(@Query("ids") @NonNull String idList,
                                                    @Query("languages") @NonNull String langList);

    @GET(MW_API_PREFIX + "action=wbgetclaims") @NonNull
    Observable<Claims> getClaims(@Query("entity") @NonNull String entity,
                                 @Query("property") @Nullable String property);

    @GET(MW_API_PREFIX + "action=wbgetentities&props=descriptions|labels|sitelinks")
    @NonNull Observable<Entities> getWikidataLabelsAndDescriptions(@Query("ids") @NonNull String idList);

    @POST(MW_API_PREFIX + "action=wbsetclaim&errorlang=uselang")
    @FormUrlEncoded
    Observable<MwPostResponse> postSetClaim(@NonNull @Field("claim") String claim,
                                            @NonNull @Field("token") String token,
                                            @Nullable @Field("summary") String summary,
                                            @Nullable @Field("tags") String tags);

    @POST(MW_API_PREFIX + "action=wbsetdescription&errorlang=uselang")
    @FormUrlEncoded
    @SuppressWarnings("checkstyle:parameternumber")
    Observable<EntityPostResponse> postDescriptionEdit(@NonNull @Field("language") String language,
                                                   @NonNull @Field("uselang") String useLang,
                                                   @NonNull @Field("site") String site,
                                                   @NonNull @Field("title") String title,
                                                   @NonNull @Field("value") String newDescription,
                                                   @Nullable @Field("summary") String summary,
                                                   @NonNull @Field("token") String token,
                                                   @Nullable @Field("assert") String user);

    @POST(MW_API_PREFIX + "action=wbsetlabel&errorlang=uselang")
    @FormUrlEncoded
    @SuppressWarnings("checkstyle:parameternumber")
    Observable<EntityPostResponse> postLabelEdit(@NonNull @Field("language") String language,
                                             @NonNull @Field("uselang") String useLang,
                                             @NonNull @Field("site") String site,
                                             @NonNull @Field("title") String title,
                                             @NonNull @Field("value") String newDescription,
                                             @Nullable @Field("summary") String summary,
                                             @NonNull @Field("token") String token,
                                             @Nullable @Field("assert") String user);

    @POST(MW_API_PREFIX + "action=wbeditentity&errorlang=uselang")
    @FormUrlEncoded
    Observable<EntityPostResponse> postEditEntity(@NonNull @Field("id") String id,
                                              @NonNull @Field("token") String token,
                                              @Nullable @Field("data") String data,
                                              @Nullable @Field("summary") String summary,
                                              @Nullable @Field("tags") String tags);


    @POST(MW_API_PREFIX + "action=reviewimagelabels")
    @FormUrlEncoded
    Observable<MwPostResponse> postReviewImageLabels(@NonNull @Field("filename") String fileName,
                                                     @NonNull @Field("token") String token,
                                                     @NonNull @Field("batch") String batchLabels);
    // ------- Watchlist -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched")
    @NonNull Observable<MwQueryResponse> getWatchedInfo(@NonNull @Query("titles") String titles);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&list=watchlist&wllimit=500&wlallrev=1&wlprop=ids|title|flags|comment|parsedcomment|timestamp|sizes|user|loginfo")

    @NonNull Observable<MwQueryResponse> getWatchlist();

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=timestamp|user|ids|comment|tags")
    @NonNull Observable<MwQueryResponse> getLastModified(@Query("titles") @NonNull String titles);

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=ids|timestamp|flags|comment|user&rvlimit=2&rvdir=newer")
    @NonNull Observable<MwQueryResponse> getRevisionDetails(@Query("titles") @NonNull String titles,
                                                            @Query("rvstartid") @NonNull Long revisionStartId);

    @POST(MW_API_PREFIX + "action=thank")
    @FormUrlEncoded
    Observable<EntityPostResponse> postThanksToRevision(@Field("rev") long revisionId,
                                                        @NonNull @Field("token") String token);

    @POST(MW_API_PREFIX + "action=watch&converttitles=&redirects=")
    @FormUrlEncoded
    Observable<WatchPostResponse> postWatch(@Nullable @Field("unwatch") Integer unwatch,
                                            @Nullable @Field("pageids") String pageIds,
                                            @Nullable @Field("titles") String titles,
                                            @Nullable @Field("expiry") String expiry,
                                            @NonNull @Field("token") String token);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=watch")
    @NonNull Observable<MwQueryResponse> getWatchToken();
}
