#!/usr/bin/env python
# coding=utf-8

import argparse
import json
import re
import sys
from pathlib import Path
from time import sleep
from typing import Iterable
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from xml.sax.saxutils import escape

from constants import HEADERS as headers

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
RES_DIR = REPO_ROOT / "app" / "src" / "main" / "res"
LANGUAGES_API_URL = "https://www.mediawiki.org/w/api.php"
TOPICS_API_URL = "https://en.wikipedia.org/w/api.php"
MSG_KEYS = [
    "wikimedia-articletopics-topic-architecture",
    "wikimedia-articletopics-topic-art",
    "wikimedia-articletopics-topic-comics-and-anime",
    "wikimedia-articletopics-topic-entertainment",
    "wikimedia-articletopics-topic-fashion",
    "wikimedia-articletopics-topic-literature",
    "wikimedia-articletopics-topic-music",
    "wikimedia-articletopics-topic-performing-arts",
    "wikimedia-articletopics-topic-sports",
    "wikimedia-articletopics-topic-tv-and-film",
    "wikimedia-articletopics-topic-video-games",
    "wikimedia-articletopics-topic-biography",
    "wikimedia-articletopics-topic-women",
    "wikimedia-articletopics-topic-business-and-economics",
    "wikimedia-articletopics-topic-education",
    "wikimedia-articletopics-topic-food-and-drink",
    "wikimedia-articletopics-topic-history",
    "wikimedia-articletopics-topic-military-and-warfare",
    "wikimedia-articletopics-topic-philosophy-and-religion",
    "wikimedia-articletopics-topic-politics-and-government",
    "wikimedia-articletopics-topic-society",
    "wikimedia-articletopics-topic-transportation",
    "wikimedia-articletopics-topic-biology",
    "wikimedia-articletopics-topic-chemistry",
    "wikimedia-articletopics-topic-computers-and-internet",
    "wikimedia-articletopics-topic-earth-and-environment",
    "wikimedia-articletopics-topic-engineering",
    "wikimedia-articletopics-topic-general-science",
    "wikimedia-articletopics-topic-mathematics",
    "wikimedia-articletopics-topic-medicine-and-health",
    "wikimedia-articletopics-topic-physics",
    "wikimedia-articletopics-topic-technology",
    "wikimedia-articletopics-topic-africa",
    "wikimedia-articletopics-topic-asia",
    "wikimedia-articletopics-topic-central-america",
    "wikimedia-articletopics-topic-europe",
    "wikimedia-articletopics-topic-north-america",
    "wikimedia-articletopics-topic-oceania",
    "wikimedia-articletopics-topic-south-america",
]
LANGUAGE_DIR_OVERRIDES = {
    "be-tarask": "values-b+be+x+old",
    "he": "values-iw",
    "hif-latn": "values-b+hif+Latn",
    "id": "values-in",
    "isv-latn": "values-b+isv+Latn",
    "kk-cyrl": "values-b+kk+Cyrl",
    "ko-kp": "values-ko-rKP",
    "ku-latn": "values-ku",
    "ms-arab": "values-b+ms+Arab",
    "nds-nl": "values-b+nds+NL",
    "pt-br": "values-pt-rBR",
    "qqq": "values-qq",
    "roa-tara": "values-b+roa+tara",
    "sh-cyrl": "values-b+sh+Cyrl",
    "sh-latn": "values-sh",
    "skr-arab": "values-skr",
    "sr-ec": "values-sr",
    "sr-el": "values-b+sr+Latn",
    "tg-cyrl": "values-b+tg+Cyrl",
    "tt-cyrl": "values-b+tt+Cyrl",
    "ug-arab": "values-ug",
    "yi": "values-ji",
    "zh-hans": "values-zh",
    "zh-hant": "values-zh-rTW",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate localized onboarding topic string resources from MediaWiki allmessages."
    )
    parser.add_argument(
        "languages",
        nargs="*",
        help="Optional MediaWiki language codes to generate explicitly, for example: en es zh-hant sr-el.",
    )
    parser.add_argument(
        "--languages-api-url",
        default=LANGUAGES_API_URL,
        help=f"MediaWiki siteinfo API endpoint. Default: {LANGUAGES_API_URL}",
    )
    parser.add_argument(
        "--topics-api-url",
        default=TOPICS_API_URL,
        help=f"Wikipedia allmessages API endpoint. Default: {TOPICS_API_URL}",
    )
    parser.add_argument(
        "--res-dir",
        type=Path,
        default=RES_DIR,
        help=f"Android res directory. Default: {RES_DIR}",
    )
    parser.add_argument(
        "--stdout",
        action="store_true",
        help="Print generated XML to stdout instead of writing files. Only valid with one language.",
    )
    return parser.parse_args()


def fetch_json(api_url: str, params: dict[str, str]) -> dict:
    request = Request(f"{api_url}?{urlencode(params)}", headers=headers)
    with urlopen(request) as response:
        return json.load(response)


