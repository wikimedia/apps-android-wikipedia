#!/usr/bin/env python
# coding=utf-8

import copy
import os
import json
import codecs
import requests
from jinja2 import Environment, FileSystemLoader


CHINESE_WIKI_LANG = "zh"
SIMPLIFIED_CHINESE_LANG = "zh-hans"
TRADITIONAL_CHINESE_LANG = "zh-hant"

# T114042
NORWEGIAN_BOKMAL_WIKI_LANG = "no"
NORWEGIAN_BOKMAL_LANG = "nb"


# Wikis that cause problems and hence we pretend
# do not exist.
# - "mo" -> Moldovan, which automatically redirects to Romanian (ro),
#   which already exists in our list.
OSTRICH_WIKIS = ["mo"]


# Represents a single wiki, along with arbitrary properties of that wiki
# Simple data container object
class Wiki(object):
    def __init__(self, lang):
        self.lang = lang
        self.props = {}


# Represents a list of wikis plus their properties.
# Encapsulates rendering code as well
class WikiList(object):
    def __init__(self, wikis):
        self.wikis = wikis
        self.template_env = Environment(loader=FileSystemLoader(
            os.path.join(os.path.dirname(os.path.realpath(__file__)), u"templates")
            ))

    def render(self, template, class_name, **kwargs):
        data = {
            u"class_name": class_name,
            u"wikis": self.wikis
        }
        data.update(kwargs)
        rendered = self.template_env.get_template(template).render(**data)
        out = codecs.open(u"../app/src/main/java/org/wikipedia/staticdata/" + class_name + u".kt", u"w", u"utf-8")
        out.write(rendered)
        out.write("\n")
        out.close()


def build_wiki(lang, english_name, local_name):
    wiki = Wiki(lang)
    wiki.props["english_name"] = english_name
    wiki.props["local_name"] = local_name
    return wiki


def list_from_sitematrix():
    QUERY_SITEMATRIX = 'https://www.mediawiki.org/w/api.php?action=sitematrix' \
        '&format=json&formatversion=2&smtype=language&smstate=all'

    print(u"Fetching languages...")
    data = json.loads(requests.get(QUERY_SITEMATRIX).text)
    wikis = []

    for key, value in data[u"sitematrix"].items():
        if type(value) is not dict:
            continue
        site_list = value[u"site"]
        if type(site_list) is not list:
            continue
        wikipedia_url = ""
        for site in site_list:
            if "wikipedia.org" in site[u"url"] and u"closed" not in site:
                wikipedia_url = site[u"url"]
        if len(wikipedia_url) == 0:
            continue
        wikis.append(build_wiki(value[u"code"], value[u"localname"], value[u"name"]))

    return wikis


# Remove unsupported wikis.
def filter_supported_wikis(wikis):
    return [wiki for wiki in wikis if wiki.lang not in OSTRICH_WIKIS]


# Apply manual tweaks to the list of wikis before they're populated.
def preprocess_wikis(wikis):
    # Add TestWiki.
    wikis.append(build_wiki(lang="test", english_name="Test", local_name="Test"))

    return wikis


# Apply manual tweaks to the list of wikis after they're populated.
def postprocess_wikis(wiki_list):
    # Add Simplified and Traditional Chinese dialects.
    chineseWiki = next((wiki for wiki in wiki_list.wikis if wiki.lang == CHINESE_WIKI_LANG), None)
    chineseWikiIndex = wiki_list.wikis.index(chineseWiki)

    simplifiedWiki = copy.deepcopy(chineseWiki)
    simplifiedWiki.lang = SIMPLIFIED_CHINESE_LANG
    simplifiedWiki.props["english_name"] = "Simplified Chinese"
    simplifiedWiki.props["local_name"] = "简体中文"
    wiki_list.wikis.insert(chineseWikiIndex + 1, simplifiedWiki)

    traditionalWiki = copy.deepcopy(chineseWiki)
    traditionalWiki.lang = TRADITIONAL_CHINESE_LANG
    traditionalWiki.props["english_name"] = "Traditional Chinese"
    traditionalWiki.props["local_name"] = "繁體中文"
    wiki_list.wikis.insert(chineseWikiIndex + 2, traditionalWiki)

    bokmalWiki = next((wiki for wiki in wiki_list.wikis if wiki.lang == NORWEGIAN_BOKMAL_WIKI_LANG), None)
    bokmalWiki.lang = NORWEGIAN_BOKMAL_LANG

    return wiki_list


# Populate the aliases for "Special:" and "File:" in all wikis
def populate_aliases(wikis):
    for wiki in wikis.wikis:
        print(u"Fetching namespace strings for %s" % wiki.lang)
        url = u"https://%s.wikipedia.org/w/api.php" % wiki.lang + \
              u"?action=query&meta=siteinfo&format=json&siprop=namespaces"
        data = json.loads(requests.get(url).text)
        # according to https://www.mediawiki.org/wiki/Manual:Namespace
        # -1 seems to be the ID for Special Pages
        wiki.props[u"special_alias"] = data[u"query"][u"namespaces"][u"-1"][u"*"]
        # Namespace 1: Talk
        wiki.props[u"talk_alias"] = data[u"query"][u"namespaces"][u"1"][u"*"]
        # Namespace 2: User
        wiki.props[u"user_alias"] = data[u"query"][u"namespaces"][u"2"][u"*"]
        # Namespace 3: User talk
        wiki.props[u"user_talk_alias"] = data[u"query"][u"namespaces"][u"3"][u"*"]
        # Namespace 6: File
        wiki.props[u"file_alias"] = data[u"query"][u"namespaces"][u"6"][u"*"]
    return wikis


# Populates data on names of main page in each wiki
def populate_main_pages(wikis):
    for wiki in wikis.wikis:
        print(u"Fetching Main Page for %s" % wiki.lang)
        url = u"https://%s.wikipedia.org/w/api.php" % wiki.lang + \
              u"?action=query&meta=siteinfo&format=json&siprop=general"
        data = json.loads(requests.get(url).text)
        wiki.props[u"main_page_name"] = data[u"query"][u"general"][u"mainpage"]
    return wikis


# Returns a function that renders a particular template when passed
# a WikiList object
def render_template(template, filename, **kwargs):
    def _actual_render(wikis):
        wikis.render(template, filename, **kwargs)
        return wikis
    return _actual_render


# Kinda like reduce(), but special cases first function
def chain(*funcs):
    res = funcs[0]()
    for func in funcs[1:]:
        res = func(res)


chain(
    list_from_sitematrix,
    filter_supported_wikis,
    preprocess_wikis,
    WikiList,
    populate_aliases,
    populate_main_pages,
    postprocess_wikis,
    render_template(u"basichash.kt.jinja", u"SpecialAliasData", key=u"special_alias"),
    render_template(u"basichash.kt.jinja", u"FileAliasData", key=u"file_alias"),
    render_template(u"basichash.kt.jinja", u"TalkAliasData", key=u"talk_alias"),
    render_template(u"basichash.kt.jinja", u"UserAliasData", key=u"user_alias"),
    render_template(u"basichash.kt.jinja", u"UserTalkAliasData", key=u"user_talk_alias"),
    render_template(u"basichash.kt.jinja", u"MainPageNameData", key=u"main_page_name"),
)
