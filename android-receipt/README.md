# WaImart — Receipt Detail (Android)

Native Android receipt screen and item editor. Built straight from the Android SDK
with `build.sh` (`aapt2 → javac → d8 → zipalign → apksigner`): no Gradle, no AndroidX.

- App name **WaImart**, screen title **Receipt Detail**, package `com.wizardofwanks.receipt`
- Pinch to zoom; two-finger press and drag moves the whole receipt
- Tap the barcode to open the item editor: one item per line as `description$price`,
  with Save / Duplicate / Undo / Redo; the draft is autosaved
- Output: `android-receipt/ReceiptDetail.apk` (signed, installable)

## Requirements (Windows and macOS)

| Tool | Version | Notes |
|------|---------|-------|
| JDK 17 | Temurin/OpenJDK 17 | `JAVA_HOME` must point at it |
| Android SDK | `platforms;android-34`, `build-tools;34.0.0`, `platform-tools` | `ANDROID_SDK_ROOT` must point at it |
| Python 3 | 3.x | only used to regenerate launcher icons if they are missing |
| Bash | Git Bash on Windows, Terminal on macOS | `build.sh` and `install.sh` are bash scripts |

`build.sh` reads `JAVA_HOME` and `ANDROID_SDK_ROOT` (falling back to
`$HOME/tools/jdk17` and `$HOME/android-sdk`) and stops with a clear message if the
SDK or build-tools are missing. Version 34 build-tools are required; other build-tools
versions are not used by the script.

## Build the signed APK on Windows

Install the JDK and SDK once, in PowerShell:

```powershell
# 1. JDK 17
winget install EclipseAdoptium.Temurin.17.JDK
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"   # match the installed folder

# 2. Android command-line tools: download commandlinetools-win-*.zip from
#    https://developer.android.com/studio#command-tools and unzip it so that
#    sdkmanager.bat ends up in %LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin
setx ANDROID_SDK_ROOT "%LOCALAPPDATA%\Android\Sdk"

# 3. SDK packages (open a new terminal first so the variables are set)
& "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

Then build in **Git Bash** (not `cmd.exe`), from the repository root:

```bash
cd android-receipt
./build.sh
# -> android-receipt/ReceiptDetail.apk
```

## Build the signed APK on macOS

```bash
# 1. JDK 17
brew install --cask temurin@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)      # add to ~/.zshrc to keep it

# 2. Android command-line tools
brew install --cask android-commandlinetools           # or unzip the tools into
                                                       # ~/Library/Android/sdk/cmdline-tools/latest yourself
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"

# 3. SDK packages (adjust the path if you unzipped the tools by hand)
"$(brew --prefix)/share/android-commandlinetools/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4. Build
cd android-receipt
chmod +x build.sh install.sh
./build.sh
# -> android-receipt/ReceiptDetail.apk
```

## Sideload the APK (Windows and macOS)

Enable **Developer options → USB debugging** on the phone, connect the USB cable and
accept the *Allow USB debugging* prompt. `adb` ships in
`$ANDROID_SDK_ROOT/platform-tools` (Windows: `%ANDROID_SDK_ROOT%\platform-tools`).

```bash
adb devices                          # the phone must be listed as "device"
adb install -r ReceiptDetail.apk     # -r keeps the app data on reinstall
adb shell am start -n com.wizardofwanks.receipt/.ReceiptDetailActivity
```

`./install.sh` does the same in one step, and builds the APK first when it is missing:

```bash
cd android-receipt
./install.sh              # build if needed, install, launch
./install.sh --no-build   # install the existing ReceiptDetail.apk
./install.sh --rebuild    # always rebuild first
```

Without a cable, copy `ReceiptDetail.apk` to the phone (USB file transfer, cloud drive,
email) and open it in the Files app; Android asks you to allow *Install unknown apps*
for that app the first time.

Wireless debugging (Android 11+): **Developer options → Wireless debugging**, then

```bash
adb pair <phone-ip>:<pairing-port>    # enter the 6-digit code shown on the phone
adb connect <phone-ip>:<adb-port>
adb install -r ReceiptDetail.apk
```

If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed copy was signed
with a different key. Remove it once, then install again:

```bash
adb uninstall com.wizardofwanks.receipt
```

## Signing keystore

The signing key is kept at **`android-receipt/keystore/signing.keystore`**
(alias `receipt`, store and key password `receiptapp`), deliberately **outside**
`build/`, which `build.sh` deletes on every run. The first build generates the key if
it is missing; every later rebuild reuses the same file, so the APK is always signed
with the same certificate and installs over the previous version.

The keystore and the built APK are gitignored — back up `keystore/signing.keystore`
somewhere safe. Deleting it makes the next build a brand-new certificate, and phones
with the old app installed then need `adb uninstall` before installing again.

`./build.sh` prints the certificate fingerprint it signed with (`apksigner verify --print-certs`).
