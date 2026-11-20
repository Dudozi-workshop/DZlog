#!/usr/bin/env bash
set -eu
app_dir=build/ios-poc/DZlogUiPoc.app
cp source.jpg qa/korean-photo/cells.txt "$app_dir/"
codesign --force --sign - "$app_dir"
device=$(xcrun simctl list devices available -j | python3 -c 'import sys,json; print(next(d["udid"] for devices in json.load(sys.stdin)["devices"].values() for d in devices if d["name"].startswith("iPhone")))')
trap 'xcrun simctl shutdown "$device" || true' EXIT
xcrun simctl boot "$device" || true
xcrun simctl bootstatus "$device" -b
xcrun simctl install "$device" "$app_dir"
pkg=com.dudoziworkshop.dzlog
xcrun simctl launch "$device" "$pkg"
data_dir=$(xcrun simctl get_app_container "$device" "$pkg" data)
for attempt in {1..30}; do
    if test -s "$data_dir/Documents/iphone.jpg"; then
        cp "$data_dir/Documents/iphone.jpg" iphone.jpg
        exit 0
    fi
    sleep 1
done
exit 1
