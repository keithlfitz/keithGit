(function () {
  var SLIDE_MS = 15000;
  var SIM_SECONDS = 8;
  var RING = 2 * Math.PI * 15;

  var ROUTES = [
    { id: "pike", name: "Pike Place Market", totalMiles: 6.8, remainingMiles: 4.2, remainingSeconds: 12 * 60 },
    { id: "ballard", name: "Ballard Avenue", totalMiles: 8.4, remainingMiles: 6.1, remainingSeconds: 18 * 60 },
    { id: "capitol", name: "Capitol Hill", totalMiles: 3.6, remainingMiles: 2.4, remainingSeconds: 9 * 60 },
    { id: "seatac", name: "Sea-Tac Airport", totalMiles: 16.2, remainingMiles: 14.8, remainingSeconds: 28 * 60 },
  ];

  var FLEET = [
    { id: "RS-2041", name: "Headrest tablet", place: "This browser", live: true },
    { id: "RS-1902", name: "Headrest tablet", place: "Airport queue", online: true, ad: "Pike Street Books", sync: "2 min ago" },
    { id: "RS-1877", name: "Headrest tablet", place: "Downtown loop", online: false, ad: "Lumen Hotel", sync: "2 hours ago" },
  ];

  var RIDES = [
    { id: "r-8821", when: "2:05 PM", dest: "Pike Place Market", minutes: 14, ads: 4, scans: 1, tablet: "RS-1902" },
    { id: "r-8814", when: "1:41 PM", dest: "Capitol Hill", minutes: 9, ads: 2, scans: 0, tablet: "RS-2041" },
    { id: "r-8802", when: "1:12 PM", dest: "Ballard Avenue", minutes: 18, ads: 5, scans: 2, tablet: "RS-2041" },
    { id: "r-8790", when: "12:20 PM", dest: "Sea-Tac Airport", minutes: 31, ads: 7, scans: 1, tablet: "RS-1877" },
    { id: "r-8774", when: "11:06 AM", dest: "Pike Place Market", minutes: 16, ads: 4, scans: 1, tablet: "RS-1902" },
  ];

  var SEED_EVENTS = [
    { kind: "Scan", id: "harbor-and-rye", name: "Harbor & Rye", where: "RS-1902", when: "2:14 PM" },
    { kind: "Scan", id: "pike-street-books", name: "Pike Street Books", where: "RS-2041", when: "1:52 PM" },
    { kind: "Impression", id: "northline-eats", name: "Northline Eats", where: "RS-2041", when: "1:48 PM" },
    { kind: "Scan", id: "cedar-dental", name: "Cedar Dental", where: "RS-1877", when: "12:16 PM" },
  ];

  var TITLES = {
    tablet: "Rides & Shares — Passenger tablet",
    driver: "Rides & Shares — Driver phone",
    admin: "Rides & Shares — Admin",
  };

  var PHONE_COPY = {
    idle: ["Driver phone", "Pair the headrest tablet over Bluetooth, then send the destination and the distance left."],
    scanning: ["Searching", "Looking for a Rides & Shares tablet nearby."],
    found: ["Tablet found", "This is the headrest in the car. Tap it to pair."],
    connecting: ["Pairing", "Connecting to RS-2041."],
    connected: ["Bluetooth paired", "Send the destination. The tablet shows the miles, the countdown, and moves the car."],
  };

  var state = {
    view: "tablet",
    adminPanel: "overview",
    adminId: null,
    slide: 0,
    slideStarted: 0,
    holdSlide: false,
    heldElapsed: 0,
    phase: "idle",
    draft: Object.assign({}, ROUTES[0]),
    link: null,
    paused: {},
    sessionImpressions: {},
    sessionScans: {},
    events: [],
    sendLog: [],
    sentOnce: false,
  };

  var phaseTimer = 0;
  var activitySig = "";
  var rideSig = "";
  var lastTripBucket = -1;

  function $(id) { return document.getElementById(id); }
  function adAt(i) { return window.CATALOG[i]; }
  function round1(n) { return Math.round(n * 10) / 10; }
  function clamp(n, a, b) { return Math.max(a, Math.min(b, n)); }

  function formatClock(seconds) {
    var whole = Math.max(0, Math.ceil(seconds));
    var m = Math.floor(whole / 60);
    var s = whole % 60;
    return m + ":" + String(s).padStart(2, "0");
  }

  function clockLabel() {
    return new Date().toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  }

  function tripNow() {
    if (!state.link) return null;
    var elapsedReal = (Date.now() - state.link.startedAt) / 1000;
    var remainingSeconds = Math.max(0, state.link.remainingSeconds - elapsedReal * SIM_SECONDS);
    var ratio = state.link.remainingSeconds === 0 ? 0 : remainingSeconds / state.link.remainingSeconds;
    var remainingMiles = state.link.remainingMiles * ratio;
    var progress = state.link.totalMiles === 0 ? 1 : 1 - remainingMiles / state.link.totalMiles;
    return {
      remainingSeconds: remainingSeconds,
      remainingMiles: remainingMiles,
      progress: clamp(progress, 0, 1),
      destination: state.link.destination,
      totalMiles: state.link.totalMiles,
      arrived: remainingSeconds <= 0.4,
    };
  }

  function countsFor(id) {
    var sampleI = window.SAMPLE.impressions[id] || 0;
    var sampleS = window.SAMPLE.scans[id] || 0;
    var liveI = state.sessionImpressions[id] || 0;
    var liveS = state.sessionScans[id] || 0;
    return { impressions: sampleI + liveI, scans: sampleS + liveS, session: liveI, sessionScans: liveS };
  }

  function totals() {
    var impressions = 0;
    var scans = 0;
    var sessionI = 0;
    var sessionS = 0;
    window.CATALOG.forEach(function (ad) {
      var c = countsFor(ad.id);
      impressions += c.impressions;
      scans += c.scans;
      sessionI += c.session;
      sessionS += c.sessionScans;
    });
    return { impressions: impressions, scans: scans, sessionI: sessionI, sessionS: sessionS, rate: impressions ? scans / impressions : 0 };
  }

  function nextActive(from, dir) {
    var n = window.CATALOG.length;
    for (var step = 1; step <= n; step++) {
      var i = (from + dir * step + n * 4) % n;
      if (!state.paused[adAt(i).id]) return i;
    }
    return -1;
  }

  function allPaused() {
    return window.CATALOG.every(function (ad) { return state.paused[ad.id]; });
  }

  function logEvent(kind, ad) {
    state.events.unshift({ kind: kind, id: ad.id, name: ad.businessName, where: "RS-2041", when: "Just now" });
    if (state.events.length > 8) state.events.pop();
  }

  function logImpression(ad) {
    state.sessionImpressions[ad.id] = (state.sessionImpressions[ad.id] || 0) + 1;
    logEvent("Impression", ad);
  }

  function goTo(i, shouldLog) {
    if (i < 0) {
      $("poster-empty").hidden = false;
      state.slideStarted = performance.now();
      return;
    }
    state.slide = i;
    state.slideStarted = performance.now();
    $("poster-empty").hidden = true;
    if (shouldLog) logImpression(adAt(i));
    paintSlide();
    paintAdminNumbers();
    paintActivity();
  }

  function advance(dir) {
    if (allPaused()) {
      $("poster-empty").hidden = false;
      state.slideStarted = performance.now();
      return;
    }
    goTo(nextActive(state.slide, dir), true);
  }

  function paintSlide() {
    var ad = adAt(state.slide);
    document.querySelectorAll(".slide").forEach(function (el, i) {
      el.classList.toggle("is-on", i === state.slide);
    });
    document.querySelectorAll(".qr-stack img").forEach(function (el, i) {
      el.classList.toggle("is-on", i === state.slide);
    });
    document.querySelectorAll(".dot").forEach(function (el, i) {
      el.classList.toggle("is-on", i === state.slide);
      el.classList.toggle("is-paused", !!state.paused[adAt(i).id]);
    });
    var panel = $("panel");
    panel.style.background = ad.panel;
    panel.style.color = ad.ink;
    panel.style.borderTopColor = ad.accent;
    $("panel-eye").textContent = ad.eyebrow;
    $("panel-eye").style.color = ad.muted;
    $("panel-name").textContent = ad.businessName;
    $("panel-tag").textContent = ad.tagline;
    $("panel-tag").style.color = ad.muted;
    $("scan-label").textContent = ad.scan;
    $("info-url").textContent = ad.infoUrl.replace("https://", "");
    $("info-url").style.color = ad.muted;
    $("qr-button").setAttribute("aria-label", "Open the phone page for " + ad.businessName);
    $("slide-count").textContent = (state.slide + 1) + " / " + window.CATALOG.length;
    if (state.adminPanel === "campaigns" && state.adminId) paintCampaignDetail();
  }

  function paintRing(elapsed) {
    var remain = Math.max(0, SLIDE_MS - elapsed);
    var frac = remain / SLIDE_MS;
    $("ring-fg").setAttribute("stroke-dasharray", RING.toFixed(3));
    $("ring-fg").setAttribute("stroke-dashoffset", (RING * (1 - frac)).toFixed(3));
    $("ring-num").textContent = String(Math.max(1, Math.ceil(remain / 1000)));
  }

  function paintTrip() {
    var tablet = $("view-tablet");
    var trip = tripNow();
    var track = $("track");
    if (!trip) {
      tablet.classList.add("is-waiting");
      tablet.classList.remove("is-linked", "is-arrived");
      $("mile-label").textContent = "Distance";
      $("mile-value").textContent = "—";
      $("time-label").textContent = "Arrive in";
      $("time-value").textContent = "—:—";
      $("dest-kicker").textContent = "Driver phone";
      $("dest-value").textContent = "Waiting for the driver phone";
      $("dest-sub").textContent = "This tablet · 192.168.4.21";
      $("track-fill").style.width = "0%";
      $("car").style.left = "0px";
      track.setAttribute("aria-valuenow", "0");
      return;
    }
    tablet.classList.add("is-linked");
    tablet.classList.remove("is-waiting");
    tablet.classList.toggle("is-arrived", trip.arrived);
    var progress = trip.arrived ? 1 : trip.progress;
    $("mile-label").textContent = trip.arrived ? "Distance" : "Remaining";
    $("mile-value").textContent = trip.arrived ? "0.0 mi" : trip.remainingMiles.toFixed(1) + " mi";
    $("time-label").textContent = trip.arrived ? "Status" : "Arrive in";
    $("time-value").textContent = trip.arrived ? "Here" : formatClock(trip.remainingSeconds);
    $("dest-kicker").textContent = trip.arrived ? "Arrived" : "Heading to";
    $("dest-value").textContent = trip.destination;
    $("dest-sub").textContent = trip.arrived ? "Bluetooth · driver phone" : "Bluetooth · driver phone";
    $("track-fill").style.width = (progress * 100) + "%";
    $("car").style.left = "calc(" + (progress * 100) + "% - " + (progress * 52) + "px)";
    track.setAttribute("aria-valuenow", String(Math.round(progress * 100)));
  }

  function flashRide() {
    var ride = $("ride");
    ride.classList.remove("is-fresh");
    void ride.offsetWidth;
    ride.classList.add("is-fresh");
  }

  function noteSend() {
    var trip = tripNow();
    if (!trip) return;
    var line = clockLabel() + "  ·  " + trip.remainingMiles.toFixed(1) + " mi · " + formatClock(trip.remainingSeconds) + " · " + trip.destination;
    state.sendLog.unshift(line);
    state.sendLog = state.sendLog.slice(0, 5);
  }

  function paintPhone() {
    ["idle", "scanning", "found", "connecting", "connected"].forEach(function (name) {
      $("phase-" + name).hidden = state.phase !== name;
    });
    var copy = PHONE_COPY[state.phase];
    $("phone-title").textContent = copy[0];
    $("phone-lead").textContent = copy[1];
    if (state.phase === "connected") paintPhoneLive();
  }

  function displayedTrip() {
    if (state.link) return tripNow();
    return {
      remainingMiles: state.draft.remainingMiles,
      remainingSeconds: state.draft.remainingSeconds,
      totalMiles: state.draft.totalMiles,
      destination: state.draft.name,
      arrived: false,
    };
  }

  function paintPhoneLive() {
    if (state.phase !== "connected") return;
    var trip = displayedTrip();
    var name = state.link ? state.link.destination : state.draft.name;
    $("phone-miles").textContent = (trip.arrived ? 0 : trip.remainingMiles).toFixed(1) + " mi";
    $("phone-total").textContent = "of " + trip.totalMiles.toFixed(1) + " mi";
    $("phone-time").textContent = trip.arrived ? "0:00" : formatClock(trip.remainingSeconds);
    $("bt-send").textContent = state.link ? "Send again" : "Send to tablet";
    $("see-tablet").hidden = !state.sentOnce;
    document.querySelectorAll(".chip").forEach(function (chip) {
      chip.classList.toggle("is-on", chip.dataset.name === name);
    });
    var log = $("send-log");
    log.innerHTML = "";
    state.sendLog.forEach(function (line) {
      var li = document.createElement("li");
      li.textContent = line;
      log.appendChild(li);
    });
    var status = $("share-status");
    status.classList.toggle("is-live", !!state.link);
    if (!state.link) status.textContent = "Paired. Nothing sent yet.";
    else if (trip.arrived) status.textContent = "Passenger arrived.";
    else if (state.sentOnce) status.textContent = "Tablet updated · " + trip.destination;
  }

  function setPhase(phase) {
    window.clearTimeout(phaseTimer);
    state.phase = phase;
    if (phase === "scanning") {
      phaseTimer = window.setTimeout(function () {
        if (state.phase === "scanning") setPhase("found");
      }, 1400);
    }
    if (phase === "connecting") {
      phaseTimer = window.setTimeout(function () {
        if (state.phase === "connecting") setPhase("connected");
      }, 900);
    }
    paintPhone();
  }

  function selectRoute(route) {
    state.draft = {
      id: route.id,
      name: route.name,
      totalMiles: route.totalMiles,
      remainingMiles: route.remainingMiles,
      remainingSeconds: route.remainingSeconds,
    };
    if (state.link) {
      state.link = {
        destination: route.name,
        totalMiles: route.totalMiles,
        remainingMiles: route.remainingMiles,
        remainingSeconds: route.remainingSeconds,
        startedAt: Date.now(),
      };
      state.sentOnce = true;
      noteSend();
      flashRide();
    }
    paintPhoneLive();
    paintTrip();
  }

  function adjust(kind, delta) {
    if (!state.link) {
      if (kind === "mi") state.draft.remainingMiles = clamp(round1(state.draft.remainingMiles + delta), 0.1, 40);
      else state.draft.remainingSeconds = clamp(state.draft.remainingSeconds + delta, 30, 90 * 60);
      if (state.draft.remainingMiles > state.draft.totalMiles) state.draft.totalMiles = state.draft.remainingMiles;
      paintPhoneLive();
      return;
    }
    var now = tripNow();
    var miles = now.remainingMiles;
    var seconds = now.arrived ? 60 : now.remainingSeconds;
    if (kind === "mi") miles = clamp(round1(miles + delta), 0.1, 40);
    else seconds = clamp(seconds + delta, 30, 90 * 60);
    var total = state.link.totalMiles;
    if (miles > total) total = round1(miles);
    state.link.remainingMiles = miles;
    state.link.remainingSeconds = seconds;
    state.link.totalMiles = total;
    state.link.startedAt = Date.now();
    noteSend();
    flashRide();
    paintPhoneLive();
    paintTrip();
  }

  function sendToTablet() {
    if (state.phase !== "connected") return;
    if (!state.link) {
      state.link = {
        destination: state.draft.name,
        totalMiles: state.draft.totalMiles,
        remainingMiles: state.draft.remainingMiles,
        remainingSeconds: state.draft.remainingSeconds,
        startedAt: Date.now(),
      };
    } else {
      var now = tripNow();
      state.link.remainingMiles = now.arrived ? 0 : now.remainingMiles;
      state.link.remainingSeconds = now.arrived ? 0 : now.remainingSeconds;
      state.link.startedAt = Date.now();
    }
    state.sentOnce = true;
    noteSend();
    flashRide();
    paintPhoneLive();
    paintTrip();
    paintAdminNumbers();
  }

  function disconnect() {
    window.clearTimeout(phaseTimer);
    state.phase = "idle";
    state.link = null;
    state.sentOnce = false;
    state.sendLog = [];
    $("share-status").textContent = "";
    paintPhone();
    paintTrip();
    paintAdminNumbers();
  }

  function openSheet() {
    var ad = adAt(state.slide);
    state.heldElapsed = performance.now() - state.slideStarted;
    state.holdSlide = true;
    state.sessionScans[ad.id] = (state.sessionScans[ad.id] || 0) + 1;
    logEvent("Scan", ad);
    $("sheet-name").textContent = ad.businessName;
    $("sheet-url").textContent = ad.infoUrl;
    $("scan-sheet").hidden = false;
    paintAdminNumbers();
    paintActivity();
    if (state.adminId === ad.id) paintCampaignDetail();
  }

  function closeSheet() {
    if ($("scan-sheet").hidden) return;
    $("scan-sheet").hidden = true;
    state.slideStarted = performance.now() - state.heldElapsed;
    state.holdSlide = false;
  }

  function writeHash() {
    var hash = "#" + state.view;
    if (state.view === "admin") {
      hash += "/" + state.adminPanel;
      if (state.adminId) hash += "/" + state.adminId;
    }
    if (location.hash !== hash) history.replaceState(null, "", hash);
  }

  function applyView() {
    ["tablet", "driver", "admin"].forEach(function (name) {
      $("view-" + name).hidden = state.view !== name;
    });
    document.querySelectorAll(".dock button").forEach(function (btn) {
      btn.setAttribute("aria-current", btn.dataset.view === state.view ? "true" : "false");
    });
    document.title = TITLES[state.view] || TITLES.tablet;
    if (state.view === "admin") paintAdmin();
  }

  function openView(view) {
    state.view = view;
    if (view !== "admin") state.adminId = null;
    applyView();
    writeHash();
  }

  function openAdmin(panel, id) {
    state.view = "admin";
    state.adminPanel = panel;
    state.adminId = id || null;
    applyView();
    writeHash();
  }

  var PANEL_TITLES = {
    overview: ["Sample week", "Overview"],
    campaigns: ["Catalog", "Campaigns"],
    tablets: ["Fleet", "Tablets"],
    rides: ["Today", "Rides"],
  };

  function paintAdmin() {
    var titles = PANEL_TITLES[state.adminPanel];
    $("admin-kicker").textContent = titles[0];
    $("admin-title").textContent = titles[1];
    document.querySelectorAll(".side-nav button").forEach(function (btn) {
      btn.setAttribute("aria-current", btn.dataset.panel === state.adminPanel ? "true" : "false");
    });
    $("panel-overview").hidden = state.adminPanel !== "overview";
    $("panel-campaigns").hidden = state.adminPanel !== "campaigns";
    $("panel-tablets").hidden = state.adminPanel !== "tablets";
    $("panel-rides").hidden = state.adminPanel !== "rides";
    $("campaign-list").hidden = state.adminPanel === "campaigns" && !!state.adminId;
    $("campaign-detail").hidden = !(state.adminPanel === "campaigns" && state.adminId);
    $("tablet-list").hidden = state.adminPanel === "tablets" && !!state.adminId;
    $("tablet-detail").hidden = !(state.adminPanel === "tablets" && state.adminId);
    $("ride-card").hidden = state.adminPanel === "rides" && !!state.adminId;
    $("ride-detail").hidden = !(state.adminPanel === "rides" && state.adminId);
    paintAdminNumbers();
    paintActivity();
    paintCampaignBadges();
    if (state.adminPanel === "campaigns" && state.adminId) paintCampaignDetail();
    if (state.adminPanel === "tablets" && state.adminId) paintTabletDetail();
    if (state.adminPanel === "rides" && state.adminId) paintRideDetail();
    paintRideTable();
  }

  function paintAdminNumbers() {
    var t = totals();
    $("kpi-impressions").textContent = t.impressions.toLocaleString();
    $("kpi-scans").textContent = t.scans.toLocaleString();
    $("kpi-rate").textContent = (t.rate * 100).toFixed(1) + "%";
    var online = state.link ? "2 of 3" : "2 of 3";
    $("kpi-tablets").textContent = online;
    $("session-line").textContent = "This browser · " + t.sessionI + " impression" + (t.sessionI === 1 ? "" : "s") + " · " + t.sessionS + " scan" + (t.sessionS === 1 ? "" : "s");
    var max = 1;
    window.CATALOG.forEach(function (ad) { max = Math.max(max, countsFor(ad.id).impressions); });
    window.CATALOG.forEach(function (ad) {
      var c = countsFor(ad.id);
      var fill = document.querySelector('[data-bar="' + ad.id + '"]');
      var count = document.querySelector('[data-count="' + ad.id + '"]');
      var impr = $("impr-" + ad.id);
      if (fill) fill.style.width = (c.impressions / max * 100) + "%";
      if (count) count.textContent = String(c.impressions);
      if (impr) impr.textContent = c.impressions + " impressions · " + c.scans + " scans";
    });
    paintCampaignBadges();
  }

  function paintActivity() {
    var rows = state.events.concat(SEED_EVENTS);
    var sig = rows.map(function (e) { return e.kind + e.id + e.when; }).join("|");
    if (sig === activitySig) return;
    activitySig = sig;
    var box = $("activity");
    box.innerHTML = "";
    rows.forEach(function (ev) {
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "activity-row";
      var kind = document.createElement("span");
      kind.className = "kind" + (ev.kind === "Scan" ? " scan" : "");
      kind.textContent = ev.kind;
      var name = document.createElement("span");
      name.textContent = ev.name;
      var when = document.createElement("span");
      when.className = "when";
      when.textContent = ev.when;
      btn.appendChild(kind);
      btn.appendChild(name);
      btn.appendChild(when);
      btn.addEventListener("click", function () { openAdmin("campaigns", ev.id); });
      box.appendChild(btn);
    });
  }

  function paintCampaignBadges() {
    window.CATALOG.forEach(function (ad) {
      var badge = $("badge-" + ad.id);
      if (!badge) return;
      var paused = !!state.paused[ad.id];
      badge.textContent = paused ? "Paused" : "Running";
      badge.classList.toggle("off", paused);
    });
  }

  function paintCampaignDetail() {
    var ad = window.CATALOG.filter(function (item) { return item.id === state.adminId; })[0];
    if (!ad) return;
    var c = countsFor(ad.id);
    $("detail-art").src = ad.image;
    $("detail-qr").src = ad.qr;
    $("detail-qr").alt = "QR code for " + ad.businessName;
    $("detail-eye").textContent = ad.eyebrow;
    $("detail-name").textContent = ad.businessName;
    $("detail-tag").textContent = ad.tagline;
    $("detail-url").textContent = ad.infoUrl;
    $("detail-impr").textContent = c.impressions.toLocaleString();
    $("detail-scans").textContent = c.scans.toLocaleString();
    $("detail-session").textContent = String(c.session);
    var paused = !!state.paused[ad.id];
    $("campaign-toggle").textContent = paused ? "Run this ad" : "Pause this ad";
    $("campaign-toggle").setAttribute("aria-pressed", paused ? "true" : "false");
  }

  function paintTabletDetail() {
    var tablet = FLEET.filter(function (item) { return item.id === state.adminId; })[0];
    if (!tablet) return;
    $("tablet-name").textContent = tablet.id;
    $("tablet-place").textContent = tablet.name + " · " + tablet.place;
    var facts = [];
    if (tablet.live) {
      var trip = tripNow();
      var ad = adAt(state.slide);
      facts = [
        ["Power", "On · this browser"],
        ["Bluetooth", state.phase === "connected" ? "Paired with the driver phone" : "Waiting for the driver phone"],
        ["Route", trip ? trip.destination : "No route yet"],
        ["Remaining", trip ? (trip.arrived ? "Arrived" : trip.remainingMiles.toFixed(1) + " mi · " + formatClock(trip.remainingSeconds)) : "—"],
        ["Now showing", ad.businessName],
      ];
    } else {
      facts = [
        ["Power", tablet.online ? "On" : "Off"],
        ["Last sync", tablet.sync],
        ["Last ad", tablet.ad],
        ["Notes", "Sample tablet. Only RS-2041 follows this browser."],
      ];
    }
    var dl = $("tablet-facts");
    dl.innerHTML = "";
    facts.forEach(function (pair) {
      var dt = document.createElement("dt");
      dt.textContent = pair[0];
      var dd = document.createElement("dd");
      dd.textContent = pair[1];
      dl.appendChild(dt);
      dl.appendChild(dd);
    });
    $("tablet-open").hidden = !tablet.live;
  }

  function paintRideTable() {
    var trip = tripNow();
    var sig = (trip ? trip.destination : "none");
    var body = $("ride-body");
    if (sig !== rideSig) {
      rideSig = sig;
      body.innerHTML = "";
    } else {
      var existing = body.querySelector('[data-ride="live"]');
      if (existing && trip) {
        existing.children[3].textContent = trip.arrived ? "Arrived" : formatClock(trip.remainingSeconds);
      }
      return;
    }
    if (trip) {
      var live = document.createElement("tr");
      live.className = "ride-row is-live";
      live.dataset.ride = "live";
      live.innerHTML = "<td>Now</td><td></td><td>RS-2041</td><td></td><td>—</td><td>—</td>";
      live.children[1].textContent = trip.destination;
      live.children[3].textContent = trip.arrived ? "Arrived" : formatClock(trip.remainingSeconds);
      body.appendChild(live);
    }
    RIDES.forEach(function (ride) {
      var tr = document.createElement("tr");
      tr.className = "ride-row";
      tr.dataset.ride = ride.id;
      [ride.when, ride.dest, ride.tablet, String(ride.minutes), String(ride.ads), String(ride.scans)].forEach(function (text) {
        var td = document.createElement("td");
        td.textContent = text;
        tr.appendChild(td);
      });
      body.appendChild(tr);
    });
  }

  function paintRideDetail() {
    if (state.adminId === "live") {
      var trip = tripNow();
      $("ride-title").textContent = trip ? trip.destination : "No live ride";
      fillFacts($("ride-facts"), trip ? [
        ["Tablet", "RS-2041"],
        ["Status", trip.arrived ? "Arrived" : "In progress"],
        ["Remaining", trip.arrived ? "0.0 mi" : trip.remainingMiles.toFixed(1) + " mi"],
        ["Countdown", trip.arrived ? "0:00" : formatClock(trip.remainingSeconds)],
        ["Source", "Bluetooth simulation from the driver phone"],
      ] : [["Status", "The driver phone has not sent a route."]]);
      return;
    }
    var ride = RIDES.filter(function (item) { return item.id === state.adminId; })[0];
    if (!ride) return;
    $("ride-title").textContent = ride.dest;
    fillFacts($("ride-facts"), [
      ["When", ride.when],
      ["Tablet", ride.tablet],
      ["Minutes", String(ride.minutes)],
      ["Ads played", String(ride.ads)],
      ["QR scans", String(ride.scans)],
      ["Notes", "Sample ride from earlier today."],
    ]);
  }

  function fillFacts(dl, pairs) {
    dl.innerHTML = "";
    pairs.forEach(function (pair) {
      var dt = document.createElement("dt");
      dt.textContent = pair[0];
      var dd = document.createElement("dd");
      dd.textContent = pair[1];
      dl.appendChild(dt);
      dl.appendChild(dd);
    });
  }

  function buildSlides() {
    var slides = $("slides");
    var qrs = $("qr-stack");
    var dots = $("dots");
    window.CATALOG.forEach(function (ad, i) {
      var slide = document.createElement("div");
      slide.className = "slide" + (i === 0 ? " is-on" : "");
      slide.dataset.tone = ad.tone;
      var img = document.createElement("img");
      img.alt = "";
      img.src = ad.image;
      var copy = document.createElement("div");
      copy.className = "poster-copy";
      var eye = document.createElement("p");
      eye.className = "eyebrow";
      eye.textContent = ad.eyebrow;
      var headline = document.createElement("p");
      headline.className = "headline";
      ad.lines.forEach(function (line, idx) {
        if (idx) headline.appendChild(document.createElement("br"));
        headline.appendChild(document.createTextNode(line));
      });
      copy.appendChild(eye);
      copy.appendChild(headline);
      slide.appendChild(img);
      slide.appendChild(copy);
      slides.appendChild(slide);

      var qr = document.createElement("img");
      qr.alt = "";
      qr.src = ad.qr;
      if (i === 0) qr.className = "is-on";
      qrs.appendChild(qr);

      var dot = document.createElement("button");
      dot.type = "button";
      dot.className = "dot" + (i === 0 ? " is-on" : "");
      dot.setAttribute("aria-label", ad.businessName);
      dot.addEventListener("click", function () { goTo(i, i !== state.slide); });
      dots.appendChild(dot);
    });
  }

  function buildRoutes() {
    var box = $("route-chips");
    ROUTES.forEach(function (route) {
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "chip" + (route.id === state.draft.id ? " is-on" : "");
      btn.dataset.name = route.name;
      btn.textContent = route.name;
      btn.addEventListener("click", function () { selectRoute(route); });
      box.appendChild(btn);
    });
  }

  function buildChart() {
    var chart = $("chart");
    window.CATALOG.forEach(function (ad) {
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "bar-row";
      var name = document.createElement("span");
      name.textContent = ad.businessName;
      var track = document.createElement("span");
      track.className = "bar-track";
      var fill = document.createElement("span");
      fill.className = "bar-fill";
      fill.dataset.bar = ad.id;
      fill.style.background = ad.accent;
      track.appendChild(fill);
      var count = document.createElement("span");
      count.className = "bar-count";
      count.dataset.count = ad.id;
      btn.appendChild(name);
      btn.appendChild(track);
      btn.appendChild(count);
      btn.addEventListener("click", function () { openAdmin("campaigns", ad.id); });
      chart.appendChild(btn);
    });
  }

  function buildCampaigns() {
    var list = $("campaign-list");
    window.CATALOG.forEach(function (ad) {
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "ad-card";
      var img = document.createElement("img");
      img.alt = "";
      img.src = ad.image;
      var body = document.createElement("div");
      var name = document.createElement("strong");
      name.textContent = ad.businessName;
      var meta = document.createElement("em");
      meta.id = "impr-" + ad.id;
      var badge = document.createElement("span");
      badge.className = "badge";
      badge.id = "badge-" + ad.id;
      badge.textContent = "Running";
      body.appendChild(name);
      body.appendChild(meta);
      body.appendChild(badge);
      btn.appendChild(img);
      btn.appendChild(body);
      btn.addEventListener("click", function () { openAdmin("campaigns", ad.id); });
      list.appendChild(btn);
    });
  }

  function buildTablets() {
    var list = $("tablet-list");
    FLEET.forEach(function (tablet) {
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "row";
      var mark = document.createElement("span");
      mark.className = "mark";
      mark.textContent = "RS";
      var text = document.createElement("span");
      var strong = document.createElement("strong");
      strong.textContent = tablet.id + " · " + tablet.place;
      var sub = document.createElement("span");
      sub.id = "fleet-sub-" + tablet.id;
      text.appendChild(strong);
      text.appendChild(sub);
      var pill = document.createElement("span");
      pill.className = "pill";
      pill.id = "fleet-pill-" + tablet.id;
      btn.appendChild(mark);
      btn.appendChild(text);
      btn.appendChild(pill);
      btn.addEventListener("click", function () { openAdmin("tablets", tablet.id); });
      list.appendChild(btn);
    });
  }

  function paintFleetRows() {
    FLEET.forEach(function (tablet) {
      var sub = $("fleet-sub-" + tablet.id);
      var pill = $("fleet-pill-" + tablet.id);
      if (!sub || !pill) return;
      if (tablet.live) {
        sub.textContent = state.link ? "On a ride · " + state.link.destination : "Waiting for the driver phone";
        pill.textContent = state.phase === "connected" ? "Paired" : "Online";
        pill.className = state.phase === "connected" ? "pill" : "pill wait";
      } else if (tablet.online) {
        sub.textContent = tablet.ad;
        pill.textContent = "Online";
        pill.className = "pill";
      } else {
        sub.textContent = "Last seen " + tablet.sync;
        pill.textContent = "Offline";
        pill.className = "pill off";
      }
    });
  }

  function readHash() {
    var parts = (location.hash || "#tablet").replace("#", "").split("/");
    var view = parts[0] || "tablet";
    if (view !== "tablet" && view !== "driver" && view !== "admin") view = "tablet";
    state.view = view;
    if (view === "admin") {
      var panel = parts[1] || "overview";
      if (panel !== "overview" && panel !== "campaigns" && panel !== "tablets" && panel !== "rides") panel = "overview";
      state.adminPanel = panel;
      state.adminId = parts[2] || null;
    }
    applyView();
  }

  function frame(now) {
    if (!state.holdSlide) {
      var elapsed = now - state.slideStarted;
      if (elapsed >= SLIDE_MS) advance(1);
      else paintRing(elapsed);
    } else {
      paintRing(state.heldElapsed);
    }
    var bucket = Math.floor(Date.now() / 1000);
    if (bucket !== lastTripBucket) {
      lastTripBucket = bucket;
      paintTrip();
      paintPhoneLive();
      paintFleetRows();
      if (state.view === "admin" && state.adminPanel === "tablets" && state.adminId === "RS-2041") paintTabletDetail();
      if (state.view === "admin" && state.adminPanel === "rides") paintRideTable();
    }
    requestAnimationFrame(frame);
  }

  function bind() {
    document.querySelector(".dock").addEventListener("click", function (event) {
      var btn = event.target.closest("[data-view]");
      if (!btn) return;
      openView(btn.dataset.view);
    });
    document.querySelector(".side-nav").addEventListener("click", function (event) {
      var btn = event.target.closest("[data-panel]");
      if (!btn) return;
      openAdmin(btn.dataset.panel, null);
    });
    $("slide-next").addEventListener("click", function () { advance(1); });
    $("slide-prev").addEventListener("click", function () { advance(-1); });
    $("slide-skip").addEventListener("click", function () { advance(1); });
    $("qr-button").addEventListener("click", openSheet);
    $("sheet-close").addEventListener("click", closeSheet);
    $("scan-sheet").addEventListener("click", function (event) {
      if (event.target === $("scan-sheet")) closeSheet();
    });
    $("open-driver").addEventListener("click", function () { openView("driver"); });
    $("empty-admin").addEventListener("click", function () { openAdmin("campaigns", null); });
    $("bt-search").addEventListener("click", function () { setPhase("scanning"); });
    $("bt-cancel").addEventListener("click", function () { setPhase("idle"); });
    $("bt-device").addEventListener("click", function () { setPhase("connecting"); });
    $("bt-back").addEventListener("click", function () { setPhase("scanning"); });
    $("bt-send").addEventListener("click", sendToTablet);
    $("bt-disconnect").addEventListener("click", disconnect);
    $("see-tablet").addEventListener("click", function () { openView("tablet"); });
    $("mi-minus").addEventListener("click", function () { adjust("mi", -0.1); });
    $("mi-plus").addEventListener("click", function () { adjust("mi", 0.1); });
    $("min-minus").addEventListener("click", function () { adjust("sec", -60); });
    $("min-plus").addEventListener("click", function () { adjust("sec", 60); });
    $("campaign-back").addEventListener("click", function () { openAdmin("campaigns", null); });
    $("campaign-toggle").addEventListener("click", function () {
      var id = state.adminId;
      if (!id) return;
      state.paused[id] = !state.paused[id];
      if (state.paused[adAt(state.slide).id]) advance(1);
      paintCampaignDetail();
      paintCampaignBadges();
      paintSlide();
    });
    $("tablet-back").addEventListener("click", function () { openAdmin("tablets", null); });
    $("tablet-open").addEventListener("click", function () { openView("tablet"); });
    $("ride-back").addEventListener("click", function () { openAdmin("rides", null); });
    $("ride-body").addEventListener("click", function (event) {
      var row = event.target.closest("[data-ride]");
      if (!row) return;
      openAdmin("rides", row.dataset.ride);
    });
    document.addEventListener("keydown", function (event) {
      if (event.key === "1") openView("tablet");
      if (event.key === "2") openView("driver");
      if (event.key === "3") openView("admin");
      if (event.key === "Escape") closeSheet();
      if (state.view !== "tablet" || state.holdSlide) return;
      if (event.key === "ArrowRight") advance(1);
      if (event.key === "ArrowLeft") advance(-1);
    });
  }

  window.addEventListener("wheel", function (event) { event.preventDefault(); }, { passive: false });
  window.addEventListener("touchmove", function (event) { event.preventDefault(); }, { passive: false });
  $("today").textContent = new Date().toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" });
  buildSlides();
  buildRoutes();
  buildChart();
  buildCampaigns();
  buildTablets();
  bind();
  goTo(0, true);
  paintTrip();
  paintPhone();
  paintFleetRows();
  readHash();
  requestAnimationFrame(frame);

  window.setInterval(function () {
    if (state.phase === "connected" && state.link && tripNow() && !tripNow().arrived) {
      noteSend();
      paintPhoneLive();
    }
  }, 5000);
})();
