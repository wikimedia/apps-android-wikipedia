#!/usr/bin/env python2
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
# - "got" -> Gothic runes wiki. The name of got in got
#   contains characters outside the Unicode BMP. Android
#   hard crashes on these. Let's ignore these fellas
#   for now.
# - "mo" -> Moldovan, which automatically redirects to Romanian (ro),
#   which already exists in our list.
OSTRICH_WIKIS = [u"got", "mo"]


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
        out = codecs.open(class_name + u".java", u"w", u"utf-8")
        out.write(rendered)
        out.close()


def build_wiki(lang, english_name, local_name):
    wiki = Wiki(lang)
    wiki.props["english_name"] = english_name
    wiki.props["local_name"] = local_name
    return wiki


def list_from_sitematrix():
    QUERY_API_URL = 'https://www.mediawiki.org/w/api.php?action=sitematrix' \
        '&format=json&smtype=language&smlangprop=code%7Cname%7Clocalname'

    print(u"Fetching languages")
    data = json.loads(requests.get(QUERY_API_URL).text)
    wikis = []

    for key, value in data[u"sitematrix"].items():
        if type(value) is dict:
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
    simplifiedWiki.props["local_name"] = "简体"
    wiki_list.wikis.insert(chineseWikiIndex + 1, simplifiedWiki)

    traditionalWiki = copy.deepcopy(chineseWiki)
    traditionalWiki.lang = TRADITIONAL_CHINESE_LANG
    traditionalWiki.props["english_name"] = "Traditional Chinese"
    traditionalWiki.props["local_name"] = "繁體"
    wiki_list.wikis.insert(chineseWikiIndex + 2, traditionalWiki)

    bokmalWiki = next((wiki for wiki in wiki_list.wikis if wiki.lang == NORWEGIAN_BOKMAL_WIKI_LANG), None)
    bokmalWiki.lang = NORWEGIAN_BOKMAL_LANG

    return wiki_list


# Populate the aliases for "Special:" and "File:" in all wikis
def populate_aliases(wikis):
    for wiki in wikis.wikis:
        print(u"Fetching Special Page and File alias for %s" % wiki.lang)
        url = u"https://%s.wikipedia.org/w/api.php" % wiki.lang + \
              u"?action=query&meta=siteinfo&format=json&siprop=namespaces"
        data = json.loads(requests.get(url).text)
        # according to https://www.mediawiki.org/wiki/Manual:Namespace
        # -1 seems to be the ID for Special Pages
        wiki.props[u"special_alias"] = data[u"query"][u"namespaces"][u"-1"][u"*"]
        # 6 is the ID for File pages
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
    render_template(u"basichash.java.jinja", u"SpecialAliasData", key=u"special_alias"),
    render_template(u"basichash.java.jinja", u"FileAliasData", key=u"file_alias"),
    render_template(u"basichash.java.jinja", u"MainPageNameData", key=u"main_page_name"),
)
