# Google Play listing

Everything the Play Console asks for, ready to paste. English only for now.

## App details

| Field | Value |
| --- | --- |
| App name (30) | SynBee |
| Package | `pl.kijewski.synbee` |
| Category | House & Home |
| Developer name | Jacek Kijewski |
| Contact email (public) | jacek.kijewski.gdynia@gmail.com |
| Website | https://github.com/jkijewski/synbee |
| Privacy policy | https://github.com/jkijewski/synbee-android/blob/main/PRIVACY.md |
| Price | Free, no in-app purchases, no ads |

### Short description (80 characters at most)

> Run your SynBee home actions from your phone: private, local, no cloud.

### Full description (4000 characters at most)

> SynBee is a private Zigbee hub that runs on your own Synology NAS. This app is its remote control for the people who live in the home.
>
> **What it does**
> • Shows your rooms as tabs to swipe between, each with its colour and icon.
> • Shows the actions the hub's owner set up, such as "Movie time" or "Good night", as tiles. Tap one to run it.
> • Colours each tile by what it switches: on, partly on or off. A check shows an action whose lights already look the way it sets them.
> • Long press a tile to see what it switches, device by device. Long press a room to see its devices: lights, sensors, batteries.
> • Works at home over Wi-Fi and, if the hub's owner allows it, away from home over the internet, like Synology Photos.
>
> **Private by design**
> • Nothing leaves your network.* The app talks only to your own SynBee hub. There are no accounts, no cloud, no analytics and no ads.
> • Pairing uses a one-time QR code from the hub. The app then trusts exactly your hub's key, so no other server can pretend to be it.
> • The hub's owner decides which rooms and actions each person may use. Nothing in the app can change the home's setup.
>
> **What you need**
> • A Synology NAS running the SynBee package, with a network Zigbee coordinator.
> • The hub's owner adds you on SynBee's App users page and shows you a pairing code.
>
> No hub yet? Choose "Try the demo" to see the app with a sample home.
>
> * Away from home, if your hub's owner allows it, the app's requests travel over the internet to your home, encrypted and accepted only by your own hub. Scanning the pairing QR code uses Google's code scanner, which may send Google diagnostic data.
>
> SynBee is free and open source (MIT licence). It comes without warranty: don't rely on it for safety or security.

## Graphics

| Asset | Size | Source |
| --- | --- | --- |
| App icon | 512 × 512 PNG | [icon-512.png](play-store/icon-512.png) |
| Feature graphic | 1024 × 500 PNG | [feature-1024x500.png](play-store/feature-1024x500.png) |
| Phone screenshots | 2–8, 9:16, at least 1080 px on the short side | From the demo (no real home data): the Living room tab, a tile's details sheet, the Bedroom room page, the pairing screen, About |

Both graphics are drawn by `scripts/make_store_graphics.py` from the hub's icon code. Take screenshots with the release build in demo mode: `adb exec-out screencap -p > shot.png`.

## Data safety

Answers for the Data safety form, matching [PRIVACY.md](../PRIVACY.md):

- **Does your app collect or share any of the required user data types?** Yes, because of Google's code scanner (below). The app's own code sends data only to the user's own hub, which the developer neither runs nor can access, so it is not collected by the developer.
- **Data collected by the code scanner (Google Play services ML Kit):** per [Google's ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure), declare **App info and performance → Diagnostics** (and **Device or other IDs** only if the disclosure page lists it for the code scanner at the time of filling in). Purpose: Analytics. Collected, not shared. Processed ephemerally: no. Required (users can avoid the scanner by typing the code, but the SDK is always present).
- **Is all of the user data collected by your app encrypted in transit?** Yes.
- **Do you provide a way for users to request that their data is deleted?** Yes: everything the app stores is on the phone and is removed by "Forget hub" or uninstalling; on the hub, the owner revokes the phone.
- **Data shared with third parties:** none.

Check the ML Kit disclosure page again before submitting; Google updates it.

## Content rating

Questionnaire category: **Utility, productivity, communication or other**. No violence, sexual content, profanity, drugs, gambling or user-generated content; users can't communicate with each other; no location sharing; no purchases. Expected rating: Everyone / PEGI 3.

## Target audience

18 and over, not designed for children. No ads.

## App access (for Google's reviewers)

The app needs a SynBee hub on a Synology NAS for most of its functions. Give reviewers this text under **App access → All or some functionality is restricted**:

> SynBee controls a Zigbee hub running on the user's own Synology NAS, so pairing needs that hardware. To review the app without it, tap **Try the demo** on the first screen. The demo shows a sample home with rooms, actions and devices, and runs actions on that sample, exactly as with a real hub. Use ⋮ → Leave the demo to return. No login is needed.

## Other declarations

- **Ads:** no ads.
- **Government app / financial features / health:** no.
- **News app:** no.
- **Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE` only; no sensitive permissions to declare.
- **Foreground services, background location, exact alarms:** none.

## Release notes for 1.0.0 (500 characters at most)

> First release. Pair with your SynBee hub by QR code, swipe between rooms, tap actions, and see what each one switches, at home or away. No hub yet? Try the demo.
