from urllib2 import urlopen
import csv
import lxml.builder as lb
from lxml import etree

# Returns CSV of all wikipedias, ordered by number of 'good' articles
URL = "https://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv&s=good"

data = csv.reader(urlopen(URL))

# Column 2 is the language code
lang_keys = [row[2] for row in data]

del lang_keys[0] # Get rid of the headers

# Generate the XML
x = lb.E

keys = [x.item(k) for k in lang_keys]

resources = x.resources(
    getattr(x, 'string-array')(*keys, name="preference_language_keys"),
)

open("languages_list.xml", "w").write(
    etree.tostring(resources, pretty_print=True, encoding="utf-8", xml_declaration=True)
)

