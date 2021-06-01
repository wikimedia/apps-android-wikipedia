package org.wikipedia.page

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.model.CodeEnum
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap
import org.wikipedia.staticdata.*
import java.util.*

/** An enumeration describing the different possible namespace codes. Do not attempt to use this
 * class to preserve URL path information such as Talk: or User: or localization.
 * @see [Wikipedia:Namespace](https://en.wikipedia.org/wiki/Wikipedia:Namespace)
 * @see [Extension default namespaces](https://www.mediawiki.org/wiki/Extension_default_namespaces)
 * @see [Manual:Namespace](https://www.mediawiki.org/wiki/Manual:Namespace.Built-in_namespaces)
 * @see [Namespaces reported by API](https://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=namespaces|namespacealiases)
 */
@Suppress("unused")
enum class Namespace(private val code: Int) : EnumCode {
    MEDIA(-2),
    SPECIAL(-1) {
        override fun talk(): Boolean {
            return false
        }
    },
    MAIN(0),
    TALK(1),
    USER(2),
    USER_TALK(3),
    PROJECT(4),
    PROJECT_TALK(5),
    FILE(6),
    FILE_TALK(7),
    MEDIAWIKI(8),
    MEDIAWIKI_TALK(9),
    TEMPLATE(10),
    TEMPLATE_TALK(11),
    HELP(12),
    HELP_TALK(13),
    CATEGORY(14),
    CATEGORY_TALK(15),
    THREAD(90),
    THREAD_TALK(91),
    SUMMARY(92),
    SUMMARY_TALK(93),
    PORTAL(100),
    PORTAL_TALK(101),
    PROPERTY(102),
    PROPERTY_TALK(103),
    TYPE(104),
    TYPE_TALK(105),
    FORM(106),
    FORM_TALK(107),
    BOOK(108),
    BOOK_TALK(109),
    FORUM(110),
    FORUM_TALK(111),
    DRAFT(118),
    DRAFT_TALK(119),
    USER_GROUP(160),
    ACL(162),
    FILTER(170),
    FILTER_TALK(171),
    USER_WIKI(200),
    USER_WIKI_TALK(201),
    USER_PROFILE(202),
    USER_PROFILE_TALK(203),
    ANNOTATION(248),
    ANNOTATION_TALK(249),
    PAGE(250),
    PAGE_TALK(251),
    INDEX(252),
    INDEX_TALK(253),
    MATH(262),
    MATH_TALK(263),
    WIDGET(274),
    WIDGET_TALK(275),
    JS_APPLET(280),
    JS_APPLET_TALK(281),
    POLL(300),
    POLL_TALK(301),
    COURSE(350),
    COURSE_TALK(351),
    MAPS_LAYER(420),
    MAPS_LAYER_TALK(421),
    QUIZ(430),
    QUIZ_TALK(431),
    EDUCATION_PROGRAM(446),
    EDUCATION_PROGRAM_TALK(447),
    BOILERPLATE(450),
    BOILERPLATE_TALK(451),
    CAMPAIGN(460),
    CAMPAIGN_TALK(461),
    SCHEMA(470),
    SCHEMA_TALK(471),
    JSON_CONFIG(482),
    JSON_CONFIG_TALK(483),
    GRAPH(484),
    GRAPH_TALK(485),
    JSON_DATA(486),
    JSON_DATA_TALK(487),
    NOVA_RESOURCE(488),
    NOVA_RESOURCE_TALK(489),
    GW_TOOLSET(490),
    GW_TOOLSET_TALK(491),
    BLOG(500),
    BLOG_TALK(501),
    USER_BOX(600),
    USER_BOX_TALK(601),
    LINK(700),
    LINK_TALK(701),
    TIMED_TEXT(710),
    TIMED_TEXT_TALK(711),
    GIT_ACCESS_ROOT(730),
    GIT_ACCESS_ROOT_TALK(731),
    INTERPRETATION(800),
    INTERPRETATION_TALK(801),
    MUSTACHE(806),
    MUSTACHE_TALK(807),
    JADE(810),
    JADE_TALK(811),
    R(814),
    R_TALK(815),
    MODULE(828),
    MODULE_TALK(829),
    SECURE_POLL(830),
    SECURE_POLL_TALK(831),
    COMMENT_STREAM(844),
    COMMENT_STREAM_TALK(845),
    CN_BANNER(866),
    CN_BANNER_TALK(867),
    GRAM(1024),
    GRAM_TALK(1025),
    TRANSLATIONS(1198),
    TRANSLATIONS_TALK(1199),
    GADGET(2300),
    GADGET_TALK(2301),
    GADGET_DEFINITION(2302),
    GADGET_DEFINITION_TALK(2303),
    TOPIC(2600);

    /** Warning: this method returns an English translation for the current namespace.  */
    @Deprecated("")
    fun toLegacyString(): String? {
        var string = if (this === MAIN) null else name
        if (string != null) {
            string = string.toLowerCase(Locale.ENGLISH).capitalize(Locale.getDefault())
        }
        return string
    }

    override fun code(): Int {
        return code
    }

    fun special(): Boolean {
        return this === SPECIAL
    }

    fun user(): Boolean {
        return this === USER
    }

    fun userTalk(): Boolean {
        return this === USER_TALK
    }

    fun main(): Boolean {
        return this === MAIN
    }

    fun file(): Boolean {
        return this === FILE
    }

    open fun talk(): Boolean {
        return code and TALK_MASK == TALK_MASK
    }

    companion object {
        @JvmStatic
        val CODE_ENUM: CodeEnum<Namespace> = CodeEnum { code -> of(code) }
        private const val TALK_MASK = 0x1
        private val MAP = EnumCodeMap(Namespace::class.java)

        @JvmStatic
        fun fromLegacyString(wiki: WikiSite, name: String?): Namespace {
            if (FileAliasData.valueFor(wiki.languageCode()).equals(name, true) ||
                    FileAliasData.valueFor(AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE).equals(name, true)) {
                return FILE
            }
            if (SpecialAliasData.valueFor(wiki.languageCode()).equals(name, true) ||
                    SpecialAliasData.valueFor(AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE).equals(name, true)) {
                return SPECIAL
            }
            if (TalkAliasData.valueFor(wiki.languageCode()).equals(name, true) ||
                    TalkAliasData.valueFor(AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE).equals(name, true)) {
                return TALK
            }
            if (UserAliasData.valueFor(wiki.languageCode()).equals(name, true) ||
                    UserAliasData.valueFor(AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE).equals(name, true)) {
                return USER
            }
            return if (UserTalkAliasData.valueFor(wiki.languageCode()).equals(name, true) ||
                    UserTalkAliasData.valueFor(AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE).equals(name, true)) {
                USER_TALK
            } else MAIN
        }

        @JvmStatic
        fun of(code: Int): Namespace {
            return MAP[code]
        }
    }
}
