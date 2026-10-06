#!/usr/bin/env python3
"""Generate the bundled sample advertisement creatives.

These posters are fictional sample ads for the passenger tablet. They are
checked in as PNGs so the app runs without this script. Re-run from the
repository root after changing the artwork:

    python3 tools/generate_sample_creatives.py
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "images"
WIDTH = 1600
HEIGHT = 1000

SANS = "/usr/share/fonts/truetype/macos/Inter-Medium.ttf"
SANS_BOLD = "/usr/share/fonts/truetype/macos/Inter-SemiBold.ttf"
SERIF = "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf"


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size)


def tracked(draw: ImageDraw.ImageDraw, xy, text, face, fill, tracking: float) -> None:
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=face, fill=fill)
        x += draw.textlength(ch, font=face) + tracking


def poster(bg, fg, muted, accent, eyebrow, title_lines, footer, motif) -> Image.Image:
    image = Image.new("RGB", (WIDTH, HEIGHT), bg)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 20, HEIGHT), fill=accent)
    tracked(draw, (96, 86), eyebrow.upper(), font(SANS_BOLD, 26), muted, 5)
    draw.rectangle((96, 142, 248, 148), fill=accent)
    title_face = font(SERIF, 104)
    y = 196
    for line in title_lines:
        draw.text((92, y), line, font=title_face, fill=fg)
        y += 118
    footer_face = font(SANS, 34)
    fy = 860
    for line in footer:
        draw.text((96, fy), line, font=footer_face, fill=muted)
        fy += 46
    motif(draw, accent, fg)
    return image


def harbor(draw, accent, fg) -> None:
    draw.ellipse((1040, 180, 1500, 640), fill=accent)
    draw.ellipse((1120, 260, 1420, 560), fill=(22, 36, 54))
    draw.rounded_rectangle((1210, 470, 1330, 700), radius=28, fill=accent)
    for i, x in enumerate((1188, 1255, 1322)):
        draw.arc((x, 150 + i * 8, x + 36, 250 + i * 8), 200, 340, fill=accent, width=8)


def northline(draw, accent, fg) -> None:
    draw.ellipse((1080, 250, 1520, 620), fill=accent)
    draw.ellipse((1120, 300, 1480, 560), fill=(31, 61, 50))
    draw.rectangle((1120, 430, 1480, 620), fill=(31, 61, 50))
    draw.pieslice((1120, 360, 1480, 640), 0, 180, fill=accent)
    draw.ellipse((1230, 430, 1370, 560), fill=(31, 61, 50))


def lumen(draw, accent, fg) -> None:
    left, top = 1120, 210
    for row in range(4):
        for col in range(3):
            x = left + col * 130
            y = top + row * 160
            draw.rounded_rectangle((x, y, x + 96, y + 124), radius=6, fill=accent)
            draw.rounded_rectangle((x + 14, y + 16, x + 82, y + 108), radius=2, fill=(42, 36, 28))


def pike(draw, accent, fg) -> None:
    spines = [(214, 78, 92), (120, 48, 52), (92, 58, 70), (232, 196, 168), (70, 86, 104)]
    x = 1100
    for red, green, blue in spines:
        draw.rectangle((x, 220, x + 78, 760), fill=(red, green, blue))
        draw.rectangle((x + 10, 250, x + 16, 730), fill=accent)
        x += 92


def cedar(draw, accent, fg) -> None:
    draw.ellipse((1080, 200, 1500, 620), fill=accent)
    draw.polygon([(1290, 300), (1210, 520), (1290, 480), (1370, 520)], fill=(231, 242, 241))
    draw.ellipse((1262, 430, 1318, 500), fill=accent)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    ads = {
        "harbor-rye.png": poster(
            (22, 36, 54),
            (243, 237, 226),
            (214, 196, 168),
            (196, 149, 90),
            "Coffee",
            ["Stay a", "minute."],
            ["Harbor & Rye"],
            harbor,
        ),
        "northline-eats.png": poster(
            (31, 61, 50),
            (244, 239, 230),
            (190, 206, 186),
            (232, 196, 138),
            "Dinner",
            ["Nearby,", "and hot."],
            ["Northline Eats"],
            northline,
        ),
        "lumen-hotel.png": poster(
            (42, 36, 28),
            (247, 241, 230),
            (214, 196, 160),
            (230, 201, 138),
            "Stay",
            ["The quiet", "room."],
            ["Lumen Hotel"],
            lumen,
        ),
        "pike-street-books.png": poster(
            (244, 237, 226),
            (58, 28, 28),
            (110, 72, 64),
            (110, 42, 40),
            "Books",
            ["One more", "chapter."],
            ["Pike Street Books"],
            pike,
        ),
        "cedar-dental.png": poster(
            (231, 242, 241),
            (18, 72, 78),
            (40, 100, 106),
            (31, 111, 120),
            "Dental",
            ["Come in", "this week."],
            ["Cedar Dental"],
            cedar,
        ),
    }
    for name, image in ads.items():
        path = OUT / name
        image.save(path, "PNG", optimize=True)
        print(path)


if __name__ == "__main__":
    main()
