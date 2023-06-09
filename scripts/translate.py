#!/usr/bin/env python
"""
This python script extracts string resources, calls Google translate

Top 20 languages to translate the release notes into

   'en' English
   'de' German
   'ru' Russian
   'it' Italian
   'fr' French
   'ja' Japanese
   'es' Spanish
   'zh' Chinese
   'nl' Dutch
   'pl' Polish
   'fa' Persian
   'tr' Turkish
   'pt' Portuguese
   'he' Hebrew
   'ar' Arabic
   'sv' Swedish
   'cs' Czech
   'fi' Finnish
   'uk' Ukrainian
   'id' Indonesian
"""
import json
import os
from googletrans import Translator

release_notes = "release notes"
OUTPUTLANGS = ["en","de","ru","it","fr","ja","es","zh","nl","pl","fa","tr","pt","he","ar","sv","cs","fi","uk","id"]

def translateNotes():
    """
    Utility function to translate release notes
    """
    translator = Translator()

    for destination in OUTPUTLANGS.values():
        resultTranslation = translator.translate( release_notes ,destination, src = "en")
print(f"Result:! \n{resultTranslation.text}\nTarget language code is {resultTranslation.dest}")