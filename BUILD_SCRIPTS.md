# Multi-Platform WizardOfWanks - Build Scripts

## 📱 Quick Build Commands

### Build All Platforms
```bash
./build-all.sh          # Unix
build-all.bat          # Windows
```

### Build Specific Platform
```bash
# iOS
eas build --platform ios

# Android  
eas build --platform android

# Web
npm run build

# macOS (Apple Silicon)
docker buildx build --platform linux/arm64 -t wizardofwanks:arm64 .
```

### Test on Device
```bash
# iOS Simulator
npm start -- --ios

# Android Emulator
npm start -- --android

# Web Browser
npm start -- --web
```

### Deploy to Stores
```bash
# App Store
eas submit --platform ios

# Play Store
eas submit --platform android

# Web Server
npm run build && scp -r build/* user@server:/var/www/
```

## 🏗️ Apple Silicon Support

### Build for Apple Silicon
```bash
docker buildx build \
  --platform linux/arm64 \
  -f Dockerfile.arm64 \
  -t wizardofwanks:arm64 .
```

### Run Multi-Architecture
```bash
docker-compose -f docker-compose.multiarch.yml up -d
```

### Check Platform
```bash
docker buildx ls
docker buildx create --use --platform linux/amd64,linux/arm64
```

## 📦 Build Matrix

| Platform | Command | Output | Size |
|----------|---------|--------|------|
| iOS | `eas build --platform ios` | .ipa | ~150MB |
| Android | `eas build --platform android` | .aab | ~100MB |
| Web | `npm run build` | dist/ | ~15MB |
| macOS ARM64 | `docker buildx build --platform linux/arm64` | image | ~500MB |

## ✅ Pre-Build Checklist

- [ ] Node.js 18+ installed
- [ ] Expo CLI installed (`npm install -g expo-cli`)
- [ ] EAS CLI installed (`npm install -g eas-cli`)
- [ ] Apple Developer account (for iOS)
- [ ] Google Play account (for Android)
- [ ] Docker installed (for Docker builds)

## 🚀 Deployment

### App Store (iOS)
1. Run: `eas build --platform ios`
2. Wait for build completion
3. Run: `eas submit --platform ios`
4. Follow App Store submission steps

### Play Store (Android)
1. Run: `eas build --platform android`
2. Wait for build completion
3. Run: `eas submit --platform android`
4. Follow Play Store submission steps

### Web Server
1. Run: `npm run build`
2. Deploy to server: `scp -r build/* server:/path/to/web/`
3. Access: https://your-domain.com

### Docker (macOS)
1. Run: `docker-compose -f docker-compose.multiarch.yml up -d`
2. Access: http://localhost:3000

## 📊 Current Status

✅ iOS: Ready for submission
✅ Android: Ready for submission
✅ Web: Ready to deploy
✅ macOS: Ready for Docker deployment
✅ Apple Silicon: Full support

## 📞 Troubleshooting

**Build fails:** Check Node.js version (need 18+)
**iOS issues:** Verify Apple team ID in eas.json
**Android issues:** Check Google Play credentials
**Web issues:** Verify API endpoints in config
**Docker ARM64:** Verify buildx setup with `docker buildx ls`

---

**Version:** 2.0.0
**Last Updated:** May 28, 2026
**Status:** Production Ready ✅
