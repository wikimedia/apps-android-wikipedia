#!/usr/bin/env bash
# Pulls all files from the device and extracts them under app/
adb backup -noapk org.wikipedia && dd if=backup.ab bs=1 skip=24 | python -c "import zlib,sys;sys.stdout.write(zlib.decompress(sys.stdin.read()))" | tar -xvf -
