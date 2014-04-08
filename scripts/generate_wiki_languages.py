from urllib2 import urlopen
import unicodecsv as csv
import json
import lxml.builder as lb
from lxml import etree

# Returns CSV of all wikipedias, ordered by number of 'good' articles
URL = "https://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv&s=good"

data = csv.reader(urlopen(URL))

lang_keys = []
lang_local_names = []
lang_eng_names = []

is_first = True
for row in data:
    if is_first:
        is_first = False
        continue  # skip headers!
    if row[2] == 'got':
        # 'got' is Gothic Runes, which lie ouotside the BMP
        # Android segfaults on these. So let's ignore those.
        # What's good for Android is also good for iOS :P
        continue
    lang_keys.append(row[2].replace("'", "\\'"))
    lang_local_names.append(row[10].replace("'", "\\'"))
    lang_eng_names.append(row[1].replace("'", "\\'"))

lang_keys.append('test')
lang_local_names.append('Test')
lang_eng_names.append('Test')

# Generate the XML, for Android
x = lb.E

keys = [x.item(k) for k in lang_keys]
local_names = [x.item(k) for k in lang_local_names]
eng_names = [x.item(k) for k in lang_eng_names]

resources = x.resources(
    getattr(x, 'string-array')(*keys, name="preference_language_keys"),
    getattr(x, 'string-array')(*local_names, name="preference_language_local_names"),
    getattr(x, 'string-array')(*eng_names, name="preference_language_canonical_names")
)


open("languages_list.xml", "w").write(
    etree.tostring(resources, pretty_print=True)
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
