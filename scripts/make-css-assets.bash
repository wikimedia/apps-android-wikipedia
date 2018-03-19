#!/usr/bin/env bash
PREFIX="https://en.wikipedia.org/w"

#For use when importing styles from a local Vagrant instance
#PREFIX="localhost:8080/w"

BASE_PATH="`dirname $0`/.."

wget "$PREFIX/load.php?skin=minerva&target=mobile&modules=skins.minerva.base.reset|skins.minerva.content.styles|mediawiki.page.gallery.styles|ext.cite.style|ext.pygments|ext.math.styles|ext.timeline.styles|mediawiki.skinning.content.parsoid|mobile.app|mobile.app.parsoid&only=styles&version=&*" -O "$BASE_PATH/app/src/main/assets/styles.css"
wget "$PREFIX/load.php?skin=minerva&target=mobile&modules=skins.minerva.base.reset|skins.minerva.content.styles|mediawiki.page.gallery.styles|ext.cite.style|ext.pygments|ext.math.styles|ext.timeline.styles|mediawiki.skinning.content.parsoid|mobile.app|mobile.app.parsoid&only=styles&version=&*" -O "$BASE_PATH/app/src/main/assets/preview.css"
