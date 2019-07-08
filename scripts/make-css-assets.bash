#!/usr/bin/env bash
INPUT_PATH="https://meta.wikimedia.org/api/rest_v1/data/css/mobile/base"
OUTPUT_PATH="`dirname $0`/../app/src/main/assets"

curl $INPUT_PATH > "$OUTPUT_PATH/styles.css"
curl $INPUT_PATH > "$OUTPUT_PATH/preview.css"
