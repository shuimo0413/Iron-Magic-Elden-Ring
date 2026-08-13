#!/usr/bin/env python3
"""Generate 32x32 Glintstone Pebble pixel art (scroll + particles) and export PNGs."""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from render_pixel_art import load_pixel_art, save_png  # noqa: E402

W = H = 32


def put(pixels: list, x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
    if 0 <= x < W and 0 <= y < H and rgba[3] > 0:
        pixels.append({"x": x, "y": y, "rgba": list(rgba)})


def blend(a: tuple[int, int, int, int], b: tuple[int, int, int, int], t: float) -> tuple[int, int, int, int]:
    t = max(0.0, min(1.0, t))
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
        int(a[3] + (b[3] - a[3]) * t),
    )


def make_scroll() -> dict:
    """Scroll parchment + diagonal cyan glintstone shard (ER-style icon)."""
    # Layered grid so later cyan shard cleanly overwrites parchment/ink.
    grid: list[list[tuple[int, int, int, int] | None]] = [[None] * W for _ in range(H)]

    def setp(x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
        if 0 <= x < W and 0 <= y < H and rgba[3] > 0:
            grid[y][x] = rgba

    parchment = (210, 210, 212, 255)
    parchment_dark = (168, 168, 172, 255)
    ink = (55, 55, 60, 200)
    ink_soft = (80, 80, 88, 140)
    roll = (185, 185, 188, 255)
    roll_edge = (130, 130, 134, 255)

    # Scroll body
    for y in range(2, 27):
        for x in range(9, 23):
            if y < 4 and (x < 10 or x > 21):
                continue
            setp(x, y, parchment if ((x + y * 3) % 7) else parchment_dark)

    # Bottom roll
    for y in range(26, 30):
        for x in range(8, 24):
            setp(x, y, roll if y < 29 else roll_edge)
    for x in range(9, 23):
        setp(x, 25, roll_edge)

    # Soft runic crest (behind the shard)
    for x, y in [
        (16, 8), (15, 9), (17, 9), (14, 10), (18, 10), (16, 11),
        (13, 12), (19, 12), (16, 13), (14, 14), (18, 14), (15, 15), (17, 15),
        (12, 11), (20, 11), (12, 15), (20, 15), (11, 13), (21, 13),
    ]:
        setp(x, y, ink)
    for x, y in [(10, 10), (22, 10), (10, 16), (22, 16), (16, 7), (16, 18)]:
        setp(x, y, ink_soft)

    # Needle-like cyan shard (bright core, teal body, soft aura)
    core = (235, 255, 255, 255)
    body = (70, 235, 245, 255)
    mid = (40, 200, 220, 230)
    glow = (50, 190, 210, 120)
    outer = (35, 150, 170, 50)

    for t in range(0, 48):
        u = t / 47.0
        x = 9.5 + u * 13.5
        y = 22.5 - u * 16.5
        cx, cy = int(round(x)), int(round(y))
        # Thin needle: wider mid-body, sharp tip
        width = 0.55 + 1.35 * math.sin(u * math.pi)
        for dy in range(-4, 5):
            for dx in range(-4, 5):
                # Stretch along diagonal (dx+dy ~ 0 is along shard)
                along = (dx + dy) * 0.15
                across = abs(dx - dy) * 0.55 + abs(dx + dy) * 0.15
                dist = across + abs(along) * 0.05
                if dist > width + 1.8:
                    continue
                px, py = cx + dx, cy + dy
                if dist < width * 0.28:
                    setp(px, py, core)
                elif dist < width * 0.65:
                    setp(px, py, body)
                elif dist < width * 1.05:
                    setp(px, py, mid)
                elif dist < width + 0.7:
                    setp(px, py, glow)
                else:
                    setp(px, py, outer)

    # Tip highlight + trail motes
    setp(23, 6, (255, 255, 255, 255))
    setp(22, 7, core)
    for x, y in [(10, 21), (11, 20), (12, 22), (9, 19), (13, 19)]:
        setp(x, y, (90, 235, 245, 200))

    pixels = [
        {"x": x, "y": y, "rgba": list(rgba)}
        for y in range(H)
        for x in range(W)
        if (rgba := grid[y][x]) is not None
    ]
    return {"name": "glintstone_pebble_scroll", "width": W, "height": H, "pixels": pixels}


def make_spark() -> dict:
    pixels: list[dict] = []
    cx, cy = 15, 15
    for y in range(H):
        for x in range(W):
            d = math.hypot(x - cx, y - cy)
            if d < 1.2:
                put(pixels, x, y, (240, 255, 255, 255))
            elif d < 2.4:
                put(pixels, x, y, (100, 240, 250, 220))
            elif d < 4.0:
                put(pixels, x, y, (40, 180, 200, 90))
            elif d < 6.0:
                put(pixels, x, y, (20, 120, 140, 30))
    return {"name": "glintstone_spark", "width": W, "height": H, "pixels": pixels}


def make_glow() -> dict:
    pixels: list[dict] = []
    cx, cy = 15.5, 15.5
    for y in range(H):
        for x in range(W):
            d = math.hypot(x - cx, y - cy) / 14.0
            if d >= 1.0:
                continue
            a = int(160 * (1.0 - d) * (1.0 - d))
            put(pixels, x, y, (40, 200, 210, a))
    return {"name": "glintstone_glow", "width": W, "height": H, "pixels": pixels}


def export(data: dict, json_path: Path, png_path: Path) -> None:
    json_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    art = load_pixel_art(json_path)
    save_png(art, png_path, scale=1)
    print(f"Wrote {json_path.name} -> {png_path}")


def main() -> int:
    assets = ROOT.parent / "src" / "main" / "resources" / "assets" / "elden_ring_spells"
    item_tex = assets / "textures" / "item"
    particle_tex = assets / "textures" / "particle"
    gui_tex = assets / "textures" / "gui" / "spell_icons"
    item_tex.mkdir(parents=True, exist_ok=True)
    particle_tex.mkdir(parents=True, exist_ok=True)
    gui_tex.mkdir(parents=True, exist_ok=True)

    scroll = make_scroll()
    export(scroll, ROOT / "辉石魔砾卷轴.json", item_tex / "glintstone_pebble_scroll.png")
    # Spell icon shares the same art
    export(scroll, ROOT / "辉石魔砾.json", gui_tex / "glintstone_pebble.png")

    spark = make_spark()
    export(spark, ROOT / "辉石火花.json", particle_tex / "glintstone_spark.png")

    glow = make_glow()
    export(glow, ROOT / "辉石辉光.json", particle_tex / "glintstone_glow.png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
