#!/usr/bin/env bash
BASE_PATH="`dirname $0`/../app/src/main/assets"

wget "https://en.wikipedia.org/api/rest_v1/data/css/mobile/base" -O "$BASE_PATH/styles.css"
wget "https://en.wikipedia.org/api/rest_v1/data/css/mobile/base" -O "$BASE_PATH/preview.css"
