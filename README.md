# 🕵️ Impostore
An Undercover / Impostor-style party game for Android, built entirely with Jetpack Compose.
Perfect for groups, passing the phone around and just having fun together!

## 🚀 Features
- 🎮 Local multiplayer (pass-and-play)
- 🌍 Multi-language support
- 🔄 Dynamic language switching
- 👤 Customizable number of players
📱 Compatible with Android 7.0+
## 🛠️ Tech Stack
- Kotlin
- Jetpack Compose
## 📦 Building a Release
To generate a signed release APK:
```bash
./gradlew assembleRelease
```
Or to create an Android App Bundle:
```bash
./gradlew bundleRelease
```
⚠️ Keystore credentials are not included in this repo.
Configure your local gradle.properties like this:
```
RELEASE_STORE_FILE=/path/to/release.keystore
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=release
RELEASE_KEY_PASSWORD=your_password
```
## Installation
To install the app, just go to [Releases](https://github.com/guanciottaman/impostore/releases/), download and install the latest APK available and install it on your device.

## 📥 Building
- Clone the repository
- Open in Android Studio
- Configure your keystore if building a release
- Build & run
## 🔐 App Signing
The signing setup uses environment variables / gradle.properties to avoid hardcoding passwords in the code.
Do not commit:
- release.keystore
- any passwords or signing files
## 🎯 Roadmap
- [ ] Improved animations
- [ ] Online mode
- [ ] Persistent scoring system
- [ ] Theme customization
## 👤 Author
### Guanciottaman
Content creator & developer \
Made with Kotlin 💙
