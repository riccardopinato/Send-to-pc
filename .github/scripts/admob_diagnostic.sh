#!/usr/bin/env bash
set -Eeuo pipefail

MODE="${1:?mode required}"
FORCE_EEA="${2:?force_eea required}"
REPORT="$GITHUB_WORKSPACE/admob-report/$MODE"
APK="$GITHUB_WORKSPACE/app/build/outputs/apk/release/app-release.apk"
PACKAGE="com.riccardopinato.inviaalpc"

mkdir -p "$REPORT"

adb install -r -g "$APK"
adb logcat -c
adb shell am force-stop "$PACKAGE" || true
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
sleep 20

adb exec-out screencap -p > "$REPORT/launch-20s.png" || true
adb shell uiautomator dump /sdcard/window.xml || true
adb pull /sdcard/window.xml "$REPORT/window.xml" || true
adb logcat -d -v threadtime > "$REPORT/logcat.txt"
grep -Ei "Monetization|UserMessagingPlatform|MobileAds|Ads|admob|consent|interstitial|banner" "$REPORT/logcat.txt" > "$REPORT/ads-log.txt" || true
adb shell pidof "$PACKAGE" > "$REPORT/pid.txt" || true

{
  echo "mode=$MODE"
  echo "force_eea=$FORCE_EEA"
  echo "production_app_id=true"
  echo "banner_unit=google_test"
  echo "interstitial_unit=google_test"
  echo "--- monetization markers ---"
  grep -E "Consent state|Mobile Ads initialized|Banner loaded|Banner load failed|Interstitial loaded|Interstitial load failed|Consent update|Ads disabled|UMP diagnostic" "$REPORT/ads-log.txt" || true
} > "$REPORT/summary.txt"

cat "$REPORT/summary.txt"
