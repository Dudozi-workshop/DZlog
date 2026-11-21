#!/usr/bin/env bash
set -eu
app_dir=build/ios-poc/DZlogUiPoc.app
cp source.jpg qa/korean-photo/cells*.txt "$app_dir/"
cp -R font-probe "$app_dir/"
codesign --force --sign - "$app_dir"
device=$(xcrun simctl list devices available -j | python3 -c 'import sys,json; print(next(d["udid"] for devices in json.load(sys.stdin)["devices"].values() for d in devices if d["name"].startswith("iPhone")))')
trap 'xcrun simctl shutdown "$device" || true' EXIT
xcrun simctl boot "$device" || true
xcrun simctl bootstatus "$device" -b
xcrun simctl install "$device" "$app_dir"
pkg=com.dudoziworkshop.dzlog
xcrun simctl launch "$device" "$pkg"
data_dir=$(xcrun simctl get_app_container "$device" "$pkg" data)
for fixture in qa/korean-photo/cells*.txt; do
    name=$(basename "$fixture" .txt)
    for platform in iphone probe; do
        photo="$platform-$name.jpg"
        for attempt in {1..30}; do
            test -s "$data_dir/Documents/$photo" && break
            sleep 1
        done
        cp "$data_dir/Documents/$photo" "$photo"
    done
done
cp "$data_dir/Documents/iphone-metrics.json" iphone-metrics.json
