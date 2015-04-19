#!/usr/bin/env python
import os
import json
import unicodecsv as csv
import codecs
from urllib2 import urlopen
from jinja2 import Environment, FileSystemLoader


# Wikis that cause problems and hence we pretend
# do not exist.
# - "got" -> Gothic runes wiki. The name of got in got
#   contains characters outside the Unicode BMP. Android
#   hard crashes on these. Let's ignore these fellas
#   for now.
OSTRITCH_WIKIS = [u"got"]


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

    def get_filtered_wiki_list(self):
        return [wiki for wiki in self.wikis if wiki.lang not in OSTRITCH_WIKIS]

    def render(self, template, class_name, **kwargs):
        data = {
            u"class_name": class_name,
            u"wikis": self.get_filtered_wiki_list()
        }
        data.update(kwargs)
        rendered = self.template_env.get_template(template).render(**data)
        out = codecs.open(class_name + u".java", u"w", u"utf-8")
        out.write(rendered)
        out.close()


def list_from_wikistats():
    URL = u"https://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv&s=good"

    print(u"Fetching languages")
    data = csv.reader(urlopen(URL))
    wikis = []

    is_first = True
    for row in data:
        if is_first:
            is_first = False
            continue  # skip headers
        wiki = Wiki(row[2])
        wiki.props[u"english_name"] = row[1]
        wiki.props[u"local_name"] = row[10]
        wiki.props[u"total_pages"] = row[3]
        wikis.append(wiki)

    # Manually add TestWiki to this list
    testWiki = Wiki(u"test")
    testWiki.props[u"english_name"] = "Test"
    testWiki.props[u"local_name"] = "Test"
    testWiki.props[u"total_pages"] = 0
    wikis.append(testWiki)

    return WikiList(wikis)


# Populate the aliases for "Special:" and "File:" in all wikis
def populate_aliases(wikis):
    for wiki in wikis.wikis:
        print(u"Fetching Special Page and File alias for %s" % wiki.lang)
        url = u"https://%s.wikipedia.org/w/api.php" % wiki.lang + \
              u"?action=query&meta=siteinfo&format=json&siprop=namespaces"
        data = json.load(urlopen(url))
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
              u"?action=query&meta=allmessages&format=json&ammessages=Mainpage"
        data = json.load(urlopen(url))
        wiki.props[u"main_page_name"] = data[u"query"][u"allmessages"][0][u"*"]
    return wikis


# Returns a function that renders a particular template when passed
# a WikiList object
def render_template(template, filename, **kwargs):
    def _actual_render(wikis):
        wikis.render(template, filename, **kwargs)
        return wikis
    return _actual_render


# Render things into a simple key value JSON dict
# Useful for the iOS side of things
def render_simple_json(key, filename):
    def _actual_render(wikis):
        data = dict([(wiki.lang, wiki.props[key]) for wiki in wikis.wikis])
        out = codecs.open(filename, u"w", u"utf-8")
        out.write(json.dumps(data))
        out.close()
        return wikis
    return _actual_render


# Kinda like reduce(), but special cases first function
def chain(*funcs):
    res = funcs[0]()
    for func in funcs[1:]:
        res = func(res)


chain(
    list_from_wikistats,
    populate_aliases,
    render_template(u"basichash.java.jinja", u"SpecialAliasData", key=u"special_alias"),
    render_template(u"basichash.java.jinja", u"FileAliasData", key=u"file_alias"),
    render_simple_json(u"special_alias", u"specialalias.json"),
    render_simple_json(u"file_alias", u"filealias.json"),
    populate_main_pages,
    render_template(u"basichash.java.jinja", u"MainPageNameData", key=u"main_page_name"),
    render_simple_json(u"main_page_name", u"mainpages.json")
)
