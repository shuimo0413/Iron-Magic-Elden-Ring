#!/usr/bin/env python3
"""Generate 32x32 icons/scrolls for Swift/Great Glintstone Shard, Glintstone Stars, and Comet."""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from render_pixel_art import load_pixel_art, save_png  # noqa: E402

W = H = 32


def export(data: dict, json_path: Path, png_path: Path) -> None:
    json_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    art = load_pixel_art(json_path)
    save_png(art, png_path, scale=1)
    print(f"Wrote {json_path.name} -> {png_path}")


def blank_grid() -> list[list[tuple[int, int, int, int] | None]]:
    return [[None] * W for _ in range(H)]


def setp(grid, x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
    if 0 <= x < W and 0 <= y < H and rgba[3] > 0:
        grid[y][x] = rgba


def draw_scroll_base(grid) -> None:
    parchment = (210, 210, 212, 255)
    parchment_dark = (168, 168, 172, 255)
    roll = (185, 185, 188, 255)
    roll_edge = (130, 130, 134, 255)
    for y in range(2, 27):
        for x in range(9, 23):
            if y < 4 and (x < 10 or x > 21):
                continue
            setp(grid, x, y, parchment if ((x + y * 3) % 7) else parchment_dark)
    for y in range(26, 30):
        for x in range(8, 24):
            setp(grid, x, y, roll if y < 29 else roll_edge)
    for x in range(9, 23):
        setp(grid, x, 25, roll_edge)


def draw_shard(
    grid,
    *,
    x0: float,
    y0: float,
    x1: float,
    y1: float,
    width_scale: float,
    core: tuple[int, int, int, int],
    body: tuple[int, int, int, int],
    mid: tuple[int, int, int, int],
    glow: tuple[int, int, int, int],
    outer: tuple[int, int, int, int],
) -> None:
    steps = 48
    for t in range(steps):
        u = t / (steps - 1)
        x = x0 + (x1 - x0) * u
        y = y0 + (y1 - y0) * u
        cx, cy = int(round(x)), int(round(y))
        width = (0.55 + 1.35 * math.sin(u * math.pi)) * width_scale
        for dy in range(-5, 6):
            for dx in range(-5, 6):
                along = (dx + dy) * 0.15
                across = abs(dx - dy) * 0.55 + abs(dx + dy) * 0.15
                dist = across + abs(along) * 0.05
                if dist > width + 1.8:
                    continue
                if dist < width * 0.28:
                    setp(grid, cx + dx, cy + dy, core)
                elif dist < width * 0.65:
                    setp(grid, cx + dx, cy + dy, body)
                elif dist < width * 1.05:
                    setp(grid, cx + dx, cy + dy, mid)
                elif dist < width + 0.7:
                    setp(grid, cx + dx, cy + dy, glow)
                else:
                    setp(grid, cx + dx, cy + dy, outer)


def grid_to_data(name: str, grid) -> dict:
    pixels = [
        {"x": x, "y": y, "rgba": list(rgba)}
        for y in range(H)
        for x in range(W)
        if (rgba := grid[y][x]) is not None
    ]
    return {"name": name, "width": W, "height": H, "pixels": pixels}


def make_swift() -> dict:
    grid = blank_grid()
    draw_scroll_base(grid)
    # Thin bright needle
    draw_shard(
        grid,
        x0=11.0, y0=21.0, x1=23.5, y1=5.5,
        width_scale=0.72,
        core=(245, 255, 255, 255),
        body=(120, 245, 255, 255),
        mid=(60, 220, 240, 220),
        glow=(70, 210, 230, 110),
        outer=(40, 170, 190, 45),
    )
    setp(grid, 24, 5, (255, 255, 255, 255))
    return grid_to_data("swift_glintstone_shard_scroll", grid)


def make_great() -> dict:
    grid = blank_grid()
    draw_scroll_base(grid)
    # Thick deep-cyan shard
    draw_shard(
        grid,
        x0=8.5, y0=23.0, x1=22.5, y1=6.0,
        width_scale=1.55,
        core=(220, 250, 255, 255),
        body=(40, 190, 235, 255),
        mid=(20, 150, 210, 230),
        glow=(30, 140, 190, 130),
        outer=(15, 100, 150, 55),
    )
    setp(grid, 23, 5, (255, 255, 255, 255))
    return grid_to_data("great_glintstone_shard_scroll", grid)


def make_stars() -> dict:
    grid = blank_grid()
    draw_scroll_base(grid)
    # Three small diagonal meteors
    shards = [
        (10.0, 20.0, 16.0, 10.0),
        (12.0, 22.0, 20.0, 8.0),
        (14.0, 23.0, 23.0, 11.0),
    ]
    for x0, y0, x1, y1 in shards:
        draw_shard(
            grid,
            x0=x0, y0=y0, x1=x1, y1=y1,
            width_scale=0.55,
            core=(255, 250, 240, 255),
            body=(160, 220, 255, 255),
            mid=(80, 180, 240, 220),
            glow=(90, 170, 230, 100),
            outer=(50, 130, 190, 40),
        )
    for x, y in [(16, 9), (20, 7), (23, 10)]:
        setp(grid, x, y, (255, 255, 255, 255))
    return grid_to_data("glintstone_stars_scroll", grid)


def make_stars_of_ruin() -> dict:
    grid = blank_grid()
    draw_scroll_base(grid)
    # Eight dark/bright blue meteors in a loose cluster
    shards = [
        (9.0, 21.0, 15.0, 11.0),
        (10.5, 22.5, 18.0, 8.5),
        (12.0, 23.5, 21.0, 10.0),
        (13.5, 21.5, 23.0, 7.5),
        (11.0, 20.0, 17.5, 9.0),
        (14.0, 24.0, 22.0, 12.5),
        (8.5, 19.5, 14.5, 12.0),
        (15.5, 22.0, 24.0, 9.5),
    ]
    use_bright_blue = True
    for x0, y0, x1, y1 in shards:
        if use_bright_blue:
            draw_shard(
                grid,
                x0=x0, y0=y0, x1=x1, y1=y1,
                width_scale=0.48,
                core=(210, 230, 255, 255),
                body=(70, 120, 255, 255),
                mid=(40, 80, 220, 220),
                glow=(50, 90, 210, 110),
                outer=(20, 40, 140, 45),
            )
        else:
            draw_shard(
                grid,
                x0=x0, y0=y0, x1=x1, y1=y1,
                width_scale=0.48,
                core=(140, 170, 255, 255),
                body=(20, 40, 160, 255),
                mid=(12, 24, 110, 230),
                glow=(18, 30, 120, 120),
                outer=(8, 14, 70, 50),
            )
        use_bright_blue = not use_bright_blue
    for x, y in [(15, 10), (18, 8), (21, 9), (23, 7), (17, 12)]:
        setp(grid, x, y, (220, 235, 255, 255))
    return grid_to_data("stars_of_ruin_scroll", grid)


def make_comet() -> dict:
    grid = blank_grid()
    draw_scroll_base(grid)
    # Long heavy comet with trailing tail motes
    draw_shard(
        grid,
        x0=7.5, y0=24.0, x1=24.0, y1=5.0,
        width_scale=1.85,
        core=(210, 245, 255, 255),
        body=(20, 160, 230, 255),
        mid=(10, 120, 200, 235),
        glow=(20, 110, 190, 140),
        outer=(10, 80, 150, 60),
    )
    # Tail sparkles
    for x, y, a in [
        (8, 24, 200), (9, 23, 180), (10, 25, 140),
        (7, 22, 120), (11, 26, 100), (6, 25, 90),
    ]:
        setp(grid, x, y, (60, 200, 255, a))
    setp(grid, 24, 4, (255, 255, 255, 255))
    setp(grid, 23, 5, (230, 250, 255, 255))
    return grid_to_data("comet_scroll", grid)


def make_glintstone_comet() -> dict:
    grid = blank_grid()
    draw_scroll_base(grid)
    # Mid-weight comet between great shard and broom comet
    draw_shard(
        grid,
        x0=8.0, y0=23.5, x1=23.5, y1=5.5,
        width_scale=1.7,
        core=(230, 250, 255, 255),
        body=(30, 175, 235, 255),
        mid=(15, 135, 205, 230),
        glow=(25, 125, 190, 135),
        outer=(12, 90, 155, 55),
    )
    for x, y, a in [
        (8, 23, 190), (9, 24, 160), (7, 22, 130),
        (10, 25, 110), (6, 24, 95),
    ]:
        setp(grid, x, y, (55, 195, 255, a))
    setp(grid, 24, 4, (255, 255, 255, 255))
    setp(grid, 23, 5, (240, 250, 255, 255))
    return grid_to_data("glintstone_comet_scroll", grid)


def main() -> int:
    assets = ROOT.parent / "src" / "main" / "resources" / "assets" / "elden_ring_spells"
    item_tex = assets / "textures" / "item"
    gui_tex = assets / "textures" / "gui" / "spell_icons"
    item_tex.mkdir(parents=True, exist_ok=True)
    gui_tex.mkdir(parents=True, exist_ok=True)

    specs = [
        ("辉石迅魔砾", "swift_glintstone_shard", make_swift),
        ("辉石大魔砾", "great_glintstone_shard", make_great),
        ("辉石彗星", "glintstone_comet", make_glintstone_comet),
        ("辉石流星", "glintstone_stars", make_stars),
        ("毁灭流星", "stars_of_ruin", make_stars_of_ruin),
        ("帚星", "comet", make_comet),
    ]
    only_name = sys.argv[1].strip() if len(sys.argv) > 1 else ""
    for cn_name, spell_path, factory in specs:
        if only_name and cn_name != only_name and spell_path != only_name:
            continue
        data = factory()
        scroll_id = f"{spell_path}_scroll"
        export(data, ROOT / f"{cn_name}卷轴.json", item_tex / f"{scroll_id}.png")
        # Spell icon shares scroll art (ER-style)
        icon_data = dict(data)
        icon_data["name"] = spell_path
        export(icon_data, ROOT / f"{cn_name}.json", gui_tex / f"{spell_path}.png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
