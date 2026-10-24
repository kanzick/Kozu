## Android releases

GitHub Actions builds a debug APK on pushes to `main` and `release/**`, and for pull requests to `main`. Download it from the run's **Artifacts** section.
To publish a signed APK, first add these repository secrets under **Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded Android signing keystore. In PowerShell, encode the keystore with `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\release.jks"))` and save the result directly as this GitHub secret.
- `ANDROID_KEYSTORE_PASSWORD`: Keystore password.
- `ANDROID_KEY_ALIAS`: Key alias in the keystore.
- `ANDROID_KEY_PASSWORD`: Password for that key.

Keep the keystore and its passwords private; do not commit them to the repository.

Then select the `main` branch and run **Release Android APK** manually from the **Actions** tab. Enter both values:

- `version_name`: Android version name in `MAJOR.MINOR.PATCH` format, such as `1.2.0`.
- `version_code`: integer from `1` to `2100000000`, greater than the previous release's code. Increase it for every release.

The workflow builds and verifies a signed APK, then publishes it as a GitHub Release tagged `v<version_name>`. A version tag can only be used once.
