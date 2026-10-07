# Rides & Shares demo

A self-contained browser prototype of an in-ride ad tablet and the screens around it. Open this folder in a browser. There is no install, no backend, and no API key.

## Open it

Open `index.html` in Chrome, Safari, Firefox, or Edge.

- From the file manager, double-click `index.html`, or
- From a terminal in this folder: `python3 -m http.server 8765` and visit `http://localhost:8765/`

JavaScript must be enabled. The page does not call Uber, Lyft, Google, or any other network API. It works offline after the files are on disk.

## What you can click

The left side (or the top bar on a narrower window) moves between:

1. **Passenger** — tablet kiosk. Ads rotate, each with a QR code and a countdown. A car moves from Broadway & 5th to BNA on a simulated trip.
2. **Driver sync** — phone screen. Connect, then transfer three ad files. Those ads join the slideshow. The transfer is simulated in the page. Bluetooth is not used.
3. **Advertiser** — campaigns, impressions, QR scans, scan rate, and a sample week chart.
4. **Fleet** — tablet status, battery, last sync, app version, and demo-only controls.
5. **Wallet** — a fictional driver's monthly ad earnings, uptime bonus, and referral bonus.

**Reset demo** clears the transfer so the extra ads leave the slideshow.

## Every figure is demo data

All businesses are fictional. They are not signed advertisers. Impression counts, scan rates, battery levels, earnings, and the Nashville trip (pickup, destination, 8.4 miles, 18 minutes) are sample numbers stored in `app.js`.

The moving car uses a schematic path and a sped-up demo clock so the sample trip is visible. It is not Google Maps and not a live route. The QR codes point at `ridesandshares.example` links, which are not real offers.
