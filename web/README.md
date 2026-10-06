# Rides & Shares tablet prototype

A clickable web prototype of the in-car advertising tablet. It is separate from any Android passenger or driver app and does not change those apps.

Passengers see a landscape slideshow. A driver phone can pair with the tablet and send a destination and remaining distance. An admin desk shows sample analytics for the five sample businesses.

## Open it

From the repository root:

```bash
python3 -m http.server 8080 --directory web
```

Then open http://localhost:8080

You can also open `web/index.html` directly in a browser. Everything the page needs is in this folder.

## Screens

A **Preview** bar sits at the bottom of every screen.

- **Passenger tablet.** Full-screen slideshow of Harbor & Rye, Northline Eats, Lumen Hotel, Pike Street Books, and Cedar Dental. Each slide has a QR code for that business's `infoUrl` on the `.example` domain. Arrows, the dots, or the ring advance the ads. The show also moves on its own every 15 seconds. The ring is the ad countdown. After the driver phone sends a route, the top of the screen shows miles remaining, a countdown to arrival, and a car moving along the trip.
- **Driver phone.** Simulates the driver companion over Bluetooth. Search for the tablet, tap it to pair, choose a destination, then **Send to tablet**. The demo clock runs faster than a real ride so the car is easy to see move.
- **Admin.** Sample impressions, QR scans, campaigns, tablets, and rides. Playing the slideshow adds impressions. Tapping a QR code records a sample scan. You can pause an ad from its campaign page.

## Catalog

`ads.json` is the advertisement list: `id`, `businessName`, `tagline`, `image`, and `infoUrl`. The page uses the same five ads. QR symbols are drawn at error correction level Q and contain only the info URL.

## Fonts

Fraunces and Outfit are included under the SIL Open Font License, version 1.1. See `fonts/OFL-fraunces.txt` and `fonts/OFL-outfit.txt`.
