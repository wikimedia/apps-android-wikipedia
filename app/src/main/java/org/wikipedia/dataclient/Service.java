package org.wikipedia.dataclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;

import org.wikipedia.captcha.Captcha;
import org.wikipedia.dataclient.mwapi.CreateAccountResponse;
import org.wikipedia.dataclient.mwapi.MwParseResponse;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.SiteMatrix;
import org.wikipedia.edit.Edit;
import org.wikipedia.edit.preview.EditPreview;
import org.wikipedia.login.LoginClient;
import org.wikipedia.search.PrefixSearchResponse;
import org.wikipedia.wikidata.Entities;

import io.reactivex.Observable;
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

    String MW_API_PREFIX = "w/api.php?format=json&formatversion=2&errorformat=plaintext&";

    int PREFERRED_THUMB_SIZE = 320;


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

    @GET(MW_API_PREFIX + "action=query&prop=description")
    @NonNull Observable<MwQueryResponse> getDescription(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo&iiprop=timestamp|user|url|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getImageExtMetadata(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&prop=videoinfo&viprop=timestamp|user|url|mime|extmetadata|derivatives&viurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getMediaInfo(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname")
    @NonNull Observable<SiteMatrix> getSiteMatrix();

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo")
    @NonNull Observable<MwQueryResponse> getSiteInfo();

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1&mainpage=1")
    @NonNull Observable<MwParseResponse> parseTextForMainPage(@NonNull @Query("page") String mainPageTitle);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=0&grnlimit=50&prop=pageprops|description")
    @NonNull Observable<MwQueryResponse> getRandomWithPageProps();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&grnlimit=50"
            + "&prop=description|imageinfo&iiprop=timestamp|user|url|mime&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getRandomWithImageInfo();

    @GET(MW_API_PREFIX + "action=query&prop=categories&clprop=hidden&cllimit=500")
    @NonNull Observable<MwQueryResponse> getCategories(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&list=categorymembers&cmlimit=500")
    @NonNull Observable<MwQueryResponse> getCategoryMembers(@NonNull @Query("cmtitle") String title,
                                                            @Nullable @Query("cmcontinue") String continueStr);

    @GET(MW_API_PREFIX + "action=query&generator=unreviewedimagelabels&guillimit=10&prop=imagelabels|imageinfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @NonNull Observable<MwQueryResponse> getImagesWithUnreviewedLabels();


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

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    @NonNull Observable<LoginClient.LoginResponse> postLogIn(@Field("username") String user,
                                                             @Field("password") String pass,
                                                             @Field("logintoken") String token,
                                                             @Field("loginreturnurl") String url);

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    @NonNull Observable<LoginClient.LoginResponse> postLogIn(@Field("username") String user,
                                                             @Field("password") String pass,
                                                             @Field("retype") String retypedPass,
                                                             @Field("OATHToken") String twoFactorCode,
                                                             @Field("logintoken") String token,
                                                             @Field("logincontinue") boolean loginContinue);

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=logout")
    @NonNull Observable<MwPostResponse> postLogout(@NonNull @Field("token") String token);

    @GET(MW_API_PREFIX + "action=query&meta=authmanagerinfo|tokens&amirequestsfor=create&type=createaccount")
    @NonNull Observable<MwQueryResponse> getAuthManagerInfo();

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=groups|blockinfo")
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
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=echomarkread")
    @NonNull Observable<MwQueryResponse> markRead(@Field("token") @NonNull String token, @Field("list") @Nullable String readList, @Field("unreadlist") @Nullable String unreadList);

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notprop=list&notfilter=!read&notlimit=1")
    @NonNull Observable<MwQueryResponse> getLastUnreadNotification();

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=unreadnotificationpages&unplimit=max&unpwikis=*")
    @NonNull Observable<MwQueryResponse> getUnreadNotificationWikis();

    // ------- Editing -------

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=content|timestamp&rvlimit=1&converttitles=")
    @NonNull Observable<MwQueryResponse> getWikiTextForSection(@NonNull @Query("titles") String title, @Query("rvsection") int section);

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=parse&prop=text&sectionpreview=&pst=&mobileformat=")
    @NonNull Observable<EditPreview> postEditPreview(@NonNull @Field("title") String title,
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

    @GET(MW_API_PREFIX + "action=query&list=usercontribs")
    @NonNull Observable<MwQueryResponse> getUserContributions(@NonNull @Query("ucuser") String username);

    @GET(MW_API_PREFIX + "action=query&prop=pageviews")
    @NonNull Observable<MwQueryResponse> getPageViewsForTitles(@NonNull @Query("titles") String titles);

    @GET(MW_API_PREFIX + "action=query&meta=wikimediaeditortaskscounts|userinfo")
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

    @GET(MW_API_PREFIX + "action=wbgetentities&props=labels&languagefallback=1")
    @NonNull Call<Entities> getWikidataLabels(@Query("ids") @NonNull String idList,
                                              @Query("languages") @NonNull String langList);

    @GET(MW_API_PREFIX + "action=wbgetentities&props=descriptions|labels|sitelinks")
    @NonNull Observable<Entities> getWikidataLabelsAndDescriptions(@Query("ids") @NonNull String idList);

    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=wbsetdescription&errorlang=uselang")
    @FormUrlEncoded
    @SuppressWarnings("checkstyle:parameternumber")
    Observable<MwPostResponse> postDescriptionEdit(@NonNull @Field("language") String language,
                                                   @NonNull @Field("uselang") String useLang,
                                                   @NonNull @Field("site") String site,
                                                   @NonNull @Field("title") String title,
                                                   @NonNull @Field("value") String newDescription,
                                                   @Nullable @Field("summary") String summary,
                                                   @NonNull @Field("token") String token,
                                                   @Nullable @Field("assert") String user);

    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=wbsetlabel&errorlang=uselang")
    @FormUrlEncoded
    @SuppressWarnings("checkstyle:parameternumber")
    Observable<MwPostResponse> postLabelEdit(@NonNull @Field("language") String language,
                                             @NonNull @Field("uselang") String useLang,
                                             @NonNull @Field("site") String site,
                                             @NonNull @Field("title") String title,
                                             @NonNull @Field("value") String newDescription,
                                             @Nullable @Field("summary") String summary,
                                             @NonNull @Field("token") String token,
                                             @Nullable @Field("assert") String user);
}
