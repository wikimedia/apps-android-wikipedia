#!/usr/bin/env python
import os
import sys
import xml.etree.ElementTree as ET

RES_FOLDER = os.path.abspath(os.path.join(os.path.dirname(__file__), "../app/src/main/res"))
EN_STRINGS = os.path.join(RES_FOLDER, "values/strings.xml")
QQ_STRINGS = os.path.join(RES_FOLDER, "values-qq/strings.xml")

# Get ElementTree containing all message names in English
enroot = ET.parse(EN_STRINGS).getroot()

# Get ElementTree containing all documented messages
qqroot = ET.parse(QQ_STRINGS).getroot()

# Create a set to store all documented messages
qqmsgs = set()

# Add all documented messages to that set
for child in qqroot:
    qqmsgs.add(child.attrib['name'])

# Iterate through all messages and check that they're documented
missing = 0
for child in enroot:
    if child.attrib['name'] not in qqmsgs:
        print(child.attrib['name'] + " is undocumented!")
        missing += 1

sys.exit(1 if missing else 0)
