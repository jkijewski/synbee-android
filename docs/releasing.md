# Releasing

## Versions

`versionName` follows semantic versioning (1.0.0, 1.0.1, 1.1.0); `versionCode` goes up by one with every build uploaded to Google Play, never reused. Both are in `app/build.gradle.kts`. Each release gets a section in [CHANGELOG.md](../CHANGELOG.md); its first lines become the Play release notes (500 characters at most).

## Signing

Release builds are signed with the SynBee release key, kept outside the repository (decision A4): a PKCS12 keystore and a properties file with its passwords, in the maintainer's documents, with an offline copy. Gradle finds them through one line in `~/.gradle/gradle.properties`:

```
synbeeSigning=/path/to/synbee-release.properties
```

The properties file holds `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Without it, release builds come out unsigned.

On Google Play, use **Play App Signing**: this key becomes the **upload key**, and Google holds the app signing key. A lost upload key can then be replaced through Play support; the app itself keeps its identity.

## Building

```bash
scripts/fetch-icon-font.sh          # once per checkout
./gradlew test bundleRelease assembleRelease
```

- `app/build/outputs/bundle/release/app-release.aab`: the Android App Bundle for Google Play.
- `app/build/outputs/apk/release/app-release.apk`: the same build as an APK, for installing directly (`adb install -r`).
- `app/build/outputs/mapping/release/mapping.txt`: R8's map; upload it with the bundle so Play shows readable crash stacks. Keep it with the release.

## Checks before a release

1. `./gradlew test` passes.
2. On a real phone, with the release APK (R8 shrinks and renames code, which a debug build doesn't show):
   - pair by QR code and by typed code, at home and on mobile data;
   - tap actions, long-press a tile and a room tab, open Connection and About and their links;
   - "Try the demo" from the pairing screen, and "Leave the demo";
   - forget the hub, and check a revoked phone returns to pairing.
3. The hub's OpenAPI document still matches `data/Models.kt` (new fields are optional for older hubs).
4. CHANGELOG.md has the release's notes; `versionCode` and `versionName` are raised.
5. Tag the commit `vX.Y.Z` after the upload.

## Google Play

The listing, Data safety answers, content rating and review notes are in [play-store.md](play-store.md).
