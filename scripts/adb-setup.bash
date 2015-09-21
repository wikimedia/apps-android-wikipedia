#!/usr/bin/env bash
# initializes a device for testing and returns when ready. all arguments are
# passed to adb
case $- in *i*) :;; *) set -euo pipefail;; esac

# ------------------------------------------------------------------------------
declare ADB_ARGS=()

# ------------------------------------------------------------------------------
# adb with any script arguments
adb() {
  command adb ${ADB_ARGS:+"${ADB_ARGS[@]}"} "$@"
}

is-booted() {
  [[ "$(adb shell getprop sys.boot_completed|tr -d '\r')" == 1 ]]
}

wait-for-boot() {
  while ! is-booted && sleep 1; do :; done
}

wait-for-internet-ping() {
  # nc isn't always available
  adb shell 'ping -c1 wikipedia.org > /dev/null && echo ok'|
  grep -q ok
}

wait-for-internet-nc() {
  # ping always fails on the API 15 emulator
  adb shell "echo -e 'GET / HTTP/1.1\n'|nc wikipedia.org 80"|
  grep -q 'HTTP/[0-9].[0-9] 200 OK'
}

wait-for-internet() {
  wait-for-internet-nc || wait-for-internet-ping
}

unlock-screen() {
  # http://developer.android.com/reference/android/view/KeyEvent.html#KEYCODE_MENU
  declare -i KEYCODE_MENU=82
  adb shell input keyevent $KEYCODE_MENU
}

# ------------------------------------------------------------------------------
main() {
  ADB_ARGS=("$@")
  adb wait-for-device
  wait-for-boot
  wait-for-internet
  unlock-screen
}

# ------------------------------------------------------------------------------
case $- in *i*) :;; *) main "$@";; esac