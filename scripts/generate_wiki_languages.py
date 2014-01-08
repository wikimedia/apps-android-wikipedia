from urllib2 import urlopen
import csv
import json
import lxml.builder as lb
from lxml import etree

# Returns CSV of all wikipedias, ordered by number of 'good' articles
URL = "https://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv&s=good"

data = csv.reader(urlopen(URL))

lang_keys = []
lang_local_names = []
lang_eng_names = []
for row in data:
    lang_keys.append(row[2])
    lang_local_names.append(row[10])
    lang_eng_names.append(row[1])

# Generate the XML, for Android
x = lb.E

keys = [x.item(k) for k in lang_keys]

# Skip the headers!
del keys[0]
resources = x.resources(
    getattr(x, 'string-array')(*keys, name="preference_language_keys"),
)

open("languages_list.xml", "w").write(
    etree.tostring(resources, pretty_print=True, encoding="utf-8", xml_declaration=True)
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
