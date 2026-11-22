# SynBee app: privacy policy

Effective 24 November 2026. Applies to the SynBee app for Android (package `pl.kijewski.synbee`), published by Jacek Kijewski.

## In short

**Nothing leaves your network.**¹ The SynBee app talks only to **your own SynBee hub**, a program that runs on your own Synology NAS. The publisher of the app runs no servers for it and receives none of your data. There is no account, no analytics, no advertising and no tracking.

¹ Two exceptions, both described below. When you use the app away from home (only if your hub's owner allows it), its requests to your hub travel over the internet between your phone and your home's router, encrypted with TLS and accepted only by your own hub's key; they go to your hub, never to the publisher or any cloud service. And if you scan the pairing QR code, Google's code scanner, part of Google Play services, may send Google diagnostic information about the scanner.

## What the app does with data

| Data | Where it goes | Why |
| --- | --- | --- |
| The hub's addresses (home and, if set, outside), the fingerprint of its key, your member name and the phone's name | Stored on the phone | To find your hub and check that it is really yours |
| A phone token issued by your hub when pairing | Stored on the phone, encrypted with a key kept in Android's Keystore; excluded from backups and device transfers | To prove to your hub that this phone was paired |
| The pairing code, the phone's model name (e.g. "Pixel 8"), and your requests (which rooms and actions to show, which action to run) | Sent to your hub only, over TLS, to an address you scanned or typed | To pair the phone and to show and run what the hub's owner allowed you |
| What your hub sends back: your rooms, actions and the state of devices | Shown on screen; not stored | To show them to you |

The app sends nothing to the publisher or to any other server. It checks every connection against the key of the hub it was paired with, and sends nothing, not even the pairing code, to a server with another key.

Your hub is run by its owner (you, or the person who invited you). It keeps a list of paired phones with their names and when each was last used, and a log of which phone ran which action. That data stays on the hub's NAS and is governed by its owner, not by this policy.

## Camera and the code scanner

To pair, the app can scan the hub's QR code with **Google's code scanner**, part of Google Play services. The scanner runs on the phone and returns only the scanned text to the app; the app itself never gets camera access or images. Google Play services may collect diagnostic and usage information about the scanner (such as device model, OS version and error codes) under Google's own terms: see [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure) and [Google's privacy policy](https://policies.google.com/privacy). You can avoid the scanner by typing the hub's address and the pairing code instead.

## Permissions

- **Internet** and **network state**: to reach your hub, and to try its home address first when on Wi-Fi.

No location, contacts, storage, microphone or camera permission is requested.

## Sharing and selling

The publisher does not collect, share or sell any data from the app.

## Keeping and deleting data

Everything the app stores is on your phone. To delete it, choose ⋮ → Forget hub in the app, clear the app's storage, or uninstall it. The hub's owner can also revoke the phone on the hub's App users page, which makes the token useless.

## Children

The app is not directed at children and collects no data from anyone.

## Changes

Changes to this policy are published at this address, with a new effective date, and listed in the app's release notes.

## Contact

Jacek Kijewski, jacek.kijewski.gdynia@gmail.com
