#!/usr/bin/env bash
PREFIX="https://www.mediawiki.org/w"
BASE_PATH="`dirname $0`/.."

wget "$PREFIX/load.php?debug=true&lang=en&modules=skins.minerva.base.reset|skins.minerva.content.styles|ext.cite.style|mediawiki.page.gallery.styles|mobile.app.pagestyles.android&only=styles&skin=vector&version=&*" -O "$BASE_PATH/app/src/main/assets/styles.css"
wget "$PREFIX/load.php?debug=true&lang=en&modules=skins.minerva.base.reset|skins.minerva.content.styles|ext.cite.style|mediawiki.page.gallery.styles|mobile.app.preview.android|mobile.app.preview&only=styles&skin=vector&version=&*" -O "$BASE_PATH/app/src/main/assets/preview.css"
wget "$PREFIX/load.php?debug=true&lang=en&modules=mobile.app.pagestyles.android.night&only=styles&skin=vector&version=&*" -O "$BASE_PATH/app/src/main/assets/dark.css"
