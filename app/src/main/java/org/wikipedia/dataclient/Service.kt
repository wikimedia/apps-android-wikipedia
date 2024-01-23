package org.wikipedia.dataclient

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.captcha.Captcha
import org.wikipedia.dataclient.discussiontools.DiscussionToolsEditResponse
import org.wikipedia.dataclient.discussiontools.DiscussionToolsInfoResponse
import org.wikipedia.dataclient.discussiontools.DiscussionToolsSubscribeResponse
import org.wikipedia.dataclient.discussiontools.DiscussionToolsSubscriptionList
import org.wikipedia.dataclient.mwapi.*
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.rollback.RollbackPostResponse
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.dataclient.wikidata.Claims
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.dataclient.wikidata.Search
import org.wikipedia.edit.Edit
import org.wikipedia.login.LoginClient.LoginResponse
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
        MW_API_PREFIX + "action=query&redirects=&converttitles=&prop=description|pageimages|coordinates|info&piprop=thumbnail" +
                "&pilicense=any&generator=prefixsearch&gpsnamespace=0&inprop=varianttitles|displaytitle&pithumbsize=" + PREFERRED_THUMB_SIZE
    )
    suspend fun prefixSearch(@Query("gpssearch") searchTerm: String?,
                             @Query("gpslimit") maxResults: Int,
                             @Query("gpsoffset") gpsOffset: Int?): MwQueryResponse

    @GET(
        MW_API_PREFIX + "action=query&converttitles=" +
                "&prop=description|pageimages|pageprops|coordinates|info&ppprop=mainpage|disambiguation" +
                "&generator=search&gsrnamespace=0&gsrwhat=text" +
                "&inprop=varianttitles|displaytitle" +
                "&gsrinfo=&gsrprop=redirecttitle&piprop=thumbnail&pilicense=any&pithumbsize=" +
                PREFERRED_THUMB_SIZE
    )
    suspend fun fullTextSearch(
        @Query("gsrsearch") searchTerm: String?,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("gsroffset") gsrOffset: Int?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=allusers&auwitheditsonly=1")
    fun prefixSearchUsers(
            @Query("auprefix") prefix: String,
            @Query("aulimit") maxResults: Int
    ): Observable<MwQueryResponse>

    @GET(
        MW_API_PREFIX + "action=query&generator=search&prop=imageinfo&iiprop=extmetadata|url" +
                "&gsrnamespace=6&iiurlwidth=" + PREFERRED_THUMB_SIZE
    )
    suspend fun fullTextSearchCommons(
        @Query("gsrsearch") searchTerm: String,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("gsroffset") gsrOffset: Int?,
    ): MwQueryResponse

    @GET(
        MW_API_PREFIX + "action=query&generator=search&gsrnamespace=0&gsrqiprofile=classic_noboostlinks" +
                "&origin=*&piprop=thumbnail&prop=pageimages|description|info|pageprops" +
                "&inprop=varianttitles&smaxage=86400&maxage=86400&pithumbsize=" + PREFERRED_THUMB_SIZE
    )
    fun searchMoreLike(
        @Query("gsrsearch") searchTerm: String?,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("pilimit") piLimit: Int,
    ): Observable<MwQueryResponse>

    // ------- Miscellaneous -------

    @get:GET(MW_API_PREFIX + "action=fancycaptchareload")
    val newCaptcha: Observable<Captcha>

    @GET(MW_API_PREFIX + "action=query&prop=langlinks&lllimit=500&redirects=&converttitles=")
    suspend fun getLangLinks(@Query("titles") title: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=description&redirects=1")
    fun getDescription(@Query("titles") titles: String): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=info|description|pageimages&inprop=varianttitles|displaytitle&redirects=1&pithumbsize=" + PREFERRED_THUMB_SIZE)
    suspend fun getInfoByPageIdsOrTitles(@Query("pageids") pageIds: String? = null, @Query("titles") titles: String? = null): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query")
    suspend fun getPageIds(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    fun getImageInfo(
        @Query("titles") titles: String,
        @Query("iiextmetadatalanguage") lang: String
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    suspend fun getImageInfoSuspend(
        @Query("titles") titles: String,
        @Query("iiextmetadatalanguage") lang: String
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=videoinfo&viprop=timestamp|user|url|mime|extmetadata|derivatives&viurlwidth=" + PREFERRED_THUMB_SIZE)
    fun getVideoInfo(
        @Query("titles") titles: String,
        @Query("viextmetadatalanguage") lang: String
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo|entityterms&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    fun getImageInfoWithEntityTerms(
            @Query("titles") titles: String,
            @Query("iiextmetadatalanguage") metadataLang: String,
            @Query("wbetlanguage") entityLang: String
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&prop=info&inprop=protection&uiprop=groups")
    fun getProtectionInfo(@Query("titles") titles: String): Observable<MwQueryResponse>

    @get:GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    val siteMatrix: Observable<SiteMatrix>

    @GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    suspend fun getSiteMatrix(): SiteMatrix

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&siprop=namespaces")
    suspend fun getPageNamespaceWithSiteInfo(
        @Query("titles") title: String?,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String? = null,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String? = null,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String? = null
    ): MwQueryResponse

    @get:GET(MW_API_PREFIX + "action=query&meta=siteinfo&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    val siteInfo: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&siprop=general|magicwords")
    suspend fun getSiteInfoWithMagicWords(): MwQueryResponse

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1")
    fun parsePage(@Query("page") pageTitle: String): Observable<MwParseResponse>

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1")
    fun parseText(@Query("text") text: String): Observable<MwParseResponse>

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1&mainpage=1")
    fun parseTextForMainPage(@Query("page") mainPageTitle: String): Observable<MwParseResponse>

    @GET(MW_API_PREFIX + "action=query&prop=info&generator=categories&inprop=varianttitles|displaytitle&gclshow=!hidden&gcllimit=500")
    suspend fun getCategories(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=description|pageimages|info&generator=categorymembers&inprop=varianttitles|displaytitle&gcmprop=ids|title")
    suspend fun getCategoryMembers(
        @Query("gcmtitle") title: String,
        @Query("gcmtype") type: String,
        @Query("gcmlimit") count: Int,
        @Query("gcmcontinue") continueStr: String?
    ): MwQueryResponse

    @get:GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&grnlimit=10&prop=description|imageinfo|revisions&rvprop=ids|timestamp|flags|comment|user|content&rvslots=mediainfo&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    @get:Headers("Cache-Control: no-cache")
    val randomWithImageInfo: Observable<MwQueryResponse>

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&list=recentchanges&rcprop=title|timestamp|ids|oresscores|sizes|tags|user|parsedcomment|comment|flags&rcnamespace=0&rctype=edit|new")
    suspend fun getRecentEdits(
        @Query("rclimit") count: Int,
        @Query("rcstart") startTimeStamp: String,
        @Query("rcdir") direction: String?,
        @Query("rctoponly") latestRevisions: String?,
        @Query("rcshow") filters: String?,
        @Query("rccontinue") continueStr: String?
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=options")
    fun postSetOptions(
        @Field("change") change: String,
        @Field("token") token: String
    ): Observable<MwPostResponse>

    @get:GET(MW_API_PREFIX + "action=streamconfigs&format=json&constraints=destination_event_service=eventgate-analytics-external")
    val streamConfigs: Observable<MwStreamConfigsResponse>

    @GET(MW_API_PREFIX + "action=query&meta=allmessages&amenableparser=1")
    suspend fun getMessages(
            @Query("ammessages") messages: String,
            @Query("amargs") args: String?
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=shortenurl")
    suspend fun shortenUrl(
            @Field("url") url: String,
    ): ShortenUrlResponse

    @GET(MW_API_PREFIX + "action=query&generator=geosearch&prop=coordinates|description|pageimages|info|extracts&inprop=varianttitles|displaytitle&exintro=true&exsentences=1")
    suspend fun getGeoSearch(
        @Query("ggscoord", encoded = true) coordinates: String,
        @Query("ggsradius") radius: Int,
        @Query("ggslimit") ggsLimit: Int,
        @Query("colimit") coLimit: Int,
    ): MwQueryResponse

    // ------- CSRF, Login, and Create Account -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens")
    fun getTokenObservable(@Query("type") type: String = "csrf"): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&meta=tokens")
    @Headers("Cache-Control: no-cache")
    suspend fun getToken(@Query("type") type: String = "csrf"): MwQueryResponse

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

    @GET(MW_API_PREFIX + "action=query&list=users&usprop=editcount|groups|registration|rights")
    suspend fun userInfo(@Query("ususers") userName: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=users&usprop=editcount|groups|registration|rights&meta=allmessages")
    suspend fun userInfoWithMessages(@Query("ususers") userName: String, @Query("ammessages") messages: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=globaluserinfo&guiprop=editcount|groups|rights")
    suspend fun globalUserInfo(@Query("guiuser") userName: String): MwQueryResponse

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

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echomarkread")
    suspend fun markRead(
        @Field("token") token: String,
        @Field("list") readList: String?,
        @Field("unreadlist") unreadList: String?
    ): MwQueryResponse

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

    @GET(MW_API_PREFIX + "action=query&prop=revisions|info&rvslots=main&rvprop=content|timestamp|ids&rvlimit=1&converttitles=&intestactions=edit&intestactionsdetail=full&inprop=editintro")
    fun getWikiTextForSectionWithInfo(
        @Query("titles") title: String,
        @Query("rvsection") section: Int?
    ): Observable<MwQueryResponse>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    suspend fun postUndoEdit(
            @Field("title") title: String,
            @Field("summary") summary: String? = null,
            @Field("assert") user: String? = null,
            @Field("token") token: String,
            @Field("undo") undoRevId: Long,
            @Field("undoafter") undoRevAfter: Long? = null,
    ): Edit

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    fun postEditSubmit(
        @Field("title") title: String,
        @Field("section") section: String?,
        @Field("sectiontitle") newSectionTitle: String?,
        @Field("summary") summary: String,
        @Field("assert") user: String?,
        @Field("text") text: String?,
        @Field("appendtext") appendText: String?,
        @Field("baserevid") baseRevId: Long,
        @Field("token") token: String,
        @Field("captchaid") captchaId: String?,
        @Field("captchaword") captchaWord: String?,
        @Field("minor") minor: Boolean? = null,
        @Field("watchlist") watchlist: String? = null,
    ): Observable<Edit>

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=visualeditoredit")
    suspend fun postVisualEditorEdit(
        @Field("paction") action: String,
        @Field("page") title: String,
        @Field("token") token: String,
        @Field("section") section: Int,
        @Field("sectiontitle") newSectionTitle: String?,
        @Field("summary") summary: String,
        @Field("assert") user: String?,
        @Field("captchaid") captchaId: String?,
        @Field("captchaword") captchaWord: String?,
        @Field("minor") minor: Boolean? = null,
        @Field("watchlist") watchlist: String? = null,
        @Field("plugins") plugins: String? = null,
        @Field("data-ge-task-image-recommendation") imageRecommendationJson: String? = null,
    ): Edit

    @GET(MW_API_PREFIX + "action=query&list=usercontribs&ucprop=ids|title|timestamp|comment|size|flags|sizediff|tags&meta=userinfo&uiprop=groups|blockinfo|editcount|latestcontrib|rights")
    suspend fun getUserContributions(
        @Query("ucuser") username: String,
        @Query("uclimit") maxCount: Int,
        @Query("uccontinue") uccontinue: String?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=usercontribs&ucprop=ids|title|timestamp|comment|size|flags|sizediff|tags")
    suspend fun getUserContrib(
            @Query("ucuser") username: String,
            @Query("uclimit") maxCount: Int,
            @Query("ucnamespace") ns: String?,
            @Query("ucshow") filter: String?,
            @Query("uccontinue") uccontinue: String?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=pageviews")
    suspend fun getPageViewsForTitles(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=wikimediaeditortaskscounts|userinfo&uiprop=groups|blockinfo|editcount|latestcontrib")
    suspend fun getEditorTaskCounts(): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=rollback")
    suspend fun postRollback(
        @Field("title") title: String,
        @Field("summary") summary: String?,
        @Field("user") user: String,
        @Field("token") token: String
    ): RollbackPostResponse

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

    @GET(MW_API_PREFIX + "action=query&prop=entityterms")
    fun getWikidataEntityTerms(
            @Query("titles") titles: String,
            @Query("wbetlanguage") lang: String
    ): Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=wbgetclaims")
    fun getClaims(
        @Query("entity") entity: String,
        @Query("property") property: String?
    ): Observable<Claims>

    @GET(MW_API_PREFIX + "action=wbgetentities&props=descriptions|labels|sitelinks")
    suspend fun getWikidataLabelsAndDescriptions(@Query("ids") idList: String): Entities

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

    // ------- Watchlist -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched")
    fun getWatchedInfo(@Query("titles") titles: String): Observable<MwQueryResponse>

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched")
    suspend fun getWatchedStatus(@Query("titles") titles: String): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched&meta=userinfo&uiprop=options")
    suspend fun getWatchedStatusWithUserOptions(@Query("titles") titles: String): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched&meta=userinfo&uiprop=rights")
    suspend fun getWatchedStatusWithRights(@Query("titles") titles: String): MwQueryResponse

    @get:GET(MW_API_PREFIX + "action=query&list=watchlist&wllimit=500&wlallrev=1&wlprop=ids|title|flags|comment|parsedcomment|timestamp|sizes|user|loginfo")
    @get:Headers("Cache-Control: no-cache")
    val watchlist: Observable<MwQueryResponse>

    @GET(MW_API_PREFIX + "action=query&list=watchlist&wllimit=500&wlprop=ids|title|flags|comment|parsedcomment|timestamp|sizes|user|loginfo")
    @Headers("Cache-Control: no-cache")
    suspend fun getWatchlist(
        @Query("wlallrev") latestRevisions: String?,
        @Query("wlshow") showCriteria: String?,
        @Query("wltype") typeOfChanges: String?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvslots=main&rvprop=timestamp|user|ids|comment|tags")
    suspend fun getLastModified(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=newer")
    suspend fun getRevisionDetailsAscending(
        @Query("titles") titles: String?,
        @Query("pageids") pageIds: String?,
        @Query("rvlimit") count: Int,
        @Query("rvstartid") revisionStartId: Long?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=older")
    suspend fun getRevisionDetailsDescending(
        @Query("titles") titles: String,
        @Query("rvlimit") count: Int,
        @Query("rvstartid") revisionStartId: Long?,
        @Query("rvcontinue") continueStr: String?,
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=older")
    suspend fun getRevisionDetailsWithInfo(
            @Query("pageids") pageIds: String,
            @Query("rvlimit") count: Int,
            @Query("rvstartid") revisionStartId: Long
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=older&inprop=watched&meta=userinfo&uiprop=rights")
    suspend fun getRevisionDetailsWithUserInfo(
        @Query("pageids") pageIds: String,
        @Query("rvlimit") count: Int,
        @Query("rvstartid") revisionStartId: Long
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

    // ------- DiscussionTools -------

    @GET(MW_API_PREFIX + "action=discussiontoolspageinfo&prop=threaditemshtml")
    suspend fun getTalkPageTopics(
            @Query("page") page: String,
            @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String,
            @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String,
            @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String
    ): DiscussionToolsInfoResponse

    @POST(MW_API_PREFIX + "action=discussiontoolssubscribe")
    @FormUrlEncoded
    suspend fun subscribeTalkPageTopic(
            @Field("page") page: String,
            @Field("commentname") topicName: String,
            @Field("token") token: String,
            @Field("subscribe") subscribe: Boolean?,
    ): DiscussionToolsSubscribeResponse

    @GET(MW_API_PREFIX + "action=discussiontoolsgetsubscriptions")
    suspend fun getTalkPageTopicSubscriptions(@Query("commentname") topicNames: String): DiscussionToolsSubscriptionList

    @POST(MW_API_PREFIX + "action=discussiontoolsedit&paction=addtopic")
    @FormUrlEncoded
    suspend fun postTalkPageTopic(
            @Field("page") page: String,
            @Field("sectiontitle") title: String,
            @Field("wikitext") text: String,
            @Field("token") token: String,
            @Field("summary") summary: String? = null,
            @Field("captchaid") captchaId: Long? = null,
            @Field("captchaword") captchaWord: String? = null
    ): DiscussionToolsEditResponse

    @POST(MW_API_PREFIX + "action=discussiontoolsedit&paction=addcomment")
    @FormUrlEncoded
    suspend fun postTalkPageTopicReply(
            @Field("page") page: String,
            @Field("commentid") commentId: String,
            @Field("wikitext") text: String,
            @Field("token") token: String,
            @Field("summary") summary: String? = null,
            @Field("captchaid") captchaId: Long? = null,
            @Field("captchaword") captchaWord: String? = null
    ): DiscussionToolsEditResponse

    @GET(MW_API_PREFIX + "action=query&generator=growthtasks")
    suspend fun getGrowthTasks(
        @Query("ggttasktypes") taskTypes: String?,
        @Query("ggttopics") topics: String?,
        @Query("ggtlimit") count: Int
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&generator=search&gsrsearch=hasrecommendation%3Aimage&gsrnamespace=0&gsrsort=random&prop=growthimagesuggestiondata|revisions&rvprop=ids|timestamp|flags|comment|user|content&rvslots=main&rvsection=0")
    suspend fun getPagesWithImageRecommendations(
        @Query("gsrlimit") count: Int
    ): MwQueryResponse

    @POST(MW_API_PREFIX + "action=growthinvalidateimagerecommendation")
    @FormUrlEncoded
    suspend fun invalidateImageRecommendation(
        @Field("tasktype") taskType: String,
        @Field("title") title: String,
        @Field("filename") fileName: String,
        @Field("token") token: String
    ): MwPostResponse

    @GET(MW_API_PREFIX + "action=paraminfo")
    suspend fun getParamInfo(
        @Query("modules") modules: String
    ): ParamInfoResponse

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
