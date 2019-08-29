#!/usr/bin/env bash

set -euo pipefail

make-release() { ./scripts/make-release.py "$@"; }

make-release --beta
make-release --prod
make-release --amazon
