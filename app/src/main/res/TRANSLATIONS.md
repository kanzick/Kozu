# UI translations

- `values/strings.xml` is the English source of truth.
- `values-vi/strings.xml` contains the Vietnamese translations.
- The app exposes **Follow system**, **English**, and **Tiếng Việt**. Other locales should only be added after their UI translations are complete.
- Add user-facing text as a named Android string resource, then call `stringResource(R.string.your_key)` in Compose or `context.getString(R.string.your_key, args...)` outside Compose.
- Keep format placeholders (`%1$s`, `%2$d`, and so on) consistent across locales. Use Android `plurals` for quantity-dependent wording.
- When adding a new string, update the English and Vietnamese resource files together. Build the app to let Android's resource compiler validate the XML and resource references.
- Add a locale to `locales_config.xml` and `AppLanguage` only after its complete resource file has been reviewed.

