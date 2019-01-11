#!/usr/bin/env bash
INPUT_PATH="https://en.wikipedia.org/api/rest_v1/data/css/mobile/base"
OUTPUT_PATH="`dirname $0`/../app/src/main/assets"

wget $INPUT_PATH -O "$OUTPUT_PATH/styles.css"
wget $INPUT_PATH -O "$OUTPUT_PATH/preview.css"
