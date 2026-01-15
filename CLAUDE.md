# CLAUDE.md

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
├── dataclient/      # Model classes and service layer for MediaWiki and Wikipedia APIs.
├── analytics/       # Model classes and service logic for our current analytics engine (Event Platform)
├── feed/            # Fragments and Views related to the Exlore Feed
├── talk/            # Activities and Views related to Wikipedia Talk pages.
├── page/            # Activities and Views for browsing Wikipedia articles in a WebView
├── edit/            # Activities and Views for editing Wikipedia articles
```
...and so on.

### Libraries and dependencies

- Jetpack Compose for any new features. View Bindings for legacy features.
  - Since the app offers four different color themes, always wrap Compose screens in the `BaseTheme` component which handles our custom themes.
  - Whenever possible, translate a legacy feature to Jetpack Compose before adding new functionality.
- Retrofit for network calls, with occasional direct usages of OkHttp.
- Coil for loading images.
- Room for database management.
- JUnit and Robolectric for unit tests.
- Espresso for instrumented tests.

## Code conventions

- ALWAYS prefer Jetpack Compose for new UI features, with a corresponding backing ViewModel class that handles state.
  - Whenever possible, use the components and extensions found in the `compose/` directory.
- ALWAYS prefer self-documenting names of variables, functions, and fields. Don't write redundant comments that explain what the next line does.
- NEVER run static analysis tools (ktlint, checkstyle, lint) during development. This should only be done as a final step, when ready to submit a pull request.
