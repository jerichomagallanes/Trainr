#!/usr/bin/env bash
# Installs an R8-minified build, launches it and fails if it crashes.
#
# Lives in a file rather than inline in the workflow because
# android-emulator-runner feeds its script to `sh -c` one line at a time, which
# breaks any multi-line shell construct.
set -euo pipefail

APK=app/build/outputs/apk/dev/minified/app-dev-minified.apk
PKG=com.jericx.trainr.dev
ACTIVITY="$PKG/com.jericx.trainr.presentation.MainActivity"

adb install -r "$APK"
adb logcat -c
adb shell am start -n "$ACTIVITY"
sleep 10

# Move past the splash into the first onboarding step, so this covers more than
# Application.onCreate.
adb shell input tap 540 2008 || true
sleep 5

if ! adb shell pidof "$PKG" > /dev/null; then
    echo "::error::The minified app is not running — it crashed after launch."
    adb logcat -d | tail -n 100
    exit 1
fi

if adb logcat -d | grep -q 'FATAL EXCEPTION'; then
    echo "::error::FATAL EXCEPTION in the minified build — most likely a missing R8 keep rule."
    adb logcat -d | grep -A 30 'FATAL EXCEPTION' | head -60
    exit 1
fi

echo "Minified build launched and survived navigation."
