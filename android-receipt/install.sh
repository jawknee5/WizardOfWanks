#!/usr/bin/env bash
# Builds the APK (optional) and sideloads it to a connected Android device with adb.
#
#   ./install.sh              build if needed, then install + launch
#   ./install.sh --no-build   install the existing ReceiptDetail.apk
#   ./install.sh --rebuild    always rebuild before installing
#
# The device must be visible to adb on THIS machine:
#   USB     - enable Developer options > USB debugging, connect the cable,
#             accept the "Allow USB debugging" prompt, check with: adb devices
#   Wi-Fi   - Developer options > Wireless debugging > Pair device with pairing code:
#             adb pair <phone-ip>:<pairing-port>   (enter the 6 digit code)
#             adb connect <phone-ip>:<adb-port>
set -euo pipefail
cd "$(dirname "$0")"

APK=ReceiptDetail.apk
PACKAGE=com.wizardofwanks.receipt
ACTIVITY="$PACKAGE/.ReceiptDetailActivity"
MODE="${1:-}"

find_adb() {
  if command -v adb >/dev/null 2>&1; then
    command -v adb
    return
  fi
  local sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/android-sdk}}"
  if [ -x "$sdk/platform-tools/adb" ]; then
    echo "$sdk/platform-tools/adb"
    return
  fi
  echo ""
}

ADB="$(find_adb)"
if [ -z "$ADB" ]; then
  echo "adb not found. Install Android platform-tools, or set ANDROID_SDK_ROOT." >&2
  exit 1
fi

if [ "$MODE" = "--rebuild" ] || { [ "$MODE" != "--no-build" ] && [ ! -f "$APK" ]; }; then
  ./build.sh
fi

if [ ! -f "$APK" ]; then
  echo "$APK is missing; run ./build.sh first." >&2
  exit 1
fi

echo "== devices =="
"$ADB" devices -l

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "No device/emulator is connected to adb. Connect the phone or run 'adb connect <ip>:<port>'." >&2
  exit 2
fi

echo "== install =="
"$ADB" install -r "$APK"

echo "== launch =="
"$ADB" shell am start -n "$ACTIVITY"
