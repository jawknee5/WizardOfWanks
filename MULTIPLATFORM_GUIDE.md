# WizardOfWanks - Multi-Platform Deployment Guide

## 📱 Platform Support

### iOS (Native)
- ✅ iPhone & iPad
- ✅ Apple Silicon (M1/M2/M3)
- ✅ iOS 13+

### Android (Native)
- ✅ Phone & Tablet
- ✅ Android 8+

### macOS (Native)
- ✅ Apple Silicon (ARM64)
- ✅ Intel (x86_64)
- ✅ Docker containers

### Web (PWA)
- ✅ All modern browsers
- ✅ Installable as app
- ✅ Offline capable

---

## 🚀 Build & Deploy

### 1. iOS Native App

**Prerequisites:**
- macOS with Xcode
- Apple Developer account

**Build:**
```bash
cd mobile
npm install
eas build --platform ios
```

**Submit to App Store:**
```bash
eas submit --platform ios
```

### 2. Android Native App

**Prerequisites:**
- Android Studio
- Google Play Developer account

**Build:**
```bash
cd mobile
npm install
eas build --platform android
```

**Submit to Play Store:**
```bash
eas submit --platform android
```

### 3. Web PWA

**Build:**
```bash
cd receipt-processor
npm install
npm run build
```

**Deploy:**
```bash
# Copy /build to your web server
scp -r build/* user@server:/var/www/wizardofwanks/
```

**Access:**
```
https://your-domain.com/wizardofwanks
```

### 4. macOS (Apple Silicon)

**Build Docker image:**
```bash
docker build -f Dockerfile.arm64 -t wizardofwanks:arm64 .
```

**Or multi-architecture:**
```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t wizardofwanks:latest \
  -f Dockerfile.multiarch .
```

**Run:**
```bash
docker-compose -f docker-compose.multiarch.yml up -d
```

---

## 📦 Project Structure

```
WizardOfWanks/
├── mobile/
│   ├── App.js                 # React Native entry
│   ├── package.json           # Expo config
│   ├── eas.json               # EAS Build config
│   └── screens/
│       ├── ScannerScreen.js
│       ├── EditorScreen.js
│       ├── QueueScreen.js
│       ├── AuditScreen.js
│       ├── ProfileScreen.js
│       └── HomeScreen.js
├── enterprise/                # Original backend
├── receipt-processor/         # Original frontend
├── Dockerfile.arm64           # Apple Silicon
├── Dockerfile.multiarch       # Multi-platform
└── docker-compose.multiarch.yml
```

---

## 🛠️ Configuration

### iOS App ID
Edit `mobile/eas.json`:
```json
{
  "ios": {
    "ascAppIdentifier": "com.wizardofwanks.enterprise",
    "appleTeamId": "YOUR_TEAM_ID"
  }
}
```

### Android Package ID
Edit `mobile/eas.json`:
```json
{
  "android": {
    "package": "com.wizardofwanks.enterprise"
  }
}
```

### API Endpoints
Edit `mobile/config/api.js`:
```javascript
export const API_URL = 'https://your-backend.com';
export const API_KEY = 'your-api-key';
```

---

## ✅ Testing

### iOS Simulator
```bash
cd mobile
npm start
# Press 'i' to open iOS Simulator
```

### Android Emulator
```bash
cd mobile
npm start
# Press 'a' to open Android Emulator
```

### Web
```bash
cd mobile
npm start -- --web
```

---

## 📊 Feature Parity

| Feature | iOS | Android | Web | macOS |
|---------|-----|---------|-----|-------|
| Camera scan | ✅ | ✅ | ✅ | ✅ |
| OCR | ✅ | ✅ | ✅ | ✅ |
| Offline queue | ✅ | ✅ | ✅ | ✅ |
| Real-time edit | ✅ | ✅ | ✅ | ✅ |
| Barcode gen | ✅ | ✅ | ✅ | ✅ |
| Audit trail | ✅ | ✅ | ✅ | ✅ |
| Manual sync | ✅ | ✅ | ✅ | ✅ |

---

## 🔐 Security

All platforms:
- ✅ Local encryption
- ✅ No automatic data sending
- ✅ User approval required for sync
- ✅ Audit logging enabled

---

## 📱 Installation

### iOS Users
1. Open App Store
2. Search "WizardOfWanks"
3. Tap Install
4. Open app

### Android Users
1. Open Play Store
2. Search "WizardOfWanks"
3. Tap Install
4. Open app

### Web Users
1. Visit https://wizardofwanks.example.com
2. Tap Menu → "Add to Home Screen"
3. Tap app icon

### macOS Users
1. Run: `docker-compose -f docker-compose.multiarch.yml up`
2. Open: http://localhost:3000

---

## 🚀 Version Management

**Current Version:** 2.0.0

**Release Channel:**
- development (internal testing)
- preview (beta testing)
- production (App Store/Play Store)

**Build Command:**
```bash
eas build --platform all  # All platforms
eas build --platform ios,android  # iOS + Android only
```

---

## 📞 Support

For issues with specific platforms:
- **iOS:** See iOS-specific troubleshooting
- **Android:** See Android-specific troubleshooting
- **Web:** See web PWA troubleshooting
- **macOS:** See Docker troubleshooting

---

**Status:** ✅ Multi-platform support enabled
**Last Updated:** May 28, 2026
**Platforms:** iOS, Android, Web, macOS (Apple Silicon)
