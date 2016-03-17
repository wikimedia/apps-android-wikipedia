#! /usr/bin/env bash

# Pulls all files from the device and extracts them under app/
#
# Allows specifying a device serial number or qualifier

# Python-based decompression, to avoid binary dependencies
read -r -d '' python_commands <<- 'EOF'
	import zlib
	import sys
	sys.stdout.write(zlib.decompress(sys.stdin.read()))
EOF

adb "$@" backup -noapk org.wikipedia \
    && \
dd if=backup.ab bs=1 skip=24 \
\
| python2 -c "$python_commands" \
\
| tar -xvf -
