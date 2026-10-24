~ LISTENING YOUR FAVORITE OSU! SONG ANYTIME ANYWHERE ~

## GitHub Releases

Every push to `main` automatically builds and signs a release APK, verifies its signature, and publishes it in GitHub Releases. You can also start a release manually from the **Actions** tab. Release versions are generated as `1.0.<workflow run number>`.

Before the first release, create a release keystore and add these repository secrets under **Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`: the keystore file encoded as Base64.
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password.
- `ANDROID_KEY_ALIAS`: the key alias.
- `ANDROID_KEY_PASSWORD`: the key password.

Create and back up the keystore securely. Do not commit it or its passwords. The same signing key must be kept for future app updates. On PowerShell, generate a keystore and copy its Base64 value:

```powershell
keytool -genkeypair -v -keystore .\kozu-release.jks -alias kozu-release -keyalg RSA -keysize 4096 -validity 10000
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\kozu-release.jks")) | Set-Clipboard
```

For the default PKCS12 keystore, use the same password for the keystore and key. After adding the four secrets, each push to `main` publishes a new APK automatically.
