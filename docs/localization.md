# Localization

ThinkTap ships in **English** (default), **German**, **Spanish**, and **Simplified Chinese**.
Two kinds of text need translating:

| Text | How it is localized |
|---|---|
| UI strings | Android string resources (`values-de`, `values-es`, `values-zh-rCN`) in `app` and `feature-trivia-ui` |
| Trivia content from OpenTDB (questions, answers, categories) | Translated on the device at runtime with [ML Kit Translation](https://developers.google.com/ml-kit/language/translation) |

## Choosing the language

- By default the app uses the system language. Android's resource matching picks the first
  supported language in the user's system list. If none is supported, the app uses English.
  Traditional Chinese (`zh-TW`, `zh-HK`, `zh-Hant`) is not supported and falls back the same way.
- The Setup screen has a **Language** picker ("System default", English, Deutsch, Español,
  简体中文) that overrides the system language. It uses the AndroidX per-app language API
  (`AppCompatDelegate.setApplicationLocales`) through `AppLanguageManager` in `app`:
  - Android 13+ stores the choice and lists it under the system's per-app language settings.
    `generateLocaleConfig` in `app/build.gradle.kts` publishes the supported languages, and
    `res/resources.properties` declares English as the default language.
  - Android 12 and lower store the choice through AppCompat's `autoStoreLocales` service, which is
    declared in the manifest.
- `MainActivity` extends `AppCompatActivity`, so changing the language recreates the activity.
- `localeFilters` limits library resources such as AppCompat and Material to the supported
  languages.

## Content translation

The content language always follows the language the UI was resolved to.
`R.string.content_language_tag` is translated in each `values-*` folder. `TriviaNavHost` reads it
and passes it to `TriviaViewModel.onContentLanguageChanged`, which forwards it to
`TriviaSdk.setContentLanguage` and reloads categories if they were already loaded.
The app owns locale preferences and UI strings; the independent SDK owns content
localization.

The SDK's internal `TranslatingTriviaRepository` wraps the OpenTDB repository and returns English content unchanged.
For any other language it calls `TriviaContentTranslator`, which:

- translates each distinct string only once, and builds both `options` and `correctAnswer` from
  the same translations so answer checking still works;
- keeps the English answers for a question if two answers would translate to the same text;
- leaves boolean `True`/`False` answers and the `difficulty` value in English. The UI shows them
  using `answer_true`/`answer_false` and the `difficulty_*` strings.

`MlKitTextTranslator` downloads a model of about 30 MB per language the first time that language
is used. The download is allowed on any network and times out after 60 seconds. Translations are
cached in memory for the life of the process.

### When translation fails

If the model can't be downloaded or a translation fails, the repository returns the original
English content and sets `translationUnavailable`. The Setup and Trivia screens then show a
banner with the `translation_unavailable_notice` string.

### Games in progress

Saved games store the questions that were already translated. A resumed game stays in the
language it started in, even if the app language has changed since.

## Adding a language

1. Add an entry to `AppLanguage` in `sdk/trivia-sdk-core`. Add its ML Kit code in
   `MlKitTextTranslator` in `sdk/trivia-sdk-android` and its display name string in the
   app's `LocalizedContent.kt`.
2. Add a `values-<qualifier>/strings.xml` to `app` and `feature-trivia-ui` with every string that
   can be translated, including `content_language_tag`.
3. Add the qualifier to `localeFilters` in `app/build.gradle.kts`.
4. Run `./sdk/gradlew -p sdk check publish`, then `./gradlew lintDebug` to catch missing
   UI translations (`MissingTranslation`).
