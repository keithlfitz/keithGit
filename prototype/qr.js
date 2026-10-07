/* Self-contained QR encoder (byte mode, error correction M, versions 1–5).
   Draws a matrix the passenger display can render. No network calls. */
(function (global) {
  "use strict";

  const EXP = new Uint8Array(512);
  const LOG = new Uint8Array(256);
  (function initField() {
    let x = 1;
    for (let i = 0; i < 255; i++) {
      EXP[i] = x;
      LOG[x] = i;
      x <<= 1;
      if (x & 0x100) x ^= 0x11d;
    }
    for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
  })();

  // Data codewords, per-block data lengths, ECC codewords per block, remainder bits.
  const VERSIONS = {
    1: { blocks: [16], ecc: 10, rem: 0 },
    2: { blocks: [28], ecc: 16, rem: 7 },
    3: { blocks: [44], ecc: 26, rem: 7 },
    4: { blocks: [32, 32], ecc: 18, rem: 7 },
    5: { blocks: [43, 43], ecc: 24, rem: 7 },
  };

  const ALIGN = {
    2: [6, 18],
    3: [6, 22],
    4: [6, 26],
    5: [6, 30],
  };

  function dataCapacity(version) {
    return VERSIONS[version].blocks.reduce((a, b) => a + b, 0);
  }

  function chooseVersion(byteLength) {
    for (let v = 1; v <= 5; v++) {
      const bits = dataCapacity(v) * 8;
      const needed = 4 + 8 + byteLength * 8;
      if (needed <= bits) return v;
    }
    throw new Error("QR payload is too long for this demo encoder");
  }

  function pushBits(bits, value, length) {
    for (let i = length - 1; i >= 0; i--) bits.push((value >>> i) & 1);
  }

  function encodeData(bytes, version) {
    const cap = dataCapacity(version);
    const bits = [];
    pushBits(bits, 0b0100, 4);
    pushBits(bits, bytes.length, 8);
    for (let i = 0; i < bytes.length; i++) pushBits(bits, bytes[i], 8);
    const maxBits = cap * 8;
    const term = Math.min(4, maxBits - bits.length);
    for (let i = 0; i < term; i++) bits.push(0);
    while (bits.length % 8 !== 0) bits.push(0);
    const out = [];
    for (let i = 0; i < bits.length; i += 8) {
      let v = 0;
      for (let j = 0; j < 8; j++) v = (v << 1) | bits[i + j];
      out.push(v);
    }
    const pads = [0xec, 0x11];
    let p = 0;
    while (out.length < cap) out.push(pads[p++ % 2]);
    return out;
  }

  function rsGenerator(degree) {
    let poly = [1];
    for (let i = 0; i < degree; i++) {
      const next = new Array(poly.length + 1).fill(0);
      for (let j = 0; j < poly.length; j++) {
        next[j] ^= poly[j];
        if (poly[j]) next[j + 1] ^= EXP[(LOG[poly[j]] + i) % 255];
      }
      poly = next;
    }
    return poly;
  }

  const GEN = {};
  function generator(degree) {
    if (!GEN[degree]) GEN[degree] = rsGenerator(degree);
    return GEN[degree];
  }

  function rsRemainder(data, degree) {
    const gen = generator(degree);
    const res = data.concat(new Array(degree).fill(0));
    for (let i = 0; i < data.length; i++) {
      const factor = res[i];
      if (!factor) continue;
      for (let j = 0; j < gen.length; j++) {
        res[i + j] ^= EXP[(LOG[gen[j]] + LOG[factor]) % 255];
      }
    }
    return res.slice(data.length);
  }

  function interleave(data, version) {
    const spec = VERSIONS[version];
    const blocks = [];
    let offset = 0;
    for (const len of spec.blocks) {
      blocks.push(data.slice(offset, offset + len));
      offset += len;
    }
    const eccs = blocks.map((block) => rsRemainder(block, spec.ecc));
    const out = [];
    const maxData = Math.max.apply(null, spec.blocks);
    for (let i = 0; i < maxData; i++) {
      for (const block of blocks) if (i < block.length) out.push(block[i]);
    }
    for (let i = 0; i < spec.ecc; i++) {
      for (const ecc of eccs) out.push(ecc[i]);
    }
    return out;
  }

  function blank(size) {
    const m = [];
    const r = [];
    for (let y = 0; y < size; y++) {
      m.push(new Array(size).fill(false));
      r.push(new Array(size).fill(false));
    }
    return { m, r };
  }

  function setFinder(grid, row, col) {
    const { m, r, size } = grid;
    for (let dy = -1; dy <= 7; dy++) {
      for (let dx = -1; dx <= 7; dx++) {
        const y = row + dy;
        const x = col + dx;
        if (y < 0 || x < 0 || y >= size || x >= size) continue;
        let dark = false;
        if (dy >= 0 && dy <= 6 && dx >= 0 && dx <= 6) {
          dark =
            dy === 0 ||
            dy === 6 ||
            dx === 0 ||
            dx === 6 ||
            (dy >= 2 && dy <= 4 && dx >= 2 && dx <= 4);
        }
        m[y][x] = dark;
        r[y][x] = true;
      }
    }
  }

  function setAlignment(grid, cx, cy) {
    const { m, r } = grid;
    if (r[cy][cx]) return;
    for (let dy = -2; dy <= 2; dy++) {
      for (let dx = -2; dx <= 2; dx++) {
        m[cy + dy][cx + dx] = Math.max(Math.abs(dx), Math.abs(dy)) !== 1;
        r[cy + dy][cx + dx] = true;
      }
    }
  }

  function reserveFormat(grid) {
    const { r, size } = grid;
    for (let i = 0; i < 9; i++) {
      r[8][i] = true;
      r[i][8] = true;
    }
    for (let i = 0; i < 8; i++) {
      r[8][size - 1 - i] = true;
      r[size - 1 - i][8] = true;
    }
  }

  function buildFunction(version) {
    const size = 21 + 4 * (version - 1);
    const grid = blank(size);
    grid.size = size;
    setFinder(grid, 0, 0);
    setFinder(grid, 0, size - 7);
    setFinder(grid, size - 7, 0);
    for (let i = 0; i < size; i++) {
      if (!grid.r[6][i]) {
        grid.m[6][i] = i % 2 === 0;
        grid.r[6][i] = true;
      }
      if (!grid.r[i][6]) {
        grid.m[i][6] = i % 2 === 0;
        grid.r[i][6] = true;
      }
    }
    const align = ALIGN[version];
    if (align) {
      for (const y of align) {
        for (const x of align) setAlignment(grid, x, y);
      }
    }
    grid.m[size - 8][8] = true;
    grid.r[size - 8][8] = true;
    reserveFormat(grid);
    return grid;
  }

  function placeData(grid, codewords, remainder) {
    const { m, r, size } = grid;
    const bits = [];
    for (const cw of codewords) {
      for (let i = 7; i >= 0; i--) bits.push((cw >> i) & 1);
    }
    for (let i = 0; i < remainder; i++) bits.push(0);
    let idx = 0;
    let upward = true;
    for (let col = size - 1; col > 0; col -= 2) {
      if (col === 6) col--;
      for (let step = 0; step < size; step++) {
        const row = upward ? size - 1 - step : step;
        for (const c of [col, col - 1]) {
          if (r[row][c]) continue;
          m[row][c] = idx < bits.length ? bits[idx] === 1 : false;
          idx++;
        }
      }
      upward = !upward;
    }
  }

  function maskBit(mask, row, col) {
    switch (mask) {
      case 0:
        return (row + col) % 2 === 0;
      case 1:
        return row % 2 === 0;
      case 2:
        return col % 3 === 0;
      case 3:
        return (row + col) % 3 === 0;
      case 4:
        return (Math.floor(row / 2) + Math.floor(col / 3)) % 2 === 0;
      case 5:
        return ((row * col) % 2) + ((row * col) % 3) === 0;
      case 6:
        return (((row * col) % 2) + ((row * col) % 3)) % 2 === 0;
      default:
        return (((row + col) % 2) + ((row * col) % 3)) % 2 === 0;
    }
  }

  function formatBits(mask) {
    const eccM = 0;
    const data = (eccM << 3) | mask;
    let rem = data << 10;
    for (let i = 14; i >= 10; i--) {
      if ((rem >>> i) & 1) rem ^= 0x537 << (i - 10);
    }
    return ((data << 10) | (rem & 0x3ff)) ^ 0x5412;
  }

  function bit(value, index) {
    return ((value >>> index) & 1) === 1;
  }

  function applyFormat(matrix, reserved, size, mask) {
    const fmt = formatBits(mask);
    const write = (row, col, i) => {
      matrix[row][col] = bit(fmt, i);
    };
    for (let i = 0; i <= 5; i++) write(i, 8, i);
    write(7, 8, 6);
    write(8, 8, 7);
    write(8, 7, 8);
    for (let i = 9; i < 15; i++) write(8, 14 - i, i);
    for (let i = 0; i < 8; i++) write(8, size - 1 - i, i);
    for (let i = 8; i < 15; i++) write(size - 15 + i, 8, i);
    void reserved;
  }

  function penalty(matrix) {
    const n = matrix.length;
    let score = 0;
    const run = (get) => {
      for (let a = 0; a < n; a++) {
        let count = 1;
        let prev = get(a, 0);
        for (let b = 1; b < n; b++) {
          const v = get(a, b);
          if (v === prev) {
            count++;
            if (count === 5) score += 3;
            else if (count > 5) score += 1;
          } else {
            count = 1;
            prev = v;
          }
        }
      }
    };
    run((row, col) => matrix[row][col]);
    run((col, row) => matrix[row][col]);

    for (let r = 0; r < n - 1; r++) {
      for (let c = 0; c < n - 1; c++) {
        const v = matrix[r][c];
        if (
          v === matrix[r][c + 1] &&
          v === matrix[r + 1][c] &&
          v === matrix[r + 1][c + 1]
        ) {
          score += 3;
        }
      }
    }

    const pat = (seq) => {
      for (let i = 0; i < seq.length - 10; i++) {
        const s = seq.slice(i, i + 11).join("");
        if (s === "10111010000" || s === "00001011101") score += 40;
      }
    };
    for (let r = 0; r < n; r++) {
      pat(matrix[r].map((v) => (v ? 1 : 0)));
      const col = [];
      for (let c = 0; c < n; c++) col.push(matrix[c][r] ? 1 : 0);
      pat(col);
    }

    let dark = 0;
    for (let r = 0; r < n; r++) {
      for (let c = 0; c < n; c++) if (matrix[r][c]) dark++;
    }
    const percent = (dark * 100) / (n * n);
    score += 10 * Math.floor(Math.abs(percent - 50) / 5);
    return score;
  }

  function cloneMatrix(matrix) {
    return matrix.map((row) => row.slice());
  }

  function encode(text) {
    const bytes = new TextEncoder().encode(text);
    const version = chooseVersion(bytes.length);
    const spec = VERSIONS[version];
    const data = encodeData(bytes, version);
    const codewords = interleave(data, version);
    const base = buildFunction(version);
    placeData(base, codewords, spec.rem);

    let best = null;
    let bestScore = Infinity;
    let bestMask = 0;
    for (let mask = 0; mask <= 7; mask++) {
      const matrix = cloneMatrix(base.m);
      for (let y = 0; y < base.size; y++) {
        for (let x = 0; x < base.size; x++) {
          if (!base.r[y][x] && maskBit(mask, y, x)) matrix[y][x] = !matrix[y][x];
        }
      }
      applyFormat(matrix, base.r, base.size, mask);
      const score = penalty(matrix);
      if (score < bestScore) {
        bestScore = score;
        best = matrix;
        bestMask = mask;
      }
    }
    return { version, size: base.size, mask: bestMask, modules: best };
  }

  function draw(canvas, text, quiet) {
    const qr = encode(text);
    const q = quiet == null ? 4 : quiet;
    const n = qr.size + q * 2;
    const css = canvas.clientWidth || 240;
    const dpr = Math.min(global.devicePixelRatio || 1, 2);
    const px = Math.max(1, Math.floor((css * dpr) / n));
    canvas.width = px * n;
    canvas.height = px * n;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#14181f";
    for (let y = 0; y < qr.size; y++) {
      for (let x = 0; x < qr.size; x++) {
        if (qr.modules[y][x]) ctx.fillRect((x + q) * px, (y + q) * px, px, px);
      }
    }
    return qr;
  }

  global.RidesQR = { encode, draw };
})(window);
