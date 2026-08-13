#!/usr/bin/env python3
"""生成辉石系额外粒子贴图（碎晶 / 闪星多帧 / 绽光），并导出到 assets。

画布固定 32×32。运行：
  python 工具链/gen_glintstone_particles.py
"""
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


def export(data: dict, json_path: Path, png_path: Path) -> None:
    json_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    art = load_pixel_art(json_path)
    save_png(art, png_path, scale=1)
    print(f"Wrote {json_path.name} -> {png_path}")


def make_shard() -> dict:
    """斜向菱形碎晶：亮核 + 青蓝体 + 外缘辉光，用于命中飞溅。"""
    pixels: list[dict] = []
    cx, cy = 15.5, 15.5
    # 菱形主轴：沿 (1,-1) 拉长，像碎裂的辉石片
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            # 旋转约 35° 的曼哈顿菱形距离
            along = dx * 0.82 + dy * (-0.57)
            across = dx * 0.57 + dy * 0.82
            # 尖头纺锤：两端细、中间稍宽
            half_len = 9.5
            u = abs(along) / half_len
            if u >= 1.0:
                continue
            half_width = 2.6 * (1.0 - u * u) + 0.35
            dist = abs(across)
            if dist > half_width + 1.8:
                continue
            t = dist / (half_width + 1.8)
            if dist < half_width * 0.22:
                rgba = (245, 255, 255, 255)
            elif dist < half_width * 0.55:
                rgba = (120, 245, 255, 240)
            elif dist < half_width * 0.9:
                rgba = (45, 200, 230, int(210 * (1.0 - u * 0.3)))
            elif dist < half_width + 0.6:
                rgba = (30, 160, 190, int(110 * (1.0 - t)))
            else:
                rgba = (20, 120, 150, int(40 * (1.0 - t)))
            put(pixels, x, y, rgba)
    # 尖端高光
    put(pixels, 22, 9, (255, 255, 255, 255))
    put(pixels, 9, 22, (180, 250, 255, 180))
    return {"name": "glintstone_shard", "width": W, "height": H, "pixels": pixels}


def make_mote(frame: int) -> dict:
    """十字星闪烁：frame 0/1/2 控制光芒伸展，粒子用 setSpriteFromAge 循环。"""
    pixels: list[dict] = []
    cx, cy = 15.5, 15.5
    # 帧越亮，射线越长
    arm = 2.2 + frame * 1.8
    core_r = 1.1 + frame * 0.25
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            d = math.hypot(dx, dy)
            # 轴对齐十字 + 对角细十字
            on_axis = min(abs(dx), abs(dy))
            on_diag = min(abs(dx - dy), abs(dx + dy)) * 0.707
            ray = min(on_axis, on_diag * 1.15)
            # 核心圆盘
            if d <= core_r:
                bright = 1.0 - d / max(core_r, 0.01)
                a = int(255 * (0.85 + 0.15 * bright))
                put(pixels, x, y, (230, 255, 255, a))
                continue
            # 射线
            if d > arm + 1.5:
                continue
            if ray > 0.85 + frame * 0.15:
                continue
            fade = 1.0 - d / (arm + 1.5)
            thickness_fade = 1.0 - ray / (0.85 + frame * 0.15 + 0.01)
            a = int(200 * fade * thickness_fade * (0.55 + 0.2 * frame))
            if a < 8:
                continue
            put(pixels, x, y, (70, 230, 245, a))
    # 四端微光点（仅中/后帧）
    if frame >= 1:
        tips = [(15, int(15 - arm)), (15, int(15 + arm)), (int(15 - arm), 15), (int(15 + arm), 15)]
        for tx, ty in tips:
            put(pixels, tx, ty, (200, 255, 255, 120 + frame * 40))
    return {"name": f"glintstone_mote_{frame}", "width": W, "height": H, "pixels": pixels}


def make_flare() -> dict:
    """中心绽光：硬核 + 柔和径向光晕，用于施法/命中瞬间闪光。"""
    pixels: list[dict] = []
    cx, cy = 15.5, 15.5
    for y in range(H):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            # 轻微椭圆，略扁，更像法术冲击波截面
            d = math.hypot(dx * 1.05, dy * 0.95) / 14.5
            if d >= 1.0:
                continue
            # 内亮外淡；中段略抬升，形成光环感
            ring = math.exp(-((d - 0.35) ** 2) / 0.06) * 0.35
            core = max(0.0, 1.0 - d * 2.2) ** 1.6
            body = (1.0 - d) ** 2.2
            intensity = min(1.0, core * 1.1 + body * 0.55 + ring)
            a = int(220 * intensity)
            if a < 4:
                continue
            if d < 0.18:
                rgba = (255, 255, 255, a)
            elif d < 0.4:
                rgba = (160, 250, 255, a)
            elif d < 0.7:
                rgba = (50, 210, 230, a)
            else:
                rgba = (25, 150, 180, a)
            put(pixels, x, y, rgba)
    return {"name": "glintstone_flare", "width": W, "height": H, "pixels": pixels}


def make_mist() -> dict:
    """稀薄雾气团：不规则软斑，拖尾体积感。"""
    pixels: list[dict] = []
    cx, cy = 15.5, 15.5
    # 三个偏移高斯核叠成不规则雾
    blobs = [
        (0.0, 0.0, 1.0, 11.0),
        (-3.5, 2.0, 0.55, 7.5),
        (2.8, -2.5, 0.45, 6.5),
        (1.5, 3.2, 0.35, 5.5),
    ]
    for y in range(H):
        for x in range(W):
            accum = 0.0
            for bx, by, weight, radius in blobs:
                d = math.hypot(x - (cx + bx), y - (cy + by)) / radius
                if d < 1.0:
                    accum += weight * (1.0 - d) * (1.0 - d)
            if accum < 0.04:
                continue
            a = int(min(140, 160 * accum))
            put(pixels, x, y, (35, 175, 195, a))
    return {"name": "glintstone_mist", "width": W, "height": H, "pixels": pixels}


def main() -> int:
    assets = ROOT.parent / "src" / "main" / "resources" / "assets" / "elden_ring_spells"
    particle_tex = assets / "textures" / "particle"
    particle_tex.mkdir(parents=True, exist_ok=True)

    shard = make_shard()
    export(shard, ROOT / "辉石碎晶.json", particle_tex / "glintstone_shard.png")

    for frame in range(3):
        mote = make_mote(frame)
        export(mote, ROOT / f"辉石闪星_{frame}.json", particle_tex / f"glintstone_mote_{frame}.png")

    flare = make_flare()
    export(flare, ROOT / "辉石绽光.json", particle_tex / "glintstone_flare.png")

    mist = make_mist()
    export(mist, ROOT / "辉石雾气.json", particle_tex / "glintstone_mist.png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
