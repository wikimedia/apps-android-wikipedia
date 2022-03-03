package org.wikipedia.dataclient

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.captcha.Captcha
import org.wikipedia.dataclient.mwapi.*
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.dataclient.wikidata.Claims
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.dataclient.wikidata.Search
import org.wikipedia.edit.Edit
import org.wikipedia.login.LoginClient.LoginResponse
import org.wikipedia.search.PrefixSearchResponse
import retrofit2.Call
import retrofit2.http.*

/**
 * Retrofit service layer for all API interactions, including regular MediaWiki and RESTBase.
 */
interface Service {

    // ------- Search -------

    @GET(
        MW_API_PREFIX + "action=query&prop=pageimages&piprop=thumbnail" +
                "&converttitles=&pilicense=any&pithumbsize=" + PREFERRED_THUMB_SIZE
    )
    fun getPageImages(@Query("titles") titles: String): Observable<MwQueryResponse>

    @GET(
        MW_API_PREFIX + "action=query&redirects=" +
                "&converttitles=&prop=description|pageimages|info&piprop=thumbnail" +
                "&pilicense=any&generator=prefixsearch&gpsnamespace=0&list=search&srnamespace=0" +
                "&inprop=varianttitles" +
                "&srwhat=text&srinfo=suggestion&srprop=&sroffset=0&srlimit=1&pithumbsize=" + PREFERRED_THUMB_SIZE
    )
    fun prefixSearch(
        @Query("gpssearch") title: String?,
        @Query("gpslimit") maxResults: Int,
        @Query("srsearch") repeat: String?
    ): Observable<PrefixSearchResponse>

