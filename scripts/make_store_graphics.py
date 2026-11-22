"""Writes the Google Play icon (512 x 512) and feature graphic (1024 x 500) from the hub's icon drawing.

Usage: python3 scripts/make_store_graphics.py PATH/TO/synbee/package/synbee-spk/make_icons.py
"""
import importlib.util
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HONEY = (255, 243, 205, 255)  # the launcher icon's background, @color/ic_launcher_background
INK = (52, 36, 22, 255)

spec = importlib.util.spec_from_file_location("make_icons", sys.argv[1])
icons = importlib.util.module_from_spec(spec)
spec.loader.exec_module(icons)
bee = icons.draw_icon()

out = Path(__file__).resolve().parent.parent / "docs" / "play-store"
out.mkdir(exist_ok=True)

# Play icon: full square, no transparency (Play adds the rounded mask itself).
icon = Image.new("RGBA", (512, 512), HONEY)
small = icons.scaled(bee, 440)
icon.alpha_composite(small, (36, 36))
icon.convert("RGB").save(out / "icon-512.png")

# Feature graphic: the bee on the left, the name and a line on the right.
feature = Image.new("RGBA", (1024, 500), HONEY)
feature.alpha_composite(icons.scaled(bee, 380), (40, 60))


def font(size, bold=False):
    for name in (["DejaVuSans-Bold.ttf"] if bold else ["DejaVuSans.ttf"]):
        for root in ("/usr/share/fonts/truetype/dejavu", "/usr/share/fonts/TTF", "/usr/share/fonts/dejavu"):
            p = Path(root) / name
            if p.exists():
                return ImageFont.truetype(str(p), size)
    return ImageFont.load_default()


d = ImageDraw.Draw(feature)
d.text((450, 150), "SynBee", font=font(96, bold=True), fill=INK)
d.text((454, 285), "Your home, on your own NAS.", font=font(32), fill=INK)
d.text((454, 332), "Private. Local. No cloud.", font=font(32), fill=INK)
feature.convert("RGB").save(out / "feature-1024x500.png")
print("written", out / "icon-512.png", out / "feature-1024x500.png")
