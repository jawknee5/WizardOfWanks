# WizardOfWanks Enterprise - Project Root

📁 **WizardOfWanks/** - Complete application root directory

## 📂 Contents

### `/enterprise/` - Main Application
- **server.js** - Express backend (12KB)
- **public/index.html** - Frontend UI (28KB)
- **migrations.js** - Database setup
- **seed.js** - Sample data
- **package.json** - Dependencies
- **Dockerfile** - Container config
- **.env** - Environment variables
- **node_modules/** - Dependencies (installed)

### `/receipt-processor/` - Receipt OCR System
- Complete standalone receipt scanning application
- Advanced OCR with barcode generation
- Offline transaction queue
- User-approved manual sync
- See: `/receipt-processor/README.md`

### `/android-receipt/` - WaImart Android App
- Native "Receipt Detail" screen and item editor (Java, no Gradle)
- Pinch zoom, two-finger pan, tap the barcode to edit items
- Signed APK: `android-receipt/ReceiptDetail.apk`
- See: `/android-receipt/README.md` for full build and sideload steps

## 🚀 Quick Start

### Windows
```batch
START.bat
```

### macOS/Linux
```bash
chmod +x START.sh
./START.sh
```

## 📱 WaImart Android APK (Windows / macOS)

The Android app in `android-receipt/` is built straight from the Android SDK with
`build.sh` (no Gradle). You need **JDK 17**, **Android SDK platform 34**, **build-tools 34.0.0**
and **platform-tools** (`adb`); `build.sh` reads `JAVA_HOME` and `ANDROID_SDK_ROOT`.
The full walkthrough is in [`android-receipt/README.md`](android-receipt/README.md).

### Build on Windows (Git Bash)

```powershell
# once, in PowerShell
winget install EclipseAdoptium.Temurin.17.JDK
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"   # match installed folder
# unzip commandlinetools-win-*.zip into %LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest
setx ANDROID_SDK_ROOT "%LOCALAPPDATA%\Android\Sdk"
& "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

```bash
# every build, in Git Bash from the repo root
cd android-receipt
./build.sh            # -> android-receipt/ReceiptDetail.apk
```

### Build on macOS

```bash
brew install --cask temurin@17 android-commandlinetools
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
"$(brew --prefix)/share/android-commandlinetools/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
cd android-receipt && chmod +x build.sh install.sh && ./build.sh
```

### Sideload on Windows and macOS

Enable **Developer options → USB debugging**, connect the cable and accept the prompt:

```bash
adb devices                          # phone must show as "device"
adb install -r ReceiptDetail.apk     # run from android-receipt/
adb shell am start -n com.wizardofwanks.receipt/.ReceiptDetailActivity
```

Or run `cd android-receipt && ./install.sh`, which builds the APK when needed, installs
it and launches it. To install without a cable, copy `ReceiptDetail.apk` to the phone and
open it there (allow *Install unknown apps* when asked). Wireless debugging:

```bash
adb pair <phone-ip>:<pairing-port>   # 6-digit code from the phone
adb connect <phone-ip>:<adb-port>
adb install -r ReceiptDetail.apk
```

If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed copy came from a
different signing key: `adb uninstall com.wizardofwanks.receipt`, then install again.

### Signing keystore

`build.sh` signs with `android-receipt/keystore/signing.keystore` (alias `receipt`,
password `receiptapp`). It sits **outside** `build/` on purpose: the build deletes
`build/` each run, so keeping the key there would throw it away and silently produce a new
certificate every time. The first build creates the key if it is missing; all later
builds reuse it, so rebuilt APKs install over the previous version. The keystore is
gitignored — keep a backup.

## 🌐 Access

- **Application**: http://localhost:5000
- **API Health**: http://localhost:5000/api/health
- **Demo Login**: admin@wizardofwanks.com / password123

## 📊 System Status

✅ WizardOfWanks Enterprise running on port 5000
✅ PostgreSQL database connected
✅ Ready for use

## 📖 Documentation

- **README.md** - Project overview (if in this directory)
- **enterprise/README.md** - Enterprise app docs
- **receipt-processor/README.md** - Receipt processor docs
- **docker-compose.wiz.yml** - Service configuration

## 🛠️ Commands

```bash
# Start services
START.bat                                    # Windows
./START.sh                                   # Mac/Linux
docker-compose -f docker-compose.wiz.yml up -d

# Stop services
docker-compose -f docker-compose.wiz.yml down

# View logs
docker-compose -f docker-compose.wiz.yml logs -f

# Restart
docker-compose -f docker-compose.wiz.yml restart
```

## 📝 Project Structure

```
WizardOfWanks/
├── START.bat                 # Windows startup
├── START.sh                  # Unix startup
├── README.md                 # This file
├── enterprise/
│   ├── server.js
│   ├── public/
│   ├── package.json
│   ├── Dockerfile
│   └── .env
├── receipt-processor/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── README.md
└── docker-compose.wiz.yml
```

## ✅ What's Running

- **WizardOfWanks Enterprise** - Receipt management system
- **PostgreSQL** - Database
- **Redis** - Cache (if configured)
- **Grafana** - Monitoring (optional)

## 🔧 Configuration

Edit `.env` in the `/enterprise/` directory to configure:
- Database connection
- API ports
- Environment settings

## 📞 Support

- See `/enterprise/README.md` for WizardOfWanks docs
- See `/receipt-processor/README.md` for Receipt Processor docs
- Check logs: `docker-compose -f docker-compose.wiz.yml logs`

---

**Version**: 2.0.0 + 3.0.0 (Receipt Processor)
**Status**: ✅ Running
**Location**: H:\WizardOfWanks\

Ready to use! 🚀
