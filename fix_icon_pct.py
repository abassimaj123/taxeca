"""
Fix TaxeCA icon: replace $ with a properly centered % symbol.
Uses actual pixel-ink bounding box for true visual centering.
"""

from PIL import Image, ImageDraw, ImageFont
import os

BG_RED  = (198, 40, 40, 255)
WHITE   = (255, 255, 255, 255)
FONT    = 'C:/Windows/Fonts/arialbd.ttf'

ICON_DIRS = [
    r'D:\mob\TaxeCA\app\src\main\res\mipmap-mdpi',
    r'D:\mob\TaxeCA\app\src\main\res\mipmap-hdpi',
    r'D:\mob\TaxeCA\app\src\main\res\mipmap-xhdpi',
    r'D:\mob\TaxeCA\app\src\main\res\mipmap-xxhdpi',
    r'D:\mob\TaxeCA\app\src\main\res\mipmap-xxxhdpi',
]

def render_pct_centered(size: int) -> Image.Image:
    """Render a % glyph and return a perfectly pixel-centered RGBA patch."""
    # We'll use the bottom 32% of the icon for the % strip
    strip_h = int(size * 0.32)
    strip_top = size - strip_h          # y where the strip starts

    # Step 1 — render % at a large size onto a temp canvas to get ink bounds
    font_size = int(strip_h * 0.80)     # slightly smaller than strip height
    font = ImageFont.truetype(FONT, font_size)

    # Render on oversized canvas so nothing is clipped
    tmp_size = size * 2
    tmp = Image.new('RGBA', (tmp_size, tmp_size), (0, 0, 0, 0))
    d   = ImageDraw.Draw(tmp)
    d.text((tmp_size // 4, tmp_size // 4), '%', font=font, fill=WHITE)

    # Step 2 — find actual ink pixel bounds (crop away transparency)
    bbox = tmp.getbbox()   # (left, top, right, bottom) of non-zero pixels
    if bbox is None:
        return Image.new('RGBA', (size, size), BG_RED)

    glyph = tmp.crop(bbox)              # just the glyph pixels, tightly cropped
    gw, gh = glyph.size

    # Step 3 — scale glyph to fit inside the strip with padding
    max_h = int(strip_h * 0.78)
    max_w = int(size  * 0.60)
    scale = min(max_h / gh, max_w / gw)
    new_w = int(gw * scale)
    new_h = int(gh * scale)
    glyph = glyph.resize((new_w, new_h), Image.LANCZOS)

    # Step 4 — compose onto a full-size red canvas
    canvas = Image.new('RGBA', (size, size), BG_RED)

    # Center horizontally; center vertically within the strip
    paste_x = (size  - new_w) // 2
    paste_y = strip_top + (strip_h - new_h) // 2
    canvas.paste(glyph, (paste_x, paste_y), mask=glyph)
    return canvas, strip_top


def patch_icon(path: str):
    img = Image.open(path).convert('RGBA')
    size = img.size[0]                  # square icon

    result, strip_top = render_pct_centered(size)

    # Overlay: keep the top part of the original icon, replace bottom with %
    out = img.copy()
    # Erase old content in the strip area
    erase = Image.new('RGBA', (size, size - strip_top), BG_RED)
    out.paste(erase, (0, strip_top))
    # Paste the glyph layer (only the strip_top..size region matters)
    strip = result.crop((0, strip_top, size, size))
    out.paste(strip, (0, strip_top), mask=strip)

    out.save(path, 'PNG')
    print(f'  patched {os.path.basename(os.path.dirname(path))}/{os.path.basename(path)}  [{size}x{size}]')


def main():
    for d in ICON_DIRS:
        for name in ('ic_launcher.png', 'ic_launcher_round.png'):
            p = os.path.join(d, name)
            if os.path.exists(p):
                patch_icon(p)
            else:
                print(f'  SKIP (not found): {p}')
    print('\nDone — all icons patched.')

if __name__ == '__main__':
    main()
