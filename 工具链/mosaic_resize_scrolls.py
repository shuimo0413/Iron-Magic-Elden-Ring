#!/usr/bin/env python3
"""
将「法术的png原图」中的法环 Wiki 图标，用区域平均马赛克算法压到 64×64，
并覆盖写入 assets 里对应的魔法卷轴物品贴图。

用法（项目根目录）:
  python 工具链/mosaic_resize_scrolls.py
  python 工具链/mosaic_resize_scrolls.py --size 64
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

# 本脚本所在目录 = 工具链/
TOOLCHAIN_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = TOOLCHAIN_DIR.parent

# 原图目录（Wiki 导出的 100px 图标）
SOURCE_DIR = PROJECT_ROOT / "法术的png原图"

# 物品贴图输出目录
ITEM_TEXTURE_DIR = (
    PROJECT_ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "elden_ring_spells"
    / "textures"
    / "item"
)

# 中文原图文件名（去掉 100px- 前缀后的法术名）→ 卷轴贴图文件名
# 仅替换项目里已注册的卷轴；其余原图留给后续法术再用。
SCROLL_MAPPING: dict[str, str] = {
    "辉石魔砾": "glintstone_pebble_scroll.png",
    "辉石迅魔砾": "swift_glintstone_shard_scroll.png",
    "辉石大魔砾": "great_glintstone_shard_scroll.png",
    "辉石彗星": "glintstone_comet_scroll.png",
    "辉石流星": "glintstone_stars_scroll.png",
    "流星雨": "star_shower_scroll.png",
    "帚星": "comet_scroll.png",
    "旋飞魔砾": "spiral_shard_scroll.png",
}

# 法术图标（GUI）输出目录与文件名映射（与 spell path 一致）
SPELL_ICON_DIR = (
    PROJECT_ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "elden_ring_spells"
    / "textures"
    / "gui"
    / "spell_icons"
)

SPELL_ICON_MAPPING: dict[str, str] = {
    "旋飞魔砾": "spiral_shard.png",
    "流星雨": "star_shower.png",
}

# 原图文件名前缀（Wiki 缩略图命名）
SOURCE_FILENAME_PREFIX = "100px-"


def mosaic_resize(source: Image.Image, target_width: int, target_height: int) -> Image.Image:
    """
    区域平均马赛克缩放：把源图按目标分辨率划分成网格，
    每个目标像素取对应源矩形内所有像素的 RGBA 算术平均。

    比最近邻更稳、比双线性更「块状」，适合把 100px Wiki 图标压成 MC 物品贴图。
    调大 target_* 更细；调小更糊、更马赛克。
    """
    rgba_source = source.convert("RGBA")
    source_width, source_height = rgba_source.size
    source_pixels = rgba_source.load()

    result = Image.new("RGBA", (target_width, target_height))
    result_pixels = result.load()

    for target_y in range(target_height):
        source_y_start = (target_y * source_height) // target_height
        source_y_end = ((target_y + 1) * source_height) // target_height
        if source_y_end <= source_y_start:
            source_y_end = source_y_start + 1

        for target_x in range(target_width):
            source_x_start = (target_x * source_width) // target_width
            source_x_end = ((target_x + 1) * source_width) // target_width
            if source_x_end <= source_x_start:
                source_x_end = source_x_start + 1

            sum_red = 0
            sum_green = 0
            sum_blue = 0
            sum_alpha = 0
            sample_count = 0

            for source_y in range(source_y_start, source_y_end):
                for source_x in range(source_x_start, source_x_end):
                    red, green, blue, alpha = source_pixels[source_x, source_y]
                    sum_red += red
                    sum_green += green
                    sum_blue += blue
                    sum_alpha += alpha
                    sample_count += 1

            result_pixels[target_x, target_y] = (
                sum_red // sample_count,
                sum_green // sample_count,
                sum_blue // sample_count,
                sum_alpha // sample_count,
            )

    return result


def resolve_source_path(spell_display_name: str) -> Path:
    """按 Wiki 命名约定定位原图：100px-<法术名>.png"""
    return SOURCE_DIR / f"{SOURCE_FILENAME_PREFIX}{spell_display_name}.png"


def convert_one(
    spell_display_name: str,
    scroll_texture_filename: str,
    target_size: int,
) -> Path:
    """读取一张原图，马赛克压到 target_size×target_size，写入卷轴贴图路径。"""
    source_path = resolve_source_path(spell_display_name)
    if not source_path.is_file():
        raise FileNotFoundError(f"找不到原图: {source_path}")

    output_path = ITEM_TEXTURE_DIR / scroll_texture_filename
    with Image.open(source_path) as source_image:
        source_width, source_height = source_image.size
        mosaic_image = mosaic_resize(source_image, target_size, target_size)
        mosaic_image.save(output_path, format="PNG")

    print(
        f"OK  {source_path.name} ({source_width}x{source_height}) "
        f"-> {output_path.relative_to(PROJECT_ROOT)} ({target_size}x{target_size})"
    )
    return output_path


def convert_spell_icon(
    spell_display_name: str,
    icon_texture_filename: str,
    target_size: int,
) -> Path:
    """将 Wiki 原图压成法术图标（默认 32×32）。"""
    source_path = resolve_source_path(spell_display_name)
    if not source_path.is_file():
        raise FileNotFoundError(f"找不到原图: {source_path}")

    output_path = SPELL_ICON_DIR / icon_texture_filename
    with Image.open(source_path) as source_image:
        source_width, source_height = source_image.size
        mosaic_image = mosaic_resize(source_image, target_size, target_size)
        mosaic_image.save(output_path, format="PNG")

    print(
        f"OK  {source_path.name} ({source_width}x{source_height}) "
        f"-> {output_path.relative_to(PROJECT_ROOT)} ({target_size}x{target_size})"
    )
    return output_path


def main() -> int:
    parser = argparse.ArgumentParser(description="马赛克压缩法术原图并替换卷轴物品贴图")
    parser.add_argument(
        "--size",
        type=int,
        default=64,
        help="输出边长（像素），默认 64；改成 32 可回到常见 MC 物品尺寸",
    )
    parser.add_argument(
        "--icon-size",
        type=int,
        default=32,
        help="法术 GUI 图标边长（像素），默认 32",
    )
    parser.add_argument(
        "--only",
        type=str,
        default="",
        help="只处理指定中文法术名（如 旋飞魔砾）；空则处理全部映射",
    )
    args = parser.parse_args()
    target_size = args.size
    icon_size = args.icon_size
    if target_size < 1 or icon_size < 1:
        raise SystemExit("--size / --icon-size 必须 >= 1")

    if not SOURCE_DIR.is_dir():
        raise SystemExit(f"原图目录不存在: {SOURCE_DIR}")
    ITEM_TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    SPELL_ICON_DIR.mkdir(parents=True, exist_ok=True)

    print(f"原图目录: {SOURCE_DIR}")
    print(f"输出目录: {ITEM_TEXTURE_DIR}")
    print(f"目标尺寸: {target_size}x{target_size}")
    print("---")

    only_name = args.only.strip()
    scroll_items = (
        {only_name: SCROLL_MAPPING[only_name]}
        if only_name and only_name in SCROLL_MAPPING
        else SCROLL_MAPPING
    )
    if only_name and only_name not in SCROLL_MAPPING and only_name not in SPELL_ICON_MAPPING:
        raise SystemExit(f"--only 未在映射中: {only_name}")

    for spell_display_name, scroll_texture_filename in scroll_items.items():
        if only_name and spell_display_name != only_name:
            continue
        convert_one(spell_display_name, scroll_texture_filename, target_size)

    icon_items = (
        {only_name: SPELL_ICON_MAPPING[only_name]}
        if only_name and only_name in SPELL_ICON_MAPPING
        else SPELL_ICON_MAPPING
    )
    for spell_display_name, icon_texture_filename in icon_items.items():
        if only_name and spell_display_name != only_name:
            continue
        convert_spell_icon(spell_display_name, icon_texture_filename, icon_size)

    print("---")
    print(f"已替换卷轴贴图 {len(scroll_items)} 张，图标 {len(icon_items)} 张。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
