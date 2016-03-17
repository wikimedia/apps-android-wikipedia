package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.model.CodeEnum;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

// https://en.wikipedia.org/wiki/Wikipedia:Namespace
// https://www.mediawiki.org/wiki/Extension_default_namespaces
// https://github.com/wikimedia/wikipedia-ios/blob/master/Wikipedia/Code/NSNumber+MWKTitleNamespace.h
// https://www.mediawiki.org/wiki/Manual:Namespace#Built-in_namespaces
public enum Namespace implements EnumCode {
    MEDIA(-2),
    SPECIAL(-1) {
        @Override
        public boolean talk() {
            return false;
        }
    },
    MAIN(0), // Main or Article
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
    PORTAL(100),
    PORTAL_TALK(101),
    BOOK(108),
    BOOK_TALK(109),
    DRAFT(118),
    DRAFT_TALK(119),
    EDUCATION_PROGRAM(446),
    EDUCATION_PROGRAM_TALK(447),
    TIMED_TEXT(710),
    TIMED_TEXT_TALK(711),
    MODULE(828),
    MODULE_TALK(829),
    GADGET(2300),
    GADGET_TALK(2301),
    GADGET_DEFINITION(2302),
    GADGET_DEFINITION_TALK(2303),
    TOPIC(2600);

    public static final CodeEnum<Namespace> CODE_ENUM = new CodeEnum<Namespace>() {
        @NonNull @Override public Namespace enumeration(int code) {
            return of(code);
        }
    };

    private static final int TALK_MASK = 0x1;
    private static final EnumCodeMap<Namespace> MAP = new EnumCodeMap<>(Namespace.class);

    private final int code;

    @NonNull
    public static Namespace of(int code) {
        return MAP.get(code);
    }

    @Override
    public int code() {
        return code;
    }

    public boolean special() {
        return this == SPECIAL;
    }

    public boolean main() {
        return this == MAIN;
    }

    public boolean file() {
        return this == FILE;
    }

    public boolean talk() {
        return (code & TALK_MASK) == TALK_MASK;
    }

    Namespace(int code) {
        this.code = code;
    }
}