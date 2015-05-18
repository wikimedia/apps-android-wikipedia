#!/usr/bin/env python
# coding=utf-8

from urllib2 import urlopen
import unicodecsv as csv
from itertools import islice
import json
import lxml.builder as lb
from lxml import etree

# Returns CSV of all wikipedias, ordered by number of 'good' articles
URL = "https://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv&s=good"

data = csv.reader(urlopen(URL))

lang_keys = []
lang_local_names = []
lang_eng_names = []


def add_lang(key, local_name, eng_name):
    lang_keys.append(key)
    lang_local_names.append(local_name)
    lang_eng_names.append(eng_name)

for row in islice(data, 1, None):
    if row[2] == 'got':
        # 'got' is Gothic Runes, which lie outside the Basic Multilingual Plane
        # < https://en.wikipedia.org/wiki/Plane_(Unicode)#Basic_Multilingual_Plane >
        # Android segfaults on these. So let's ignore those.
        # What's good for Android is also good for iOS :P
        pass
    elif row[2] == 'zh':
        add_lang(key='zh-hans', local_name=u'简体', eng_name='Simplified Chinese')
        add_lang(key='zh-hant', local_name=u'繁體', eng_name='Traditional Chinese')
    else:
        add_lang(key=row[2].replace("'", "\\'"),
                 local_name=row[10].replace("'", "\\'"),
                 eng_name=row[1].replace("'", "\\'"))

add_lang(key='test', local_name='Test', eng_name='Test')

# Generate the XML, for Android
NAMESPACE = 'http://schemas.android.com/tools'
TOOLS = '{%s}' % NAMESPACE
x = lb.ElementMaker(nsmap={'tools': NAMESPACE})

keys = [x.item(k) for k in lang_keys]
local_names = [x.item(k) for k in lang_local_names]
eng_names = [x.item(k) for k in lang_eng_names]

resources = x.resources(
    getattr(x, 'string-array')(*keys, name="preference_language_keys"),
    getattr(x, 'string-array')(*local_names, name="preference_language_local_names"),
    getattr(x, 'string-array')(*eng_names, name="preference_language_canonical_names")
)
resources.set(TOOLS + 'ignore', 'MissingTranslation')

open("languages_list.xml", "w").write(
    etree.tostring(resources, pretty_print=True, xml_declaration=True, encoding='utf-8')
)

# Generate the JSON, for iOS
langs_json = []

# Start from 1, to skip the headers
for i in xrange(1, len(lang_keys)):
    langs_json.append({
        "code": lang_keys[i],
        "name": lang_local_names[i],
        "canonical_name": lang_eng_names[i]
    })

open("languages_list.json", "w").write(json.dumps(langs_json, indent=4))
