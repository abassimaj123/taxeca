const { createCanvas } = require('canvas');
const fs = require('fs');
const path = require('path');

const W = 1024, H = 500;
const c = createCanvas(W, H);
const ctx = c.getContext('2d');

// ── Background gradient ──────────────────────────────────────────────────────
const grad = ctx.createLinearGradient(0, 0, W, H);
grad.addColorStop(0, '#C62828');
grad.addColorStop(1, '#B71C1C');
ctx.fillStyle = grad;
ctx.fillRect(0, 0, W, H);

// ── Watermark maple leaf (top-right, low opacity) ────────────────────────────
function drawMapleLeaf(ctx, cx, cy, iconSize, alpha) {
  const scale = (iconSize * 0.62) / 3720;
  const T = (px, py) => [cx + (px - 2310) * scale, cy + (py - 2415) * scale];
  let x = 2400, y = 4430;
  ctx.beginPath();
  ctx.moveTo(...T(x, y));
  const step = (dx, dy) => { x += dx; y += dy; ctx.lineTo(...T(x, y)); };
  step(-45,-863); step(111,-98); step(859,151); step(-116,-320); step(20,-73);
  step(941,-762); step(-212,-99); step(-34,-79); step(186,-572); step(-542,115);
  step(-73,-38); step(-105,-247); step(-423,454); step(-111,-57); step(204,-1052);
  step(-327,189); step(-91,-27); step(-332,-652); step(-332,652); step(-91,27);
  step(-327,-189); step(204,1052); step(-111,57); step(-423,-454); step(-105,247);
  step(-73,38); step(-542,-115); step(186,572); step(-34,79); step(-212,99);
  step(941,762); step(20,73); step(-116,320); step(859,-151); step(111,98);
  step(-45,863);
  ctx.closePath();
  ctx.globalAlpha = alpha;
  ctx.fillStyle = '#FFFFFF';
  ctx.fill();
  ctx.globalAlpha = 1;
}
drawMapleLeaf(ctx, W * 0.82, H * 0.42, 420, 0.06);

// ── App icon (left side) ─────────────────────────────────────────────────────
function drawIcon(ctx, x, y, size) {
  const s = size, r = s * 0.22;
  // shadow
  ctx.shadowColor = 'rgba(0,0,0,0.45)';
  ctx.shadowBlur = 28;
  ctx.shadowOffsetY = 8;
  // rounded rect background
  ctx.fillStyle = '#C62828';
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + s - r, y); ctx.arcTo(x + s, y, x + s, y + r, r);
  ctx.lineTo(x + s, y + s - r); ctx.arcTo(x + s, y + s, x + s - r, y + s, r);
  ctx.lineTo(x + r, y + s); ctx.arcTo(x, y + s, x, y + s - r, r);
  ctx.lineTo(x, y + r); ctx.arcTo(x, y, x + r, y, r);
  ctx.closePath();
  ctx.fill();
  ctx.shadowColor = 'transparent';
  ctx.shadowBlur = 0;
  ctx.shadowOffsetY = 0;
  // border
  ctx.strokeStyle = 'rgba(255,255,255,0.15)';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + s - r, y); ctx.arcTo(x + s, y, x + s, y + r, r);
  ctx.lineTo(x + s, y + s - r); ctx.arcTo(x + s, y + s, x + s - r, y + s, r);
  ctx.lineTo(x + r, y + s); ctx.arcTo(x, y + s, x, y + s - r, r);
  ctx.lineTo(x, y + r); ctx.arcTo(x, y, x + r, y, r);
  ctx.closePath();
  ctx.stroke();
  // maple leaf
  ctx.fillStyle = '#FFFFFF';
  drawMapleLeaf(ctx, x + s * 0.5, y + s * 0.41, s, 1.0);
  // $ sign
  const dollarSize = Math.round(s * 0.19);
  ctx.fillStyle = '#FFFFFF';
  ctx.font = `bold ${dollarSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('$', x + s * 0.5, y + s * 0.88);
}

const iconSize = 230;
const iconX = 52;
const iconY = (H - iconSize) / 2;
drawIcon(ctx, iconX, iconY, iconSize);

// ── Divider ──────────────────────────────────────────────────────────────────
const divX = iconX + iconSize + 48;
ctx.strokeStyle = 'rgba(255,255,255,0.18)';
ctx.lineWidth = 1.5;
ctx.beginPath();
ctx.moveTo(divX, 48);
ctx.lineTo(divX, H - 48);
ctx.stroke();

// ── Text block (right of divider) ───────────────────────────────────────────
const textX = divX + 44;
const textW = W - textX - 32;
let textY = 62;

// 🍁 TaxeCA
ctx.textAlign = 'left';
ctx.textBaseline = 'top';
ctx.fillStyle = '#FFFFFF';
ctx.font = 'bold 82px Arial, sans-serif';
ctx.fillText('🍁 TaxeCA', textX, textY);
textY += 94;

// Subtitle
ctx.font = '28px Arial, sans-serif';
ctx.fillStyle = 'rgba(255,255,255,0.82)';
ctx.fillText('Canada Tax Calculator', textX, textY);
textY += 52;

// Divider under subtitle
ctx.strokeStyle = 'rgba(255,255,255,0.22)';
ctx.lineWidth = 1;
ctx.beginPath();
ctx.moveTo(textX, textY);
ctx.lineTo(textX + textW, textY);
ctx.stroke();
textY += 24;

// Feature bullets
const bullets = [
  '🏛️  Federal + All 13 Provinces & Territories',
  '🛒  Shopping List with Auto Tax',
  '🍽️  Restaurant Mode + Tip + Bill Split',
  '🔄  Reverse Calculation (Total → Before Tax)',
];
ctx.font = '24px Arial, sans-serif';
ctx.fillStyle = 'rgba(255,255,255,0.92)';
bullets.forEach(b => {
  ctx.fillText(b, textX, textY);
  textY += 44;
});

// ── Bottom banner ────────────────────────────────────────────────────────────
const bannerH = 44;
const bannerY = H - bannerH;
ctx.fillStyle = 'rgba(0,0,0,0.28)';
ctx.fillRect(0, bannerY, W, bannerH);

ctx.font = 'bold 18px Arial, sans-serif';
ctx.fillStyle = 'rgba(255,255,255,0.75)';
ctx.textAlign = 'center';
ctx.textBaseline = 'middle';
ctx.fillText('FREE  •  Bilingual FR/EN  •  All Provinces & Territories  •  Premium $1.99', W / 2, bannerY + bannerH / 2);

// ── Save ─────────────────────────────────────────────────────────────────────
const outDir = path.join(__dirname, '..', 'store_assets');
fs.mkdirSync(outDir, { recursive: true });
const outPath = path.join(outDir, 'feature_graphic.png');
fs.writeFileSync(outPath, c.toBuffer('image/png'));
console.log(`✓ feature_graphic.png  ${W}×${H}px  →  ${outPath}`);
