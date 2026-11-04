## Adding or changing a string

1. Add a stable resource to `values/strings.xml`.
2. For static UI text, use `KozuText` (import it as `Text`) or `kozuUiString()` for accessibility/snackbar copy.
3. Add the same resource name to each locale being translated. Keep format placeholders in the same order.
4. Run `python create_i18n.py` to regenerate the static source-string lookup, then run `python create_i18n.py --check` to verify it is current. 
⬇️"https://github.com/kanzick/kozu_i18n.git"

Dynamic phrases should use a format string resource and a matching pattern in `ui/i18n/KozuText.kt`; do not build translated sentences by concatenating translated fragments.