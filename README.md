# Rides and Shares

Passenger tablet for the back of a rideshare front seat. Businesses advertise to people already on an Uber or Lyft trip. The tablet plays a fullscreen slideshow, and each advertisement has a QR code the passenger scans for that business.

This is not a trip-request app. It does not talk to Uber or Lyft.

Advertisements are a JSON file bundled in the passenger app, so the slideshow runs with no backend. A later advertiser portal can replace that file.

A second app, installed on the driver's phone, opens Google Maps navigation and sends the remaining driving distance to the tablet on the same Wi-Fi. The tablet cannot read the Google Maps app on its own.

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

The passenger debug APK is `app/build/outputs/apk/debug/app-debug.apk`. The driver debug APK is `driver/build/outputs/apk/debug/driver-debug.apk`.

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

A bad catalog does not crash the kiosk; the screen explains what failed. A missing image still shows the name, the line, and the QR code. The passenger app uses the network only to receive the driver's distance updates on the local Wi-Fi.

## Distance to the destination

The passenger tablet shows a banner above the ads. Until the driver phone is sharing, the banner says it is waiting and prints this tablet's address, such as `192.168.4.21:8787`. When an update arrives it shows the remaining driving distance and time, for example `4.2 mi · 12 min`, and the destination the driver entered.

The two phones have to be on the same Wi-Fi. The driver's hotspot works. Updates are a small UDP packet to port 8787, about every 10 seconds. If nothing arrives for 45 seconds the banner goes back to waiting, so a phone that left the car does not leave a stale distance on screen.

The passenger app does not open Google Maps and does not scrape another app's screen. The driver app asks Google's Directions API for the driving distance from the phone's current location, then opens the Google Maps app on the turn-by-turn screen for the same destination.

### Driver phone

Add a Directions API key to `local.properties` (this file stays on your machine):

```
MAPS_API_KEY=your-key
```

Enable the Directions API for that key, then build and install the driver app:

```bash
./gradlew :driver:assembleDebug
adb install -r driver/build/outputs/apk/debug/driver-debug.apk
```

On the driver phone, type the address shown on the tablet and the destination, then tap **Start and open Google Maps**. Allow location. Google Maps navigates; the passenger tablet updates as the car moves. Stop sharing from the same screen.

Without a key the driver app still builds, and it tells the driver to add `MAPS_API_KEY` instead of sending a distance.

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
