# Architecture

The app is a thin client: everything about the home (devices, rooms, actions, who may use what) is set up and decided on the hub. The app shows what the hub says and asks it to run actions. It keeps almost nothing.

## Layers

```
ui/            Compose screens, one file each, and AppViewModel (the screen state)
  ↓ calls
data/Hub       what the screens need from a hub: home, details, room, run, me, connection
  ├─ HubClient   the real hub, over the app API (HTTPS with the pinned key)
  └─ DemoHub     a sample home in memory, for trying and reviewing the app
data/          HubStore (paired hub, addresses), TokenStore (Keystore-encrypted token),
               Pairing (QR code and typed code), Pinning (the trust manager), Addresses (home or away), Models
```

- **UI** (`ui/`): `MainActivity` hosts one Compose tree. `App.kt` lays out the header and chooses between `PairScreen` and `HomeScreen`; `DetailsSheet`, `RoomScreen`, `ConnectionScreen` and `AboutScreen` open over it. Screens read one `UiState` and call `AppViewModel` methods; they hold only text being typed.
- **State** (`ui/AppViewModel.kt`): one `StateFlow<UiState>`. Every hub call runs in `viewModelScope`; failures become a message on screen, a refused token (401) forgets the pairing and shows the pairing screen. While the app is in front, the home screen and any open sheet are re-read every 5 seconds; nothing runs in the background.
- **Hub** (`data/Hub.kt`): the seam between screens and the network. `HubClient` implements it over the app API; `DemoHub` with fixed sample data. Pairing and the key check belong to `HubClient` only.
- **Storage** (`data/HubStore.kt`, `data/TokenStore.kt`): `SharedPreferences` for the hub's addresses, key fingerprint and names, with a data version and upgrade steps (like the hub's database migrations). The token is encrypted with an AES-GCM key in Android Keystore and excluded from backups and device transfers.

## The API contract

The app speaks version 1 of the hub's app API. Its requests and answers are described in the hub's OpenAPI document, [`openapi/app-api.json`](https://github.com/jkijewski/synbee/blob/main/openapi/app-api.json), which the hub generates from the same schemas it serves the routes with. `data/Models.kt` mirrors those answers; unknown fields are ignored, so a newer hub adding fields keeps working, and a breaking change would come with a new prefix (`/api/app/v2`) while the hub keeps serving v1. `ping` tells the app the hub's API version before pairing.

## Data flow

1. **Pairing:** the QR code (or typed address and code) gives the hub's addresses, its key fingerprint and a one-time code. `HubClient` pings the hub with that key pinned, then posts the code and the phone's name, and receives the token. `HubStore` keeps the addresses, fingerprint and names; `TokenStore` the token.
2. **Home screen:** `GET /home` gives the member's rooms (with colours), actions (with state, power and a status line) and status tiles (with words, a level, an optional colour and an explanation). Tabs, status tiles and action tiles are built from it; `GET /me` says whether the member may pause alerts.
3. **Run:** `POST /actions/{id}/run`; the home screen is re-read at once to show the new state. An answer with `notRun` means the action's conditions didn't hold; the app shows what wasn't true.
4. **Pause alerts:** `POST /alerts/pause` with minutes (0 resumes).
5. **Where to connect:** after each load, `GET /connection` updates the home addresses and the outside address the owner allows. On Wi-Fi the home address is tried first, elsewhere the outside one; the last address that answered is tried first next time.

## Security model

- **Pinned key, not certificate authorities.** One trust manager accepts exactly the hub key from pairing (SHA-256 of its SubjectPublicKeyInfo). A server with any other key fails the TLS handshake before a request is written, so it never sees the token or a pairing code. Host names aren't checked: the key is the identity, and the hub is often reached by its IP address.
- **No plain HTTP:** the network security config forbids cleartext traffic.
- **Token:** 256 bits from the hub, sent only as a bearer header to the pinned hub, stored encrypted with a Keystore key that never leaves the device's secure hardware, and never backed up. The hub keeps only its hash and can revoke it.
- **Permissions:** internet and network state only. The QR scanner is Google's code scanner from Play services: it runs in its own activity and returns only the text, so the app needs no camera permission.
- **What the app can do on the hub:** only what the owner allowed the member (see the hub's app users spec); nothing in the app changes configuration.

## Tests

`app/src/test` runs on the JVM:

- `PinningTest`: the real `HubClient` against local TLS servers with known keys: the paired key is accepted, another key is refused before the token is sent.
- `PairingTest`: QR code parsing and validation, typed codes, home and outside addresses.
- `DemoHubTest`: the sample home answers like a hub, and running actions changes state as on a hub.

Run them with `./gradlew test`. The hub's own tests check the server side of the same contract, including that the OpenAPI document matches what the hub serves.

## Libraries

AndroidX core, activity and lifecycle; Jetpack Compose with Material 3; OkHttp (TLS and HTTP); kotlinx.serialization (JSON); kotlinx.coroutines; Google's code scanner (Play services). Material Symbols (font, Apache 2.0) for room and action icons, fetched by `scripts/fetch-icon-font.sh` at a pinned commit. Versions are exact, in `gradle/libs.versions.toml`.
