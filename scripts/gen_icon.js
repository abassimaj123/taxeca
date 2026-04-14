const { createCanvas } = require('canvas');
const fs = require('fs');
const path = require('path');

const BASE = path.join(__dirname, '..', 'app', 'src', 'main', 'res');
const SIZES = [
  { dir: 'mipmap-mdpi',    px: 48  },
  { dir: 'mipmap-hdpi',    px: 72  },
  { dir: 'mipmap-xhdpi',   px: 96  },
  { dir: 'mipmap-xxhdpi',  px: 144 },
  { dir: 'mipmap-xxxhdpi', px: 192 },
];

/**
 * Feuille d'érable canadienne officielle
 * Path extrait du SVG du drapeau (Wikipedia commons: Flag_of_Canada_(Pantone).svg)
 *
 * Le path orignal utilise des arcs SVG (a) de rayon 65–95 dans un espace de
 * 3720 unités de large → ils font <2% de la largeur et sont imperceptibles
 * à l'échelle d'une icône → simplifiés en lineTo.
 *
 * Bounding box calculée en traçant tous les deltas relatifs :
 *   X : [450, 4170]  →  largeur 3720, centre X = 2310
 *   Y : [400, 4430]  →  hauteur 4030, centre Y = 2415
 */
function drawMapleLeaf(ctx, cx, cy, iconSize) {
  // Scale : feuille = 62% de la largeur de l'icône
  const scale = (iconSize * 0.62) / 3720;

  // Transforme un point path → canvas
  const T = (px, py) => [cx + (px - 2310) * scale, cy + (py - 2415) * scale];

  let x = 2400, y = 4430;
  ctx.beginPath();
  ctx.moveTo(...T(x, y));

  // Avance de (dx, dy) en relatif et trace un lineTo
  const step = (dx, dy) => {
    x += dx; y += dy;
    ctx.lineTo(...T(x, y));
  };

  // ── Path officiel du drapeau canadien (arcs → lineTo) ──────────────
  step(-45, -863);
  step(111,  -98);   // a95 95 0 0 1  111 -98
  step(859,  151);
  step(-116, -320);
  step( 20,  -73);   // a65 65 0 0 1   20 -73
  step(941, -762);
  step(-212,  -99);
  step(-34,  -79);   // a65 65 0 0 1  -34 -79
  step(186, -572);
  step(-542,  115);
  step(-73,  -38);   // a65 65 0 0 1  -73 -38
  step(-105, -247);
  step(-423,  454);
  step(-111,  -57);  // a65 65 0 0 1 -111 -57
  step(204, -1052);
  step(-327,  189);
  step(-91,  -27);   // a65 65 0 0 1  -91 -27
  step(-332, -652);
  step(-332,  652);
  step(-91,   27);   // a65 65 0 0 1  -91  27
  step(-327, -189);
  step(204,  1052);
  step(-111,   57);  // a65 65 0 0 1 -111  57
  step(-423, -454);
  step(-105,  247);
  step(-73,   38);   // a65 65 0 0 1  -73  38
  step(-542, -115);
  step(186,  572);
  step(-34,   79);   // a65 65 0 0 1  -34  79
  step(-212,   99);
  step(941,  762);
  step( 20,   73);   // a65 65 0 0 1   20  73
  step(-116,  320);
  step(859, -151);
  step(111,   98);   // a95 95 0 0 1  111  98
  step(-45,  863);
  ctx.closePath();   // z : rejoint (2400, 4430) → trace la base de la tige
  ctx.fill();
}

function makeRoundIcon(sq) {
  const s = sq.width;
  const cr = createCanvas(s, s);
  const cx = cr.getContext('2d');
  cx.beginPath();
  cx.arc(s / 2, s / 2, s / 2, 0, Math.PI * 2);
  cx.clip();
  cx.drawImage(sq, 0, 0);
  return cr;
}

function drawIcon(size) {
  const c = createCanvas(size, size);
  const ctx = c.getContext('2d');
  const s = size;

  // ── Fond rouge Canada arrondi ────────────────────────────────────────
  const r = s * 0.22;
  ctx.fillStyle = '#C62828';
  ctx.beginPath();
  ctx.moveTo(r, 0);
  ctx.lineTo(s - r, 0); ctx.arcTo(s, 0, s, r, r);
  ctx.lineTo(s, s - r); ctx.arcTo(s, s, s - r, s, r);
  ctx.lineTo(r, s);     ctx.arcTo(0, s, 0, s - r, r);
  ctx.lineTo(0, r);     ctx.arcTo(0, 0, r, 0, r);
  ctx.closePath();
  ctx.fill();

  // ── Feuille d'érable officielle blanche ──────────────────────────────
  // Centre vertical à 41% → haut feuille ≈ 8%, bas tige ≈ 75%
  ctx.fillStyle = '#FFFFFF';
  drawMapleLeaf(ctx, s * 0.500, s * 0.410, s);

  // ── Petit "$" blanc sous la feuille ─────────────────────────────────
  const dollarSize = Math.round(s * 0.19);
  ctx.fillStyle = '#FFFFFF';
  ctx.font = `bold ${dollarSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('$', s * 0.500, s * 0.880);

  return c;
}

SIZES.forEach(({ dir, px }) => {
  const outDir = path.join(BASE, dir);
  fs.mkdirSync(outDir, { recursive: true });
  const sq = drawIcon(px);
  fs.writeFileSync(path.join(outDir, 'ic_launcher.png'),       sq.toBuffer('image/png'));
  fs.writeFileSync(path.join(outDir, 'ic_launcher_round.png'), makeRoundIcon(sq).toBuffer('image/png'));
  console.log(`✓ ${dir}  ${px}×${px}px`);
});
console.log('\nTaxeCA — feuille officielle Wikipedia flag path — done!');
