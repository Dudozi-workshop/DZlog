#!/usr/bin/env bash
set -eu
pkg=com.dudoziworkshop.dzlog
adb install -r android-source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n "$pkg/.qa.PhotoActivity"
for attempt in {1..30}; do
    if adb shell run-as "$pkg" test -s files/android.jpg; then
        adb exec-out run-as "$pkg" cat files/android.jpg > android.jpg
        exit 0
    fi
    sleep 1
done
adb logcat -d -s AndroidRuntime
exit 1
