#!/usr/bin/env python2
# coding=utf-8

import itertools
import urllib
import urllib2
import lxml
import lxml.builder as lb
import unicodecsv
from HTMLParser import HTMLParser

# Returns CSV of all wikipedias, ordered by number of 'good' articles
QUERY_API_URL = ('https://' 'wikistats.wmflabs.org' '/' 'api.php' '?')
QUERY_PARAMS = [('action', 'dump'), ('table', 'wikipedias'),
                ('format', 'csv'), ('s', 'good')]
RESULT_COLUMN = {'english_name': 1, 'language_code': 2, 'local_name': 10}


lang_keys = []
lang_local_names = []
lang_eng_names = []

parser = HTMLParser()


def add_lang(key, local_name, eng_name):
    lang_keys.append(key)
    lang_local_names.append(parser.unescape(local_name))
    lang_eng_names.append(parser.unescape(eng_name))


def escape(s):
    return s.replace("'", "\\'")


QUERY_URL = QUERY_API_URL + urllib.urlencode(QUERY_PARAMS)
response_file = urllib2.urlopen(QUERY_URL)
csv_data = unicodecsv.reader(response_file)

start_at_row = 1
end_at_row = None
for row in itertools.islice(csv_data, start_at_row, end_at_row):
    language_code = row[RESULT_COLUMN['language_code']]
    if language_code == 'got':
        # 'got' is Gothic Runes, which lie outside the Basic Multilingual Plane
        # Android segfaults on these. So let's ignore those.
        # What's good for Android is also good for iOS :P
        continue
    if language_code == 'zh':
        add_lang(key='zh-hans', local_name=u'简体',
                 eng_name='Simplified Chinese')
        add_lang(key='zh-hant', local_name=u'繁體',
                 eng_name='Traditional Chinese')
        continue
    if language_code == 'no':  # T114042
        language_code = 'nb'

    local_name = row[RESULT_COLUMN['local_name']]
    english_name = row[RESULT_COLUMN['english_name']]
    add_lang(key=escape(language_code), local_name=escape(local_name),
             eng_name=escape(english_name))

add_lang(key='test', local_name='Test', eng_name='Test')
add_lang(key='en-x-piglatin', local_name='Igpay Atinlay', eng_name='Pig Latin')
add_lang(key='', local_name='None', eng_name='None (development)')

# Generate the XML, for Android
NAMESPACE = 'http://schemas.android.com/tools'
TOOLS = '{%s}' % NAMESPACE
x = lb.ElementMaker(nsmap={'tools': NAMESPACE})

keys = [x.item(k) for k in lang_keys]
local_names = [x.item(k) for k in lang_local_names]
eng_names = [x.item(k) for k in lang_eng_names]

resources = x.resources(
    getattr(x, 'string-array')(*keys, name='preference_language_keys'),
    getattr(x, 'string-array')(*local_names,
                               name='preference_language_local_names'),
    getattr(x, 'string-array')(*eng_names,
                               name='preference_language_canonical_names'))
resources.set(TOOLS + 'ignore', 'MissingTranslation')

with open('languages_list.xml', 'w') as f:
    f.write(lxml.etree.tostring(resources, pretty_print=True,
                                xml_declaration=True, encoding='utf-8'))
