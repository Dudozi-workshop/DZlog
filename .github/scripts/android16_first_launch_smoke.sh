#!/usr/bin/env bash
set -euo pipefail

package="com.dudoziworkshop.dzlog"
apk="app/build/outputs/apk/debug/app-debug.apk"

adb install -r "$apk"
adb logcat -c
adb shell am start -W -n "${package}/.MainActivity"
sleep 12

pid="$(adb shell pidof "$package" | tr -d '\r' || true)"
adb logcat -d -t 4000 > api36-launch-log.txt
if [[ -z "$pid" ]]; then
  echo "DZlog process is not running after fresh launch" >&2
  exit 1
fi

if grep -E -A 25 'FATAL EXCEPTION' api36-launch-log.txt |
    grep -Eq 'Process: com\.dudoziworkshop\.dzlog'; then
  echo "DZlog fatal exception detected during fresh launch" >&2
  exit 1
fi

adb exec-out screencap -p > api36-fresh-start.png
adb shell uiautomator dump /sdcard/dzlog-ui.xml >/dev/null
adb pull /sdcard/dzlog-ui.xml api36-ui.xml >/dev/null
if ! grep -Eq 'DZlog|시작하기|찍고|기록' api36-ui.xml; then
  echo "First-launch accessibility text not found; investigate screenshot" >&2
  exit 1
fi

echo "DZlog process $pid is running; Android 16 first launch UI captured"
