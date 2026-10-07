/* Rides & Shares browser demo. Every number is local sample data. */
(function () {
  "use strict";

  var AD_MS = 6000;
  var PLAYBACK = 30;
  var TRIP_SECONDS = 18 * 60;
  var STORAGE = "ridesandshares-demo";

  var ADS = [
    {
      id: "hot-chicken",
      name: "Hot Chicken & Honey",
      tagline: "Crispy, sweet heat on Gallatin Pike.",
      neighborhood: "East Nashville",
      category: "Dinner",
      url: "https://ridesandshares.example/a/hot-chicken",
      theme: "chicken",
      tone: "light",
      motif: "",
      impressions: 48220,
      scans: 1642,
      week: [1420, 1680, 1710, 1590, 1840, 2100, 1860],
    },
    {
      id: "gulch-pour",
      name: "The Gulch Pour House",
      tagline: "Natural wine, two blocks off Division.",
      neighborhood: "The Gulch",
      category: "Evening",
      url: "https://ridesandshares.example/a/gulch-pour",
      theme: "pour",
      tone: "light",
      motif: '<div class="glass"></div>',
      impressions: 36910,
      scans: 1108,
      week: [980, 1100, 1240, 1180, 1420, 1680, 1320],
    },
    {
      id: "cumberland",
      name: "Cumberland Records",
      tagline: "New and used vinyl on Elliston Place.",
      neighborhood: "Elliston",
      category: "Shops",
      url: "https://ridesandshares.example/a/cumberland-records",
      theme: "records",
      tone: "light",
      motif: '<div class="vinyl"></div>',
      impressions: 22440,
      scans: 806,
      week: [640, 720, 690, 710, 860, 980, 740],
    },
    {
      id: "biscuit",
      name: "Belle Meade Biscuit Co.",
      tagline: "Buttermilk biscuits until 2 p.m.",
      neighborhood: "Belle Meade",
      category: "Breakfast",
      url: "https://ridesandshares.example/a/biscuit-co",
      theme: "biscuit",
      tone: "dark",
      motif: "",
      impressions: 41180,
      scans: 1977,
      week: [1680, 1720, 1660, 1700, 1540, 980, 860],
    },
    {
      id: "dental",
      name: "Riverside Dental",
      tagline: "New-patient visits. Evenings open.",
      neighborhood: "12South",
      category: "Care",
      url: "https://ridesandshares.example/a/riverside-dental",
      theme: "dental",
      tone: "dark",
      motif: "",
      impressions: 19650,
      scans: 589,
      week: [520, 610, 580, 640, 600, 410, 280],
    },
    {
      id: "jazz",
      name: "Lantern Jazz Room",
      tagline: "Tonight's set on Printers Alley starts at 8.",
      neighborhood: "Printers Alley",
      category: "Music",
      url: "https://ridesandshares.example/a/printers-alley",
      theme: "jazz",
      tone: "light",
      file: "printers-alley-jazz.ad",
      bytes: 1240000,
      syncedOnly: true,
      impressions: 8420,
      scans: 312,
      week: [180, 210, 240, 260, 320, 410, 280],
    },
    {
      id: "coffee",
      name: "Germantown Coffee Roasters",
      tagline: "Single origin at the walk-up window.",
      neighborhood: "Germantown",
      category: "Coffee",
      url: "https://ridesandshares.example/a/germantown-coffee",
      theme: "coffee",
      tone: "light",
      file: "germantown-coffee.ad",
      bytes: 980000,
      syncedOnly: true,
      impressions: 15330,
      scans: 690,
      week: [460, 510, 490, 520, 540, 380, 300],
    },
    {
      id: "plants",
      name: "12South Plant Shop",
      tagline: "Houseplants for the ride home.",
      neighborhood: "12South",
      category: "Home",
      url: "https://ridesandshares.example/a/plant-shop",
      theme: "plants",
      tone: "light",
      motif: '<div class="leaf"></div>',
      file: "12south-plant-shop.ad",
      bytes: 1410000,
      syncedOnly: true,
      impressions: 12110,
      scans: 412,
      week: [280, 340, 360, 390, 410, 450, 320],
    },
  ];

  var FLEET = [
    { id: "RS-0142", place: "Headrest · Broadway", status: "online", battery: 78, charging: false, syncLabel: "26 min ago", version: "1.4.2", who: "Maya Chen · fictional" },
    { id: "RS-0208", place: "Airport queue", status: "online", battery: 64, charging: false, syncLabel: "2 hr ago", version: "1.4.2", who: "Andre Ellis · fictional" },
    { id: "RS-0091", place: "Gulch spare", status: "offline", battery: 12, charging: false, syncLabel: "Yesterday 6:40p", version: "1.3.9", who: "Unassigned" },
    { id: "RS-0315", place: "East Nashville", status: "online", battery: 91, charging: true, syncLabel: "18 min ago", version: "1.4.2", who: "Priya Shah · fictional" },
  ];

  var DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

  var state = load();
  var adIndex = 0;
  var adStarted = performance.now();
  var tripStart = performance.now();
  var shownId = null;
  var selectedCampaign = ADS[0].id;
  var selectedTablet = "RS-0142";
  var transferTimer = null;

  function load() {
    try {
      var raw = JSON.parse(localStorage.getItem(STORAGE) || "{}");
      return {
        synced: !!raw.synced,
        connected: !!raw.connected,
        charging: !!raw.charging,
        lastSync: raw.lastSync || null,
      };
    } catch (e) {
      return { synced: false, connected: false, charging: false, lastSync: null };
    }
  }

  function save() {
    localStorage.setItem(STORAGE, JSON.stringify(state));
  }

  function currentAds() {
    return ADS.filter(function (ad) { return !ad.syncedOnly || state.synced; });
  }

  function num(n) { return n.toLocaleString("en-US"); }
  function money(n) { return n.toLocaleString("en-US", { style: "currency", currency: "USD" }); }
  function rate(scans, impressions) { return (100 * scans / impressions).toFixed(1) + "%"; }
  function kb(bytes) {
    if (bytes >= 1000000) return (bytes / 1000000).toFixed(1) + " MB";
    return Math.round(bytes / 1000) + " KB";
  }

  function toast(message) {
    var el = document.getElementById("toast");
    el.hidden = false;
    el.textContent = message;
    clearTimeout(toast._t);
    toast._t = setTimeout(function () { el.hidden = true; }, 3200);
  }

  function screenFromHash() {
    var name = (location.hash || "#passenger").slice(1);
    if (["passenger", "sync", "advertiser", "fleet", "wallet"].indexOf(name) === -1) name = "passenger";
    return name;
  }

  function showScreen() {
    var name = screenFromHash();
    document.querySelectorAll(".screen").forEach(function (section) {
      section.hidden = section.getAttribute("data-screen") !== name;
    });
    document.querySelectorAll(".nav a").forEach(function (link) {
      var on = link.getAttribute("data-screen") === name;
      if (on) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
    });
    if (name === "passenger") renderAd(true);
    if (name === "sync") renderSync();
    if (name === "advertiser") renderAdvertiser();
    if (name === "fleet") renderFleet();
    if (name === "wallet") renderWallet();
  }

  function renderAd(force) {
    var ads = currentAds();
    if (adIndex >= ads.length) adIndex = 0;
    var ad = ads[adIndex];
    if (!force && shownId === ad.id) return;
    shownId = ad.id;
    var poster = document.getElementById("poster");
    poster.innerHTML =
      (ad.syncedOnly ? '<div class="ribbon">New · driver sync</div>' : "") +
      '<div class="poster-face theme-' + ad.theme + " tone-" + ad.tone + '">' +
      '<div class="motif">' + (ad.motif || "") + "</div>" +
      '<p class="eyebrow">' + ad.category + " · " + ad.neighborhood + "</p>" +
      "<h2>" + ad.name + "</h2>" +
      '<p class="tag">' + ad.tagline + "</p>" +
      '<p class="fictional">Fictional sample · not a signed advertiser</p>' +
      "</div>";
    document.getElementById("qr-name").textContent = ad.name;
    document.getElementById("qr-tag").textContent = ad.tagline;
    document.getElementById("qr-url").textContent = ad.url;
    var canvas = document.getElementById("qr-canvas");
    canvas.setAttribute("aria-label", "QR code for the sample page of " + ad.name);
    if (window.RidesQR) window.RidesQR.draw(canvas, ad.url, 4);
  }

  function tripProgress(now) {
    var elapsed = ((now - tripStart) / 1000) * PLAYBACK;
    return Math.min(1, elapsed / TRIP_SECONDS);
  }

  function placeCar(progress) {
    var path = document.getElementById("route-base");
    var done = document.getElementById("route-done");
    var svg = document.getElementById("route-svg");
    var car = document.getElementById("trip-car");
    if (!path.getTotalLength) return;
    var len = path.getTotalLength();
    if (!len) return;
    done.style.strokeDasharray = len * progress + " " + len;
    var point = path.getPointAtLength(len * progress);
    var ahead = path.getPointAtLength(Math.min(len, len * progress + 12));
    var box = svg.viewBox.baseVal;
    var angle = Math.atan2(ahead.y - point.y, ahead.x - point.x) * 180 / Math.PI;
    car.style.left = (point.x / box.width) * 100 + "%";
    car.style.top = (point.y / box.height) * 100 + "%";
    car.style.transform = "translate(-20%, -70%) rotate(" + angle + "deg)";
  }

  function renderTrip(now) {
    var progress = tripProgress(now);
    var milesLeft = 8.4 * (1 - progress);
    var secondsLeft = TRIP_SECONDS * (1 - progress);
    var mins = secondsLeft / 60;
    document.getElementById("trip-clock").textContent = mins.toFixed(1) + " min";
    var whole = Math.ceil(secondsLeft);
    var mm = Math.floor(whole / 60);
    var ss = whole % 60;
    document.getElementById("trip-clock-small").textContent =
      mm + ":" + String(ss).padStart(2, "0");
    document.getElementById("trip-miles").textContent = milesLeft.toFixed(1) + " mi left";
    document.getElementById("route-note").textContent = progress >= 1
      ? "Sample trip complete · simulated route, not Google Maps"
      : "Broadway → Korean Veterans Blvd → I-40 E → Terminal Dr · simulated, not Google Maps";
    document.getElementById("replay-trip").hidden = progress < 1;
    placeCar(progress);
  }

  function tick(now) {
    var ads = currentAds();
    if (now - adStarted >= AD_MS) {
      adIndex = (adIndex + 1) % ads.length;
      adStarted = now;
      if (screenFromHash() === "passenger") renderAd(true);
      else shownId = null;
    }
    var remain = Math.ceil((AD_MS - (now - adStarted)) / 1000);
    var count = document.getElementById("ad-count");
    if (count) count.textContent = String(Math.max(1, remain));
    renderTrip(now);
    requestAnimationFrame(tick);
  }

  function incoming() {
    return ADS.filter(function (ad) { return ad.syncedOnly; });
  }

  function renderSync() {
    var box = document.getElementById("sync-status");
    var list = document.getElementById("file-list");
    var connect = document.getElementById("connect-btn");
    var transfer = document.getElementById("transfer-btn");
    var log = document.getElementById("sync-log");
    if (state.synced) {
      box.innerHTML = "<strong>RS-0142 already has the new ads</strong><span>Simulated transfer finished in this browser. Open Passenger to see them in the slideshow.</span>";
      connect.disabled = true;
      connect.textContent = "Connected";
      transfer.disabled = true;
      transfer.textContent = "Already transferred";
      log.textContent = "3 ad files are in the passenger slideshow.";
    } else if (state.connected) {
      box.innerHTML = "<strong>Connected to RS-0142</strong><span>Simulated local link. Bluetooth hardware is not used.</span>";
      connect.disabled = true;
      connect.textContent = "Connected";
      transfer.disabled = false;
      log.textContent = "Ready to copy 3 ad files onto the tablet.";
    } else {
      box.innerHTML = "<strong>Tablet RS-0142 nearby</strong><span>Headrest display · simulated discovery · no radio scan.</span>";
      connect.disabled = false;
      connect.textContent = "Connect";
      transfer.disabled = true;
      log.textContent = "Connect, then transfer. Nothing leaves this page.";
    }
    list.innerHTML = incoming().map(function (ad) {
      var pct = state.synced ? 100 : 0;
      return "<li data-file=\"" + ad.file + "\"><span>" + ad.file + "</span><span>" + kb(ad.bytes) +
        "</span><div class=\"bar\"><i style=\"width:" + pct + "%\"></i></div></li>";
    }).join("");
  }

  function setFileProgress(name, pct) {
    var row = document.querySelector('[data-file="' + name + '"] i');
    if (row) row.style.width = pct + "%";
  }

  function startTransfer() {
    if (state.synced || transferTimer) return;
    var files = incoming();
    var transfer = document.getElementById("transfer-btn");
    transfer.disabled = true;
    transfer.textContent = "Transferring…";
    document.getElementById("sync-log").textContent = "Copying ad files over the simulated local link…";
    var started = performance.now();
    var duration = 3200;
    transferTimer = requestAnimationFrame(function frame(now) {
      var t = Math.min(1, (now - started) / duration);
      files.forEach(function (ad, index) {
        var local = Math.min(1, Math.max(0, t * files.length - index));
        setFileProgress(ad.file, Math.round(local * 100));
      });
      var current = files[Math.min(files.length - 1, Math.floor(t * files.length))];
      if (t < 1 && current) {
        document.getElementById("sync-log").textContent = "Sending " + current.file + " · " + Math.round(t * 100) + "%";
      }
      if (t < 1) {
        transferTimer = requestAnimationFrame(frame);
        return;
      }
      transferTimer = null;
      state.synced = true;
      state.lastSync = Date.now();
      save();
      var fleet = FLEET[0];
      fleet.syncLabel = "Just now";
      document.getElementById("sync-log").textContent = "3 ads added to the slideshow.";
      transfer.textContent = "Transferred";
      toast("3 ads added to the passenger slideshow. They are demo creatives, not live buys.");
      var ads = currentAds();
      adIndex = ads.findIndex(function (ad) { return ad.syncedOnly; });
      if (adIndex < 0) adIndex = 0;
      adStarted = performance.now();
      shownId = null;
      renderSync();
    });
  }

  function renderAdvertiser() {
    var impressions = 0;
    var scans = 0;
    ADS.forEach(function (ad) {
      impressions += ad.impressions;
      scans += ad.scans;
    });
    var onDevice = currentAds().length;
    document.getElementById("kpis").innerHTML = [
      ["Impressions", num(impressions), true],
      ["QR scans", num(scans), false],
      ["Scan rate", rate(scans, impressions), false],
      ["On RS-0142", onDevice + " of " + ADS.length, false],
    ].map(function (item) {
      return '<article class="kpi' + (item[2] ? " accent" : "") + '"><span class="kicker">' +
        item[0] + "</span><strong>" + item[1] + "</strong></article>";
    }).join("");
    document.getElementById("device-pill").textContent = state.synced ? "All 8 on RS-0142" : "5 on RS-0142";
    document.getElementById("campaign-rows").innerHTML = ADS.map(function (ad) {
      var onTablet = !ad.syncedOnly || state.synced;
      return '<tr data-id="' + ad.id + '"' + (ad.id === selectedCampaign ? ' class="selected"' : "") + ">" +
        "<td><strong>" + ad.name + "</strong><br><span class=\"fine\">" + ad.neighborhood + " · fictional</span></td>" +
        "<td>" + num(ad.impressions) + "</td>" +
        "<td>" + num(ad.scans) + "</td>" +
        "<td>" + rate(ad.scans, ad.impressions) + "</td>" +
        "<td class=\"" + (onTablet ? "live" : "wait") + "\">" + (onTablet ? "On this tablet" : "Not on RS-0142 yet") + "</td>" +
        "</tr>";
    }).join("");
    renderChart();
  }

  function renderChart() {
    var ad = ADS.filter(function (item) { return item.id === selectedCampaign; })[0] || ADS[0];
    var max = Math.max.apply(null, ad.week);
    var total = ad.week.reduce(function (a, b) { return a + b; }, 0);
    document.getElementById("chart-title").textContent = ad.name + " · " + num(total) + " sample plays";
    document.getElementById("chart").innerHTML = ad.week.map(function (value, i) {
      var h = Math.round((value / max) * 100);
      return "<div><b style=\"height:" + h + "%\"></b><span>" + DAYS[i] + "</span></div>";
    }).join("");
  }

  function fleetSyncLabel(device) {
    if (device.id === "RS-0142" && state.lastSync) {
      var mins = Math.round((Date.now() - state.lastSync) / 60000);
      if (mins <= 0) return "Just now";
      if (mins === 1) return "1 min ago";
      return mins + " min ago";
    }
    return device.syncLabel;
  }

  function renderFleet() {
    document.getElementById("fleet-rows").innerHTML = FLEET.map(function (device) {
      var battery = device.id === "RS-0142" && state.charging ? device.battery : device.battery;
      var charging = device.charging || (device.id === "RS-0142" && state.charging);
      var cls = battery < 20 ? "low" : charging ? "charging" : "";
      var dot = device.status === "offline" ? "off" : "ok";
      return '<tr data-id="' + device.id + '"' + (device.id === selectedTablet ? ' class="selected"' : "") + ">" +
        "<td><strong>" + device.id + "</strong><br><span class=\"fine\">" + device.place + "</span></td>" +
        "<td><span class=\"status\"><i class=\"dot " + (dot === "off" ? "off" : "") + "\"></i>" + device.status + "</span></td>" +
        "<td><span class=\"battery " + cls + "\"><i style=\"width:" + battery + "%\"></i></span>" + battery + "%" +
        (charging ? " · charging" : "") + "</td>" +
        "<td>" + fleetSyncLabel(device) + "</td>" +
        "<td>" + device.version + "</td>" +
        "<td>" + device.who + "</td></tr>";
    }).join("");
    document.getElementById("control-target").textContent = selectedTablet;
    var note = document.getElementById("control-note");
    note.textContent = selectedTablet === "RS-0142"
      ? "Restart slideshow updates the passenger display in this tab. Ping and charging stay on this page. No tablet is contacted."
      : "These controls are sample actions for " + selectedTablet + ". They do not leave the browser.";
  }

  function renderWallet() {
    var lines = [
      ["Ad-play earnings", "$186.40", "6,420 sample plays on RS-0142 × $0.029"],
      ["Uptime bonus", "$45.00", "94% of October shifts · Steady tier"],
      ["Referral bonus", "$25.00", "Andre Ellis activated a tablet · fictional"],
    ];
    document.getElementById("wallet-total").textContent = "$256.40";
    document.getElementById("bonus-list").innerHTML = lines.map(function (line) {
      return "<li><span><strong>" + line[0] + "</strong><br><span class=\"fine\">" + line[2] +
        "</span></span><strong>" + line[1] + "</strong></li>";
    }).join("");
    document.getElementById("activity").innerHTML = [
      "Oct 2 · Uptime bonus posted · $45.00 sample",
      "Oct 6 · Referral bonus · Andre Ellis · $25.00 sample",
      "Running · Qualified ad plays · $186.40 sample",
      "Total on this demo ledger · $256.40",
    ].map(function (item) { return "<li>" + item + "</li>"; }).join("");
  }

  document.getElementById("connect-btn").addEventListener("click", function () {
    state.connected = true;
    save();
    renderSync();
    toast("Simulated connection to RS-0142. No Bluetooth radio was used.");
  });

  document.getElementById("transfer-btn").addEventListener("click", startTransfer);

  document.getElementById("reset-demo").addEventListener("click", function () {
    localStorage.removeItem(STORAGE);
    state.synced = false;
    state.connected = false;
    state.charging = false;
    state.lastSync = null;
    FLEET[0].syncLabel = "26 min ago";
    FLEET[0].charging = false;
    adIndex = 0;
    adStarted = performance.now();
    shownId = null;
    if (transferTimer) cancelAnimationFrame(transferTimer);
    transferTimer = null;
    showScreen();
    toast("Demo reset. The extra ads are off the slideshow again.");
  });

  document.getElementById("replay-trip").addEventListener("click", function () {
    tripStart = performance.now();
  });

  document.getElementById("campaign-rows").addEventListener("click", function (event) {
    var row = event.target.closest("tr");
    if (!row) return;
    selectedCampaign = row.getAttribute("data-id");
    renderAdvertiser();
  });

  document.getElementById("fleet-rows").addEventListener("click", function (event) {
    var row = event.target.closest("tr");
    if (!row) return;
    selectedTablet = row.getAttribute("data-id");
    renderFleet();
  });

  document.getElementById("btn-restart").addEventListener("click", function () {
    if (selectedTablet !== "RS-0142") {
      toast("Slideshow restart in this demo only affects RS-0142, the on-page passenger display.");
      return;
    }
    adIndex = 0;
    adStarted = performance.now();
    shownId = null;
    toast("Passenger slideshow restarted in this tab. No tablet was contacted.");
  });

  document.getElementById("btn-ping").addEventListener("click", function () {
    var device = FLEET.filter(function (item) { return item.id === selectedTablet; })[0];
    if (device && device.status === "offline") {
      toast(selectedTablet + " is marked offline in this demo. No device was contacted.");
      return;
    }
    toast("Simulated ping to " + selectedTablet + ". Nothing was sent off this page.");
  });

  document.getElementById("btn-charge").addEventListener("click", function () {
    if (selectedTablet !== "RS-0142") {
      var device = FLEET.filter(function (item) { return item.id === selectedTablet; })[0];
      if (device) device.charging = !device.charging;
      renderFleet();
      toast("Charging flag for " + selectedTablet + " flipped in the demo table only.");
      return;
    }
    state.charging = !state.charging;
    save();
    renderFleet();
    toast(state.charging ? "RS-0142 marked charging in this demo." : "RS-0142 marked unplugged in this demo.");
  });

  window.addEventListener("hashchange", showScreen);
  showScreen();
  requestAnimationFrame(tick);
})();
