#!/usr/bin/env bash
set -eu
pkg=com.dudoziworkshop.dzlog
adb install -r android-source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n "$pkg/.qa.PhotoActivity"
for fixture in qa/korean-photo/cells*.txt; do
    name=$(basename "$fixture" .txt)
    photo="android-$name.jpg"
    for attempt in {1..30}; do
        adb shell run-as "$pkg" test -s "files/$photo" && break
        sleep 1
    done
    if ! adb shell run-as "$pkg" test -s "files/$photo"; then
        adb logcat -d -s AndroidRuntime
        exit 1
    fi
    adb exec-out run-as "$pkg" cat "files/$photo" > "$photo"
done
for attempt in {1..30}; do
    adb shell run-as "$pkg" test -s files/measured-faces.json && break
    sleep 1
done
adb exec-out run-as "$pkg" cat files/measured-faces.json > measured-faces.json
adb exec-out run-as "$pkg" cat files/android-metrics.json > android-metrics.json
python3 qa/korean-photo/measure-faces.py
