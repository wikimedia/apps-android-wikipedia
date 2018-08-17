#!/usr/bin/env python
# coding=utf-8

from datetime import datetime, timedelta
import lxml
import lxml.builder as lb
import json
import requests


QUERY_SITEMATRIX = 'https://www.mediawiki.org/w/api.php?action=sitematrix' \
    '&format=json&formatversion=2&smtype=language&smstate=all'

QUERY_ALLUSERS = '/w/api.php?action=query&format=json&formatversion=2&list=allusers' \
    '&aulimit=50&auactiveusers=1&auwitheditsonly=1'

lang_keys = []
lang_local_names = []
lang_eng_names = []
lang_rank = []


def add_lang(key, local_name, eng_name, rank):
    rank_pos = 0
    # Automatically keep the arrays sorted by rank
    for index, item in enumerate(lang_rank):
        rank_pos = index
        if (rank > item):
            break
    lang_keys.insert(rank_pos, key)
    lang_local_names.insert(rank_pos, local_name)
    lang_eng_names.insert(rank_pos, eng_name)
    lang_rank.insert(rank_pos, rank)


data = json.loads(requests.get(QUERY_SITEMATRIX).text)

for key, value in data[u"sitematrix"].items():
    if type(value) is not dict:
        continue
    language_code = value[u"code"]
    if language_code == 'got':
        # 'got' is Gothic Runes, which lie outside the Basic Multilingual Plane
        # Android segfaults on these. So let's ignore those.
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
    # TODO: If we want to remove languages with too few active users:
    # allusers = json.loads(requests.get(wikipedia_url + QUERY_ALLUSERS).text)
    # if len(allusers[u"query"][u"allusers"]) < 10:
    #    print ("Excluding " + language_code + " (too few active users).")
    #    continue
    # Use the AQS API to get total pageviews for this language wiki in the last month:
    date = datetime.today() - timedelta(days=31)
    unique_device_response = json.loads(requests.get('https://wikimedia.org/api/rest_v1/metrics/unique-devices/' +
                                                     wikipedia_url.replace('https://', '') + '/all-sites/monthly/' +
                                                     date.strftime('%Y%m01') + '/' + date.strftime('%Y%m01')).text)
    rank = 0
    if u"items" in unique_device_response:
        if len(unique_device_response[u"items"]) > 0:
            rank = unique_device_response[u"items"][0][u"devices"]
    print ("Rank for " + language_code + ": " + str(rank))
    if language_code == 'zh':
        add_lang(key='zh-hans', local_name=u'简体中文',
                 eng_name='Simplified Chinese', rank=rank)
        add_lang(key='zh-hant', local_name=u'繁體中文',
                 eng_name='Traditional Chinese', rank=rank)
        continue
    if language_code == 'no':  # T114042
        language_code = 'nb'
    add_lang(language_code, value[u"name"].replace("'", "\\'"), value[u"localname"].replace("'", "\\'"), rank)


add_lang(key='test', local_name='Test', eng_name='Test', rank=0)
add_lang(key='en-x-piglatin', local_name='Igpay Atinlay', eng_name='Pig Latin', rank=0)

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

with open('languages_list.xml', 'wb') as f:
    f.write(lxml.etree.tostring(resources, pretty_print=True,
                                xml_declaration=True, encoding='utf-8'))
