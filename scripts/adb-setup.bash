#!/usr/bin/env bash
# initializes attached devices for testing and returns when ready
case $- in *i*) :;; *) set -euo pipefail;; esac

# ------------------------------------------------------------------------------
# adb with any script arguments

is-booted() {
  [[ "$(adb "$@" shell getprop sys.boot_completed|tr -d '\r')" == 1 ]]
}

wait-for-boot() {
  echo 'waiting for boot...'
  while ! is-booted "$@" && sleep 1; do :; done
}

wait-for-internet-ping() {
  echo 'waiting for internet (ping)...'
  # nc isn't always available
  adb "$@" shell 'ping -c1 wikipedia.org > /dev/null && echo ok'|
  grep -q ok
}

wait-for-internet-nc() {
  echo 'waiting for internet (nc)...'
  # ping always fails on the API 15 emulator
  adb "$@" shell "echo -e 'GET / HTTP/1.1\n'|nc wikipedia.org 80"|
  grep -q 'HTTP/[0-9].[0-9] 200 OK'
}

wait-for-internet() {
  wait-for-internet-nc "$@" || wait-for-internet-ping "$@"
}

unlock-screen() {
  echo 'unlocking...'
  # http://developer.android.com/reference/android/view/KeyEvent.html#KEYCODE_MENU
  declare -i KEYCODE_MENU=82
  adb "$@" shell input keyevent $KEYCODE_MENU
}

reset-clock() {
  echo 'resetting clock...'
  # The emulator's clock may be out of sync which causes network errors like:
  #   javax.net.ssl.SSLHandshakeException: com.android.org.bouncycastle.jce.exception.ExtCertPathValidatorException: Could not validate certificate: current time: Fri Sep 04 09:18:17 GMT+00:00 2015, validation time: Thu Dec 10 23:22:05 GMT+00:00 2015
  adb "$@" shell date -s $(date +%Y%m%d.%H%M%S)
}

# ------------------------------------------------------------------------------
main() {
  declare devices=( $(adb devices|sed -nr '2,$ s_([^\t ]+).*_\1_p') )
  echo "${#devices[@]} devices detected"

  for serialno in "${devices[@]}"; do
    echo "setting up $serialno..."

    echo 'waiting for adb connection...'
    adb -s "$serialno" wait-for-device

    wait-for-boot -s "$serialno"
    wait-for-internet -s "$serialno"
    unlock-screen -s "$serialno"
    reset-clock -s "$serialno"

    echo "$serialno ready"
  done
}

# ------------------------------------------------------------------------------
case $- in *i*) :;; *) main "$@";; esac
