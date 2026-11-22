# Decisions

The hub's decision records cover the parts both sides share: [members and pairing (0020)](https://github.com/jkijewski/synbee/blob/main/docs/decisions/0020-app-members-and-pairing.md), [the app API (0021)](https://github.com/jkijewski/synbee/blob/main/docs/decisions/0021-app-api.md) and [the app as a separate project (0022)](https://github.com/jkijewski/synbee/blob/main/docs/decisions/0022-android-app.md). These are the app's own, as the app is now; records are updated in place when a decision changes.

## A1. Kotlin, Jetpack Compose and few libraries

OkHttp for TLS with a custom trust manager, kotlinx.serialization for JSON, and Google's code scanner for QR codes. No dependency injection framework or database: the app holds one screen state and a handful of stored values, and its parts are wired by hand in the view model.

## A2. The pinned key decides, before anything is sent

Every request goes through one trust manager that accepts exactly the paired key. Pairing by QR code takes the key from the code; pairing by typed code reads the key first and shows its short form for the user to compare with the hub's page. In both cases the pairing code is sent only to a server with that key.

## A3. Home first, the outside address away

As Synology Photos does: an outside address and port, announced by the hub or typed by the user, used when not on Wi-Fi (and as a fallback on Wi-Fi). The pinned key makes the outside address safe to try: a wrong server gets nothing. Pairing works at home or away: from the QR code's outside address, or a typed one.

## A4. One release key, kept outside the repository

Android installs an update only if it is signed with the same key as the installed app, so losing the key means uninstalling, losing the pairing and pairing again. The key is a PKCS12 keystore (RSA 4096, valid 100 years) kept with its passwords in a properties file in the maintainer's documents, never in the repository; Gradle finds it through the `synbeeSigning` property in `~/.gradle/gradle.properties`. Without that file a release build comes out unsigned rather than signed with another key. Keep a copy of both files offline.

## A5. A demo with a sample home, inside the app

Most of the app needs a SynBee hub on a Synology NAS, which store reviewers and curious users don't have. "Try the demo" on the pairing screen swaps the hub for `DemoHub`, a sample home kept in memory that answers like a hub (rooms, actions with state, devices in words) and changes state when an action runs. It sends nothing anywhere and doesn't touch a real pairing. Screens talk to a `Hub` interface, so the demo exercises the same screens as a real hub; screenshots for the store come from it, with no real home's names in them.
