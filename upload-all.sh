#!/usr/bin/env bash

set -euo pipefail

scp ./releases/wikipedia-*-beta*apk releases1002.eqiad.wmnet:/srv/org/wikimedia/releases/mobile/android/wikipedia/betas/
scp ./releases/wikipedia-*-r*apk releases1002.eqiad.wmnet:/srv/org/wikimedia/releases/mobile/android/wikipedia/stable/