    @GET(
        MW_API_PREFIX + "action=query&converttitles=" +
                "&prop=description|pageimages|pageprops|info&ppprop=mainpage|disambiguation" +
                "&generator=search&gsrnamespace=0&gsrwhat=text" +
                "&inprop=varianttitles" +
                "&gsrinfo=&gsrprop=redirecttitle&piprop=thumbnail&pilicense=any&pithumbsize=" +
                PREFERRED_THUMB_SIZE
    )
    fun fullTextSearch(
        @Query("gsrsearch") searchTerm: String?,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("continue") cont: String?,
        @Query("gsroffset") gsrOffset: String?
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&list=allusers&auwitheditsonly=1")
    fun prefixSearchUsers(
            @Query("auprefix") prefix: String,
            @Query("aulimit") maxResults: Int
    ): Observable<MwQueryResponse>

    // ------- Miscellaneous -------

    @get:GET(MW_API_PREFIX + "action=fancycaptchareload")
    val newCaptcha: Observable<Captcha>

    @GET(MW_API_PREFIX + "action=query&prop=langlinks&lllimit=500&redirects=&converttitles=")
    fun getLangLinks(@Query("titles") title: String): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=description&redirects=1")
    fun getDescription(@Query("titles") titles: String): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=info|description&inprop=varianttitles&redirects=1")
    fun getInfoByPageId(@Query("pageids") pageIds: String): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo|imagelabels&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    fun getImageInfo(
        @Query("titles") titles: String,
        @Query("iiextmetadatalanguage") lang: String
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=videoinfo|imagelabels&viprop=timestamp|user|url|mime|extmetadata|derivatives&viurlwidth=" + PREFERRED_THUMB_SIZE)
    fun getVideoInfo(
        @Query("titles") titles: String,
        @Query("viextmetadatalanguage") lang: String
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&prop=info&inprop=protection&uiprop=groups")
    fun getProtectionInfo(@Query("titles") titles: String): Observable<MwQueryResponse>

    @get:GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    val siteMatrix: Observable<SiteMatrix>

    @GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    suspend fun getSiteMatrix(): SiteMatrix

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&siprop=namespaces")
    fun getPageNamespaceWithSiteInfo(@Query("titles") title: String): Observable<MwQueryResponse>

    @get:GET(MW_API_PREFIX + "action=query&meta=siteinfo&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    val siteInfo: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1")
    fun parsePage(@Query("page") pageTitle: String): Observable<MwParseResponse>

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1")
    fun parseText(@Query("text") text: String): Observable<MwParseResponse>

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1&mainpage=1")
    fun parseTextForMainPage(@Query("page") mainPageTitle: String): Observable<MwParseResponse>

    @get:GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=0&grnlimit=50&prop=pageprops|description")
    @get:Headers("Cache-Control: no-cache")
    val randomWithPageProps: Observable<MwQueryResponse>

    @get:GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&grnlimit=100&prop=imagelabels")
    @get:Headers("Cache-Control: no-cache")
    val randomWithImageLabels: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=categories&clprop=hidden&cllimit=500")
    fun getCategories(@Query("titles") titles: String): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&list=categorymembers&cmlimit=500")
    fun getCategoryMembers(
        @Query("cmtitle") title: String,
        @Query("cmcontinue") continueStr: String?
    ): Observable<MwQueryResponse>

    @get:GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&grnlimit=10&prop=description|imageinfo|revisions&rvprop=ids|timestamp|flags|comment|user|content&rvslots=mediainfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @get:Headers("Cache-Control: no-cache")
    val randomWithImageInfo: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&generator=unreviewedimagelabels&guillimit=10&prop=imagelabels|imageinfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    fun getImagesWithUnreviewedLabels(@Query("uselang") lang: String): Observable<MwQueryResponse>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=options")
    fun postSetOptions(
        @Field("change") change: String,
        @Field("token") token: String
    ): Observable<MwPostResponse>

    @get:GET(MW_API_PREFIX + "action=streamconfigs&format=json&constraints=destination_event_service=eventgate-analytics-external")
    val streamConfigs: Observable<MwStreamConfigsResponse>

    // ------- CSRF, Login, and Create Account -------

    @get:GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    @get:Headers("Cache-Control: no-cache")
    val csrfTokenCall: Call<MwQueryResponse?>

    @get:GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    @get:Headers("Cache-Control: no-cache")
    val csrfToken: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    @Headers("Cache-Control: no-cache")
    suspend fun getCsrfToken(): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=createaccount&createmessageformat=html")
    fun postCreateAccount(
        @Field("username") user: String,
        @Field("password") pass: String,
        @Field("retype") retype: String,
        @Field("createtoken") token: String,
        @Field("createreturnurl") returnurl: String,
        @Field("email") email: String?,
        @Field("captchaId") captchaId: String?,
        @Field("captchaWord") captchaWord: String?
    ): Observable<CreateAccountResponse>

    @get:GET(MW_API_PREFIX + "action=query&meta=tokens&type=login")
    @get:Headers("Cache-Control: no-cache")
    val loginToken: Observable<MwQueryResponse>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    fun postLogIn(
        @Field("username") user: String?,
        @Field("password") pass: String?,
        @Field("logintoken") token: String?,
        @Field("loginreturnurl") url: String?
    ): Observable<LoginResponse>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    fun postLogIn(
        @Field("username") user: String?,
        @Field("password") pass: String?,
        @Field("retype") retypedPass: String?,
        @Field("OATHToken") twoFactorCode: String?,
        @Field("logintoken") token: String?,
        @Field("logincontinue") loginContinue: Boolean
    ): Observable<LoginResponse>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=logout")
    fun postLogout(@Field("token") token: String): Observable<MwPostResponse>

    @get:GET(MW_API_PREFIX + "action=query&meta=authmanagerinfo|tokens&amirequestsfor=create&type=createaccount")
    val authManagerInfo: Observable<MwQueryResponse>

    @get:GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=groups|blockinfo|editcount|latestcontrib|hasmsg")
    val userInfo: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&list=users&usprop=groups|cancreate")
    fun getUserList(@Query("ususers") userNames: String): Observable<MwQueryResponse>

    // ------- Notifications -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notformat=model&notlimit=max")
    suspend fun getAllNotifications(
        @Query("notwikis") wikiList: String?,
        @Query("notfilter") filter: String?,
        @Query("notcontinue") continueStr: String?
    ): MwQueryResponse

    // TODO: remove "KT" if we remove the Observable one.
    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notformat=model&notlimit=max")
    suspend fun getAllNotificationsKT(
        @Query("notwikis") wikiList: String?,
        @Query("notfilter") filter: String?,
        @Query("notcontinue") continueStr: String?
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echomarkread")
    fun markRead(
        @Field("token") token: String,
        @Field("list") readList: String?,
        @Field("unreadlist") unreadList: String?
    ): Observable<MwQueryResponse>

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notwikis=*&notprop=list&notfilter=!read&notlimit=1")
    suspend fun lastUnreadNotification(): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=unreadnotificationpages&unplimit=max&unpwikis=*")
    suspend fun unreadNotificationWikis(): MwQueryResponse

    // TODO: remove "KT" if we remove the Observable one.
    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=unreadnotificationpages&unplimit=max&unpwikis=*")
    suspend fun unreadNotificationWikisKT(): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echopushsubscriptions&command=create&provider=fcm")
    fun subscribePush(
        @Field("token") csrfToken: String,
        @Field("providertoken") providerToken: String
    ): Observable<MwQueryResponse>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echopushsubscriptions&command=delete&provider=fcm")
    fun unsubscribePush(
        @Field("token") csrfToken: String,
        @Field("providertoken") providerToken: String
    ): Observable<MwQueryResponse>

    // ------- Editing -------

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=content|timestamp|ids&rvlimit=1&converttitles=")
    fun getWikiTextForSection(
        @Query("titles") title: String,
        @Query("rvsection") section: Int
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=revisions|info&rvprop=content|timestamp|ids&rvlimit=1&converttitles=&intestactions=edit&intestactionsdetail=full")
    fun getWikiTextForSectionWithInfo(
        @Query("titles") title: String,
        @Query("rvsection") section: Int
    ): Observable<MwQueryResponse>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    fun postUndoEdit(
        @Field("title") title: String,
        @Field("undo") revision: Long,
        @Field("token") token: String
    ): Observable<Edit>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    fun postEditSubmit(
        @Field("title") title: String,
        @Field("section") section: String,
        @Field("sectiontitle") newSectionTitle: String?,
        @Field("summary") summary: String,
        @Field("assert") user: String?,
        @Field("text") text: String?,
        @Field("appendtext") appendText: String?,
        @Field("baserevid") baseRevId: Long,
        @Field("token") token: String,
        @Field("captchaid") captchaId: String?,
        @Field("captchaword") captchaWord: String?
    ): Observable<Edit>

    @GET(MW_API_PREFIX + "action=query&list=usercontribs&ucprop=ids|title|timestamp|comment|size|flags|sizediff|tags&meta=userinfo&uiprop=groups|blockinfo|editcount|latestcontrib")
    fun getUserContributions(
        @Query("ucuser") username: String,
        @Query("uclimit") maxCount: Int,
        @Query("uccontinue") uccontinue: String?
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=pageviews")
    fun getPageViewsForTitles(@Query("titles") titles: String): Observable<MwQueryResponse>

    @get:GET(MW_API_PREFIX + "action=query&meta=wikimediaeditortaskscounts|userinfo&uiprop=groups|blockinfo|editcount|latestcontrib")
    val editorTaskCounts: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&generator=wikimediaeditortaskssuggestions&prop=pageprops&gwetstask=missingdescriptions&gwetslimit=3")
    fun getEditorTaskMissingDescriptions(@Query("gwetstarget") targetLanguage: String): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&generator=wikimediaeditortaskssuggestions&prop=pageprops&gwetstask=descriptiontranslations&gwetslimit=3")
    fun getEditorTaskTranslatableDescriptions(
        @Query("gwetssource") sourceLanguage: String,
        @Query("gwetstarget") targetLanguage: String
    ): Observable<MwQueryResponse>

    // ------- Wikidata -------

    @GET(MW_API_PREFIX + "action=wbgetentities")
    fun getEntitiesByTitle(
        @Query("titles") titles: String,
        @Query("sites") sites: String
    ): Observable<Entities>

    @GET(MW_API_PREFIX + "action=wbsearchentities&type=item&limit=20")
    fun searchEntities(
        @Query("search") searchTerm: String,
        @Query("language") searchLang: String,
        @Query("uselang") resultLang: String
    ): Observable<Search>

    @GET(MW_API_PREFIX + "action=wbgetentities&props=labels&languagefallback=1")
    fun getWikidataLabels(
        @Query("ids") idList: String,
        @Query("languages") langList: String
    ): Observable<Entities>

    @GET(MW_API_PREFIX + "action=wbgetclaims")
    fun getClaims(
        @Query("entity") entity: String,
        @Query("property") property: String?
    ): Observable<Claims>

    @GET(MW_API_PREFIX + "action=wbgetentities&props=descriptions|labels|sitelinks")
    fun getWikidataLabelsAndDescriptions(@Query("ids") idList: String): Observable<Entities>

    @POST(MW_API_PREFIX + "action=wbsetclaim&errorlang=uselang")
    @FormUrlEncoded
    fun postSetClaim(
        @Field("claim") claim: String,
        @Field("token") token: String,
        @Field("summary") summary: String?,
        @Field("tags") tags: String?
    ): Observable<MwPostResponse>

    @POST(MW_API_PREFIX + "action=wbsetdescription&errorlang=uselang")
    @FormUrlEncoded
    fun postDescriptionEdit(
        @Field("language") language: String,
        @Field("uselang") useLang: String,
        @Field("site") site: String,
        @Field("title") title: String,
        @Field("value") newDescription: String,
        @Field("summary") summary: String?,
        @Field("token") token: String,
        @Field("assert") user: String?
    ): Observable<EntityPostResponse>

    @POST(MW_API_PREFIX + "action=wbsetlabel&errorlang=uselang")
    @FormUrlEncoded
    fun postLabelEdit(
        @Field("language") language: String,
        @Field("uselang") useLang: String,
        @Field("site") site: String,
        @Field("title") title: String,
        @Field("value") newDescription: String,
        @Field("summary") summary: String?,
        @Field("token") token: String,
        @Field("assert") user: String?
    ): Observable<EntityPostResponse>

    @POST(MW_API_PREFIX + "action=wbeditentity&errorlang=uselang")
    @FormUrlEncoded
    fun postEditEntity(
        @Field("id") id: String,
        @Field("token") token: String,
        @Field("data") data: String?,
        @Field("summary") summary: String?,
        @Field("tags") tags: String?
    ): Observable<EntityPostResponse>

    @POST(MW_API_PREFIX + "action=reviewimagelabels")
    @FormUrlEncoded
    fun postReviewImageLabels(
        @Field("filename") fileName: String,
        @Field("token") token: String,
        @Field("batch") batchLabels: String
    ): Observable<MwPostResponse>

    @GET(MW_API_PREFIX + "action=visualeditor&paction=metadata")
    fun getVisualEditorMetadata(@Query("page") page: String): Observable<MwVisualEditorResponse>

    // ------- Watchlist -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched")
    fun getWatchedInfo(@Query("titles") titles: String): Observable<MwQueryResponse>

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched")
    suspend fun getWatchedStatus(@Query("titles") titles: String): MwQueryResponse

    @get:GET(MW_API_PREFIX + "action=query&list=watchlist&wllimit=500&wlallrev=1&wlprop=ids|title|flags|comment|parsedcomment|timestamp|sizes|user|loginfo")
    @get:Headers("Cache-Control: no-cache")
    val watchlist: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=timestamp|user|ids|comment|tags")
    fun getLastModified(@Query("titles") titles: String): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=ids|timestamp|flags|comment|user&rvlimit=2&rvdir=newer")
    suspend fun getRevisionDetails(
        @Query("titles") titles: String,
        @Query("rvstartid") revisionStartId: Long
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=ids|timestamp|flags|comment|user&rvlimit=2&rvdir=newer")
    suspend fun getEditDetails(
        @Query("titles") titles: String,
        @Query("rvstartid") revisionStartId: Long
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=ids|timestamp|size|flags|comment|user&rvlimit=500&rvdir=older")
    suspend fun getEditHistoryDetails(
        @Query("titles") titles: String
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=ids|timestamp|flags|comment|user&rvlimit=1&rvdir=newer")
    suspend fun getArticleCreatedDate(
        @Query("titles") titles: String
    ): MwQueryResponse

    @POST(MW_API_PREFIX + "action=thank")
    @FormUrlEncoded
    suspend fun postThanksToRevision(
        @Field("rev") revisionId: Long,
        @Field("token") token: String
    ): EntityPostResponse

    @POST(MW_API_PREFIX + "action=watch&converttitles=&redirects=")
    @FormUrlEncoded
    fun postWatch(
        @Field("unwatch") unwatch: Int?,
        @Field("pageids") pageIds: String?,
        @Field("titles") titles: String?,
        @Field("expiry") expiry: String?,
        @Field("token") token: String
    ): Observable<WatchPostResponse>

    @POST(MW_API_PREFIX + "action=watch&converttitles=&redirects=")
    @FormUrlEncoded
    suspend fun watch(
            @Field("unwatch") unwatch: Int?,
            @Field("pageids") pageIds: String?,
            @Field("titles") titles: String?,
            @Field("expiry") expiry: String?,
            @Field("token") token: String
    ): WatchPostResponse

    @get:GET(MW_API_PREFIX + "action=query&meta=tokens&type=watch")
    @get:Headers("Cache-Control: no-cache")
    val watchToken: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=watch")
    @Headers("Cache-Control: no-cache")
    suspend fun getWatchToken(): MwQueryResponse

    companion object {
        const val WIKIPEDIA_URL = "https://wikipedia.org/"
        const val WIKIDATA_URL = "https://www.wikidata.org/"
        const val COMMONS_URL = "https://commons.wikimedia.org/"
        const val URL_FRAGMENT_FROM_COMMONS = "/wikipedia/commons/"
        const val MW_API_PREFIX = "w/api.php?format=json&formatversion=2&errorformat=html&errorsuselocal=1&"
        const val PREFERRED_THUMB_SIZE = 320

        // Maximum cache time for site-specific data, and other things not likely to change very often.
        const val SITE_INFO_MAXAGE = 86400
    }
}
