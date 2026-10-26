#!/usr/bin/env bash
set -u

APK="${1:?apk path required}"
PACKAGE="vn.kozu"
ACTIVITY="$PACKAGE/.MainActivity"
LOG="${2:-logcat.txt}"

adb wait-for-device
adb shell input keyevent 82 || true

echo "::group::Install"
adb install -r -d "$APK" || { echo "::error::The APK could not be installed on a clean emulator"; exit 1; }
echo "::endgroup::"

adb logcat -c
adb shell am start -W -n "$ACTIVITY" || { echo "::error::Activity failed to start"; adb logcat -d > "$LOG"; exit 1; }

sleep 15

adb shell input tap 540 1800 || true
sleep 3
adb shell input swipe 540 1600 540 600 300 || true
sleep 3

adb logcat -d > "$LOG"

if ! adb shell pidof "$PACKAGE" > /dev/null; then
  echo "::error::The app is not running after launch (it crashed or exited)"
  grep -E "FATAL EXCEPTION|AndroidRuntime|Process: $PACKAGE" "$LOG" | head -50
  exit 1
fi

if grep -E "FATAL EXCEPTION" "$LOG" | grep -q .; then
  if grep -A12 "FATAL EXCEPTION" "$LOG" | grep -q "$PACKAGE"; then
    echo "::error::A fatal exception was logged for $PACKAGE"
    grep -A25 "FATAL EXCEPTION" "$LOG" | head -80
    exit 1
  fi
fi

echo "Smoke test passed: the app installed, launched and stayed alive."
