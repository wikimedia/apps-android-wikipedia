# AGENTS.md

This repository is the official Wikipedia app for Android, with features for reading Wikipedia articles, exploring the Wikipedia ecosystem by searching and browsing related content, as well as editing and administering Wikipedia content.

## General architecture

- The `app` directory contains all the code for the app itself.
- The `analytics/testkitchen` directory is a work-in-progress analytics engine that will eventually integrate with the app, but not yet.

### Build flavors

- The `dev` flavor is used for day-to-day development and running on local devices by developers.
- The `alpha` flavor is used for continuous integration and QA testing, both manually and using GitHub Actions.
- The `beta` flavor is for building and deploying to the [Wikipedia Beta](https://play.google.com/store/apps/details?id=org.wikipedia.beta) app on the Play Store.
- The `prod` flavor is for building and deploying to the production [Wikipedia](https://play.google.com/store/apps/details?id=org.wikipedia) app on the Play Store and other third-party app stores.
- The `fdroid` flavor is for building and deploying to the [F-Droid store](https://f-droid.org/en/packages/org.wikipedia) specifically. This flavor is configured to exclude any closed-source packages or libraries. All such code is contained in the `src/extra` directory, which is included in all other flavors, but is stubbed out in the `fdroid` flavor.

### Project organization

Classes and packages are organized roughly by "feature":
```
/app/src/main/java/org/wikipedia/
├── dataclient/      # Model classes and service layer for MediaWiki and Wikipedia APIs
├── analytics/       # Model classes and service logic for our current analytics engine (Event Platform)
├── feed/            # Fragments and Views related to the Exlore Feed
├── talk/            # Activities and Views related to Wikipedia Talk pages
├── page/            # Activities and Views for browsing Wikipedia articles in a WebView
├── edit/            # Activities and Views for editing Wikipedia articles
```
...and so on.

### Libraries and dependencies

- Jetpack Compose for any new features. View Bindings for legacy features.
  - Since the app offers four different color themes, always wrap Compose screens in the `BaseTheme` component which handles our custom themes.
  - Whenever possible, translate a legacy feature to Jetpack Compose before adding new functionality.
- Retrofit for network calls, with occasional direct usages of OkHttp.
- Kotlinx.serialization for serializing and deserializing JSON objects from remote APIs and local storage.
- Coil for loading images.
- Room for database management.
- JUnit and Robolectric for unit tests.
- Espresso for instrumented tests.

### Miscellaneous

- SharedPreferences are encapsulated in `Prefs.kt`. If adding a new preference, follow the pattern in that file.
- When setting up an A/B test for any feature, subclass from `ABTest.kt`, which automatically assigns the current user into a test bucket.

## Code conventions

- To check if the app builds without errors: `./gradlew assembleDevDebug`
  - Do NOT run other static analysis tools (ktlint, checkstyle, lint) during development. This should only be done as a final step, when ready to submit a pull request.
- ALWAYS prefer Jetpack Compose for new UI features, with a corresponding backing ViewModel class that handles state.
  - Whenever possible, use the components and extensions found in the `compose/` directory.
  - Important: for Text composables that might display HTML text from spannable CharSequence strings (bold, italic, etc), use our `compose/components/HtmlText` composable instead of the standard Text() composable.
- ALWAYS prefer self-documenting names of variables, functions, and fields. Don't write redundant comments that explain what the next line does.
- Avoid using deprecated APIs and classes in new code whenever possible.
- Avoid using reflection, unless all other options are exhausted.

## Release notes

- Every previous release is denoted by a git tag of the form `r/...`. Release notes must be composed based on git commits that happened after the last release tag.
- To get the hash of the last release tag: git show-ref -s <last-release-tag>
- To get a list of commits since last release: git log <last-release-hash>..HEAD --oneline --no-merges
- Release notes should be terse, but understandable by a general audience. Exclude commits that are version bumps of dependencies.