def fetch_available_language_codes(api_url: str) -> list[str]:
    payload = fetch_json(
        api_url,
        {
            "action": "query",
            "format": "json",
            "meta": "siteinfo",
            "formatversion": "2",
            "siprop": "languages|languagevariants",
            "siinlanguagecode": "",
        },
    )
    languages = payload.get("query", {}).get("languages", [])
    if not languages:
        raise ValueError("No languages returned from MediaWiki siteinfo")
    return [language["code"] for language in languages if language.get("code")]


def existing_values_directories(res_dir: Path) -> set[str]:
    return {path.name for path in res_dir.iterdir() if path.is_dir() and path.name.startswith("values")}


def fetch_messages(api_url: str, language_code: str, msg_keys: Iterable[str]) -> dict[str, str]:
    payload = fetch_json(
        api_url,
        {
            "format": "json",
            "formatversion": "2",
            "errorformat": "html",
            "errorsuselocal": "1",
            "action": "query",
            "meta": "allmessages",
            "amenableparser": "1",
            "ammessages": "|".join(msg_keys),
            "amlang": language_code,
        },
    )

    messages = payload.get("query", {}).get("allmessages", [])
    if not messages:
        raise ValueError(f"No allmessages payload returned for language '{language_code}'")

    result = {}
    missing_keys = []
    for message in messages:
        name = message.get("name")
        value = message.get("content")
        if not name:
            continue
        if "missing" in message:
            missing_keys.append(name)
            continue
        if value is None:
            missing_keys.append(name)
            continue
        result[name] = value

    if missing_keys:
        raise ValueError(
            "Missing localized values for language "
            f"'{language_code}': {', '.join(sorted(missing_keys))}"
        )

    return result


def msg_key_to_resource_name(msg_key: str) -> str:
    resource_name = re.sub(r"[^a-z0-9_]", "_", msg_key.lower().replace("-", "_"))
    if not re.match(r"^[a-z_]", resource_name):
        resource_name = f"topic_{resource_name}"
    return resource_name


def to_android_values_dir(language_code: str) -> str:
    normalized = language_code.replace("_", "-")
    override = LANGUAGE_DIR_OVERRIDES.get(normalized.lower())
    if override:
        return override

    if normalized == "en":
        return "values"

    parts = [part for part in normalized.split("-") if part]
    if not parts:
        raise ValueError(f"Invalid language code: '{language_code}'")

    language = parts[0].lower()
    extras = parts[1:]
    if not extras:
        return f"values-{language}"

    if len(extras) == 1 and re.fullmatch(r"[A-Za-z0-9]{2,3}", extras[0]):
        return f"values-{language}-r{extras[0].upper()}"

    qualifier_parts = [language]
    for part in extras:
        if re.fullmatch(r"[A-Za-z]{4}", part):
            qualifier_parts.append(part.title())
        elif re.fullmatch(r"[A-Za-z0-9]{2,3}", part):
            qualifier_parts.append(part.upper())
        else:
            qualifier_parts.append(part)
    return "values-b+" + "+".join(qualifier_parts)


def build_xml(msg_keys: Iterable[str], localized_messages: dict[str, str], language_code: str) -> str:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="MissingTranslation">',
        f'    <!-- Generated by scripts/generate_topic_strings.py for {language_code}. -->',
    ]

    for msg_key in msg_keys:
        resource_name = msg_key_to_resource_name(msg_key)
        text = escape(localized_messages[msg_key], {'"': '\\"', "'": "\\'"})
        lines.append(f'    <string name="{resource_name}">{text}</string>')

    lines.append("</resources>")
    lines.append("")
    return "\n".join(lines)


def write_output(xml_text: str, res_dir: Path, language_code: str) -> Path:
    values_dir = res_dir / to_android_values_dir(language_code)
    values_dir.mkdir(parents=True, exist_ok=True)
    output_file = values_dir / "strings_topics.xml"
    output_file.write_text(xml_text, encoding="utf-8")
    return output_file


def resolve_target_languages(args: argparse.Namespace) -> list[str]:
    if args.languages:
        return args.languages

    existing_dirs = existing_values_directories(args.res_dir)
    resolved = []
    for language_code in fetch_available_language_codes(args.languages_api_url):
        values_dir = to_android_values_dir(language_code)
        if values_dir in existing_dirs:
            resolved.append(language_code)
    return resolved


def main() -> int:
    args = parse_args()
    target_languages = resolve_target_languages(args)
    if args.stdout and len(target_languages) != 1:
        raise ValueError("--stdout can only be used with a single language code")
    if not target_languages:
        raise ValueError("No matching Android values directories found for the available languages")

    msg_keys = MSG_KEYS
    for language_code in target_languages:
        localized_messages = fetch_messages(args.topics_api_url, language_code, msg_keys)
        xml_text = build_xml(msg_keys, localized_messages, language_code)
        if args.stdout:
            sys.stdout.write(xml_text)
            continue

        output_file = write_output(xml_text, args.res_dir, language_code)
        print(f"Wrote {output_file}")
        sleep(1)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())