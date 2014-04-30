#!/usr/bin/env python
import os
import json
import unicodecsv as csv
import codecs
from urllib2 import urlopen
from jinja2 import Environment, FileSystemLoader


# Wikis that cause problems and hence we pretend
# do not exist.
# - 'got' -> Gothic runes wiki. The name of got in got
#   contains characters outside the Unicode BMP. Android
#   hard crashes on these. Let's ignore these fellas
#   for now.
OSTRITCH_WIKIS = [u'got']


# Represents a single wiki, along with arbitrary properites of that wiki
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
            os.path.join(os.path.dirname(os.path.realpath(__file__)), u'templates')
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
        out = codecs.open(class_name + ".java", u'w', u'utf-8')
        out.write(rendered)
        out.close()


def list_from_wikistats():
    URL = u"https://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv&s=good"

    print "Fetching languages"
    data = csv.reader(urlopen(URL))
    wikis = []

    is_first = True
    for row in data:
        if is_first:
            is_first = False
            continue  # skip headers
        wiki = Wiki(row[2])
        wiki.props[u'english_name'] = row[1]
        wiki.props[u'local_name'] = row[10]
        wiki.props[u'total_pages'] = row[3]
        wikis.append(wiki)

    return WikiList(wikis)


# Populate the aliases for 'Special:' in all wikis
def populate_special_alias(wikis):
    for wiki in wikis.wikis:
        print "Fetching Special Page alias for %s" % wiki.lang
        url = u"https://%s.wikipedia.org/w/api.php?action=query&meta=siteinfo&format=json&siprop=namespaces" % wiki.lang
        data = json.load(urlopen(url))
        # -1 seems to be the ID for Special Pages
        wiki.props[u'special_alias'] = data['query']['namespaces']['-1']['*']
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
    list_from_wikistats,
    populate_special_alias,
    render_template(u'basichash.java.jinja', u'SpecialAliasData', key=u'special_alias')
)
