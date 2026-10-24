## Android releases

GitHub Actions builds a debug APK on pushes to `main` and `release/**`, and for pull requests to `main`. Download it from the run's **Artifacts** section.
To publish a signed APK, select the `main` branch and run **Release Android APK** manually from the **Actions** tab. Enter both values:
- `version_name`: Android version name in `MAJOR.MINOR.PATCH` format, such as `1.2.0`.
- `version_code`: positive integer greater than the previous release's code.
