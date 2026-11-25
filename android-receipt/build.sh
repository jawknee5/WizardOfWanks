#!/usr/bin/env bash
# Builds a signed, installable APK without Gradle.
#
# Requirements (already installed in this workspace):
#   JDK 17          $HOME/tools/jdk17            (override with JAVA_HOME)
#   Android SDK     $HOME/android-sdk            (override with ANDROID_SDK_ROOT)
#                   platforms/android-34, build-tools/34.0.0
#
# Usage: ./build.sh          -> android-receipt/ReceiptDetail.apk
set -euo pipefail
cd "$(dirname "$0")"

JAVA_HOME="${JAVA_HOME:-$HOME/tools/jdk17}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
BUILD_TOOLS="$ANDROID_SDK_ROOT/build-tools/34.0.0"
ANDROID_JAR="$ANDROID_SDK_ROOT/platforms/android-34/android.jar"
export PATH="$JAVA_HOME/bin:$PATH"

OUT=build
APK=ReceiptDetail.apk

if [ ! -x "$BUILD_TOOLS/aapt2" ] || [ ! -f "$ANDROID_JAR" ]; then
  echo "Android SDK not found. Set ANDROID_SDK_ROOT to an SDK with platforms;android-34 and build-tools;34.0.0." >&2
  exit 1
fi

if [ ! -f src/main/res/mipmap-xxhdpi/ic_launcher.png ]; then
  python3 tools/make_icons.py
fi

rm -rf "$OUT"
mkdir -p "$OUT/res" "$OUT/classes" "$OUT/dex"

echo "== aapt2 compile =="
"$BUILD_TOOLS/aapt2" compile --dir src/main/res -o "$OUT/res/resources.zip"

echo "== aapt2 link =="
"$BUILD_TOOLS/aapt2" link \
  -o "$OUT/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest src/main/AndroidManifest.xml \
  -R "$OUT/res/resources.zip" \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  --version-code 1 \
  --version-name 1.0

echo "== javac =="
find src/main/java -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 --release 11 -Xlint:-options \
  -cp "$ANDROID_JAR" -d "$OUT/classes" @"$OUT/sources.txt"

echo "== d8 =="
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --release --min-api 21 \
  --output "$OUT/dex" @"$OUT/classes.txt"

(cd "$OUT/dex" && zip -q -u "../base.apk" classes.dex)

echo "== zipalign =="
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/base.apk" "$OUT/aligned.apk"

KEYSTORE="$OUT/signing.keystore"
if [ ! -f "$KEYSTORE" ]; then
  echo "== generating signing key =="
  keytool -genkeypair -keystore "$KEYSTORE" -alias receipt -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass receiptapp -keypass receiptapp \
    -dname "CN=Receipt Detail, OU=Mobile, O=WizardOfWanks, L=San Jose, ST=CA, C=US" >/dev/null
fi

echo "== apksigner =="
"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" --ks-pass pass:receiptapp --key-pass pass:receiptapp \
  --ks-key-alias receipt --out "$APK" "$OUT/aligned.apk"

"$BUILD_TOOLS/apksigner" verify --print-certs "$APK" | sed -n '1,6p'
echo "== badging =="
"$BUILD_TOOLS/aapt2" dump badging "$APK" | sed -n '1,8p'
ls -lh "$APK"
