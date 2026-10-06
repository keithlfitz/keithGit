# Rides and Shares

Passenger tablet for the back of a rideshare front seat. Businesses advertise to people already on an Uber or Lyft trip. The tablet plays a fullscreen slideshow, and each advertisement has a QR code the passenger scans for that business.

This is not a trip-request app. It does not talk to Uber or Lyft.

Version 1 is the passenger tablet only. Advertisements are a JSON file bundled in the app, so it runs with no backend. A later advertiser portal can replace that file.

## What the passenger sees

The tablet stays landscape, fullscreen, and awake. Every 15 seconds the slideshow advances. Each slide shows the ad creative, the business name, a short line, and a large QR code. The code encodes that ad's info URL and is drawn with a quiet zone so it can be scanned from the seat.

Five fictional sample businesses ship in the app: Harbor & Rye, Northline Eats, Lumen Hotel, Pike Street Books, and Cedar Dental. Their links use the reserved `.example` domain. Scanning shows a real URL; it will not open a live site until a portal replaces the catalog.

## Requirements

- JDK 17 or newer
- Android SDK 35 with build-tools 35
- A landscape Android tablet running Android 8.0 (API 26) or newer for a real install

Check that `adb` and the SDK are on your path, or point the project at the SDK:

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

`local.properties` is machine-specific and is not committed.

## Build

```bash
./gradlew assembleDebug test
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

Release:

```bash
./gradlew assembleRelease
```

The unsigned release APK is `app/build/outputs/apk/release/app-release-unsigned.apk`. Debug builds are `android:testOnly`, so a device owner set while you are provisioning can be removed with `adb`. Release builds are not test-only; clearing device owner from a release install takes a factory reset.

## Run

Install and launch on a device or tablet emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ridesandshares.passenger/.MainActivity
```

The activity is also a Home screen. On a personal phone, Android may ask you to pick a launcher. Keep your usual launcher. Open Rides and Shares from the app list instead. On a dedicated tablet, accept this app as Home so a reboot returns to the slideshow.

In Android Studio, open this directory and run the `app` configuration on a landscape tablet virtual device (API 26+).

## Advertisement catalog

`app/src/main/assets/ads.json` is a JSON list. Each object has:

| Field | Meaning |
| --- | --- |
| `id` | Stable id, unique in the list |
| `businessName` | Name on the slide |
| `tagline` | Short line under the name |
| `image` | Creative path inside `assets`, such as `images/harbor-rye.png` |
| `infoUrl` | `https` URL encoded in that slide's QR code |

Creatives live in `app/src/main/assets/images/`. To change the samples, edit the JSON and drop in new PNGs. `tools/generate_sample_creatives.py` redraws the bundled posters.

The app does not request network access. A bad catalog does not crash the kiosk; the screen explains what failed. A missing image still shows the name, the line, and the QR code.

## Kiosk mode

The slideshow hides the status and navigation bars, keeps the screen on, and ignores Back. That is immersive fullscreen. It is not yet locked: a passenger can still leave the app until lock task is on.

Use a dedicated tablet. Device owner cannot be set on a device that already has an account.

### Device owner (tablet in the car)

1. Factory-reset the tablet, or remove every account. Do not sign into Google yet.
2. Enable Developer options and USB debugging.
3. Install the debug APK (step above). Do not add an account.
4. Make this package the device owner:

```bash
adb shell dpm set-device-owner com.ridesandshares.passenger/.KioskDeviceAdminReceiver
```

5. Open the app:

```bash
adb shell am start -n com.ridesandshares.passenger/.MainActivity
```

On launch the app whitelists itself for lock task, turns off the status bar and keyguard, sets itself as the persistent Home app, and pins the slideshow. Home, Recents, and notifications stay unavailable. Reboot should return to the ads.

Leave lock task while the debug build is installed:

```bash
adb shell am task lock stop
adb shell dpm remove-active-admin com.ridesandshares.passenger/.KioskDeviceAdminReceiver
```

`remove-active-admin` works for a device owner only while the installed build is test-only. That is the debug APK. If the command is refused, factory-reset the tablet.

### Screen pinning (no device owner)

Use this when you are previewing on a phone or a tablet you cannot factory-reset.

1. On the device: Settings → Security → Screen pinning (some versions call it App pinning). Turn it on.
2. Launch Rides and Shares. It calls lock task. Confirm the pin prompt if Android shows one.
3. To unpin, follow the gesture shown on the prompt (often Overview and Back together).

If screen pinning is off and this package is not the device owner, the slideshow still fills the screen, but the system bars can be brought back and the passenger can leave. Use device owner for a tablet that stays in the car.
