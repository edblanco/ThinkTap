# Pro authentication

The pro flavor requires Google sign-in using Firebase Authentication and Android
Credential Manager. The free flavor has no authentication UI or provider dependencies.
Authentication belongs to the app, not the independently published trivia SDK.

## Firebase and Google setup

1. Create or select a Firebase project. Register an Android app with package
   `com.dosparta.triviagame2`. Free and pro currently share that application ID;
   changing it is outside this feature.
2. In **Authentication > Sign-in method**, enable **Google** and configure the
   project's support email.
3. Register the debug and production signing certificate SHA-1/SHA-256 fingerprints.
   Use `./gradlew :app:signingReport` for local signing. For Play distribution,
   register the Play App Signing certificate as well as any relevant local signing
   certificate.
4. Obtain the Firebase client API key, Firebase application ID (the `mobilesdk_app_id`,
   not the Android package), and project ID from the project's Android configuration.
5. Obtain the **Web application** OAuth client ID associated with the Google provider
   (`client_type: 3` in the downloaded Firebase configuration). Do not use the Android
   OAuth client ID or an OAuth client secret.
6. Set these Gradle properties in your user-level `~/.gradle/gradle.properties`,
   or supply them through your build environment:

   ```properties
   thinktap.firebase.apiKey=YOUR_FIREBASE_CLIENT_API_KEY
   thinktap.firebase.applicationId=YOUR_FIREBASE_MOBILESDK_APP_ID
   thinktap.firebase.projectId=YOUR_FIREBASE_PROJECT_ID
   thinktap.auth.webClientId=YOUR_WEB_OAUTH_CLIENT_ID
   ```

7. Build and install `./gradlew :app:installProDebug` on a device with Google Play
   services and an available Google account.

These are client configuration values embedded in the pro APK, not server credentials.
Never put service-account credentials or OAuth client secrets in the app or repository.
Restrict the client API key appropriately for the Firebase APIs and registered Android
application/signing certificates.

Firebase is initialized explicitly as the named `thinktap-pro` app. The Google Services
Gradle plugin and `google-services.json` are not required. This keeps free builds
independent of Firebase configuration and lets CI compile/test both flavors without
production account access. Missing configuration does **not** enable guest access:
pro displays an actionable configuration error and disables login.

## Session and game behavior

- First login requires internet. Firebase manages session persistence; the app does not
  store passwords or a local boolean that pretends a user is authenticated.
- A previously signed-in user can pass the login gate using the persisted Firebase
  session while offline. Uncached trivia and online translation still require internet.
- The app refreshes the Firebase user on entry/resume. Detected invalid/disabled/deleted
  users are signed out; network errors do not discard an existing local session.
  Offline devices cannot detect server-side revocation immediately.
- **Sign out** is available above all authenticated trivia screens. It pauses and
  saves the game before Firebase sign-out and clears Credential Manager session state.
  Save or provider errors are displayed explicitly.
- If an in-progress game cannot be saved during logout, the same controller retains
  its newer progress in memory. The next sign-in retries preservation before reading
  storage; continued write failures show a retryable startup error instead of restoring
  an older snapshot. Unsaved progress cannot survive process termination.
- Games are device-local and **shared across accounts** on the same installation.
  Logout, account changes, and detected session invalidation do not delete progress.
  Existing games saved before login are also retained.
- Language and daily reminder settings remain unchanged. Notification entry uses
  the same login gate; signed-out users cannot reach gameplay through reminders.
- No cloud sync, account-owned game storage, subscriptions, or payment entitlement
  verification is included. Login does not prove a paid pro entitlement.

## Tests and CI

```bash
./gradlew :app:testProDebugUnitTest :app:testFreeDebugUnitTest \
  :feature-trivia-ui:testDebugUnitTest
./gradlew :app:assembleProDebug :app:assembleFreeDebug \
  :app:lintProDebug :app:lintFreeDebug
./gradlew :app:dependencies --configuration freeDebugRuntimeClasspath
```

Deterministic authentication/gate tests use fake provider state and local in-memory
session storage; no Firebase configuration, production accounts, or repository secrets
are needed. Existing feature instrumentation tests directly exercise trivia screens,
not the application authentication gate. Pro gate tests explicitly establish fake
authentication before rendering the real trivia navigation flow.

Before release, also validate real Google sign-in, chooser cancellation, persistence
after app restart, offline entry, logout, account switching, and reminder entry on a
configured device. Confirm that free runtime dependencies contain neither Firebase
Authentication nor Credential Manager/Google ID.

## Production release obligations

Google sign-in can create a Firebase account automatically. Logout is **not** account
deletion. Before publishing, resolve applicable privacy disclosures and store
account-deletion requirements, including an in-app deletion flow and external deletion
request mechanism where required. This login implementation is not a substitute for
that release work.
