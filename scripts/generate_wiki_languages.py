#!/usr/bin/env python2
# coding=utf-8

import lxml
import lxml.builder as lb
import json
import requests


QUERY_API_URL = 'https://www.mediawiki.org/w/api.php?action=sitematrix' \
    '&format=json&smtype=language&smlangprop=code%7Cname%7Clocalname'

lang_keys = []
lang_local_names = []
lang_eng_names = []


def add_lang(key, local_name, eng_name):
    lang_keys.append(key)
    lang_local_names.append(local_name)
    lang_eng_names.append(eng_name)


data = json.loads(requests.get(QUERY_API_URL).text)

for key, value in data[u"sitematrix"].items():
    if type(value) is not dict:
        continue
    language_code = value[u"code"]
    if language_code == 'got':
        # 'got' is Gothic Runes, which lie outside the Basic Multilingual Plane
        # Android segfaults on these. So let's ignore those.
        continue
    if language_code == 'zh':
        add_lang(key='zh-hans', local_name=u'简体',
                 eng_name='Simplified Chinese')
        add_lang(key='zh-hant', local_name=u'繁體',
                 eng_name='Traditional Chinese')
        continue
    if language_code == 'no':  # T114042
        language_code = 'nb'
    add_lang(language_code, value[u"name"], value[u"localname"])


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
    getattr(x, 'string-array')(*local_names, name='preference_language_local_names'),
    getattr(x, 'string-array')(*eng_names, name='preference_language_canonical_names'))
resources.set(TOOLS + 'ignore', 'MissingTranslation')

with open('languages_list.xml', 'w') as f:
    f.write(lxml.etree.tostring(resources, pretty_print=True,
                                xml_declaration=True, encoding='utf-8'))
