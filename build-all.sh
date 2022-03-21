#!/usr/bin/env bash

set -euo pipefail

make-release() { ./scripts/make-release.py "$@"; }

make-release --beta --bundle
make-release --prod --bundle
make-release --channel amazon
make-release --channel samsung
make-release --channel huawei
