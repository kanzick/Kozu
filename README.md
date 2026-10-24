## Android releases

GitHub Actions builds a debug APK on pushes to `main` `release/**`. Download it from the run's **Artifacts** section.

Then select the `main` branch and run **Release Android APK** manually from the **Actions** tab. Enter both values:
- `version_name`: Android version name in `MAJOR.MINOR.PATCH` format, such as `1.2.0`.
- `version_code`: integer from 1 ~ 2100000000, greater than the previous release's code. Increase it for every release.

The workflow builds and verifies a signed APK, then publishes it as a GitHub Release tagged `v<version_name>`. A version tag can only be used once.
