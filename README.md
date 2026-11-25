# SynBee for Android

The mobile app for [SynBee](https://github.com/jkijewski/synbee), a private Zigbee hub on a Synology NAS. It is for the people who live in the home ("members"); everything about the home is set up in SynBee's web interface, never in the app.

> **Status:** 1.0.0, prepared for Google Play. The app pairs with a hub, shows the member's rooms as tabs to swipe between, with their actions as tiles, and runs an action with a tap. Switching single devices comes later.

## What it does

- **Pairs by QR code** from the hub's **App users** page, or by typing the hub's address and the pairing code, after comparing the hub's key with the one the page shows.
- **Shows rooms and actions** (`GET /api/app/v1/home`): one tab per room, in the hub's order and colours, swiped left and right; each room's actions as tiles with their icons. Actions in no room are under "Other". A tap runs the action.
- **Shows state:** a tile is filled in its room's colour when what it switches is on, grey when off, and has a thick border when only some is on; a check marks an action whose devices already show what it sets. A line under the name says "Off", "2 of 3 on" or the like. States are re-read every 5 seconds while the app is in front.
- **Status tiles** in each room, before its actions: devices, sensor groups and checks the owner chose, in words and colours (temperatures blue to red); a tap explains them.
- **"Not run"**: an action whose conditions don't hold says why.
- **Pause alerts** (⋮) for people the owner allows: nothing goes to Synology Chat for an hour, eight hours or until 7:00.
- **Long press** on a tile shows what the action touches, device by device; on a room tab, the room's devices.
- **About** (⋮ → About): what the app is, the author, the MIT licence in full and the no-warranty notice.
- **Demo:** "Try the demo" on the pairing screen shows a sample home kept in the app, for trying it without a hub. Nothing is sent anywhere.
- **Sends a test request** (`GET /api/app/v1/me`, in the ⋮ menu).
- **At home and away**, at an address and port of your home, with no cloud account: the hub's home address on Wi-Fi, and away from home the outside address and port (a DDNS name such as `myhome.synology.me` and the port the router forwards). The hub announces it once the owner switches it on, or it can be typed under ⋮ → Connection. Pairing works at home or away.

## How it connects

- **Trust:** the app trusts exactly the hub's public key, compared by its SHA-256 SPKI fingerprint from the pairing code. That replaces certificate authorities and host names, so the hub is reached by its IP address with no warnings. A server with another key receives nothing: no token and no pairing code.
- **Token:** issued once by the hub when pairing, kept encrypted with an Android Keystore key. The hub stores only its hash and can revoke it; the app then asks to pair again.
- **API:** `/api/app/v1/` on the hub's app port (default 8443), described in the hub's [OpenAPI document](https://github.com/jkijewski/synbee/blob/main/openapi/app-api.json) (generated from the hub's code and checked by its tests) and [app API spec](https://github.com/jkijewski/synbee/blob/main/openspec/specs/app-api/spec.md). The app checks the version in `ping`.
- **QR scanning** uses Google's code scanner from Play services, which needs no camera permission.

## Building

Requirements: JDK 17 or later, and the Android SDK with platform 37 (compile) and build-tools 36.1 (Android Studio, or the command-line tools). Put `sdk.dir=…` in `local.properties`, or set `ANDROID_HOME`.

```bash
scripts/fetch-icon-font.sh   # once: the Material Symbols font (Apache-2.0) at a pinned commit, checked by SHA-256
./gradlew test assembleDebug
```

The debug APK is in `app/build/outputs/apk/debug/`. Install it with `adb install`, or open the folder in Android Studio.

To try it without a NAS, run the hub's simulator (see the hub's README) with `HUB_APP_PORT=8443`, add a member on the App users page and pair. An emulator reaches the computer at its own Wi-Fi address.

## Documentation

| Document | Contents |
| --- | --- |
| [User guide](docs/user-guide.md) | Pairing, the home screen, tiles and their colours, away from home, the demo: step by step |
| [Architecture](docs/architecture.md) | Layers, screens, state, data flow, security model, tests |
| [Decisions](docs/decisions.md) | Why the app is built this way |
| [Releasing](docs/releasing.md) | Versions, signing, building the Play bundle, checks before a release |
| [Google Play listing](docs/play-store.md) | Store texts, Data safety answers, content rating, review access |
| [Privacy policy](PRIVACY.md) | What the app does with data: nothing leaves your network, except the app's own encrypted traffic to your hub when used away from home |
| [Changelog](CHANGELOG.md) | What changed in each version |

## Support

Questions and problems: [GitHub issues](https://github.com/jkijewski/synbee-android/issues) or jacek.kijewski.gdynia@gmail.com. If SynBee is useful to you, you can [buy me a coffee](https://buycoffee.to/jacek-kijewski).

## Trademarks

SynBee is an independent project, not affiliated with or endorsed by Synology Inc. Synology and Synology Chat are trademarks of Synology Inc.

## Licence

MIT, see [LICENSE](LICENSE). Provided as is, without warranty of any kind; it switches real devices, so don't rely on it for safety or security. The app's About page shows the licence and the libraries it uses.

## Icon

The bee on a NAS is SynBee's own icon, drawn by the hub's `package/synbee-spk/make_icons.py`. The launcher icons (`mipmap-*`) and `drawable-nodpi/bee.png` were rendered from it: an adaptive icon on a honey background, and a round one for older launchers.
