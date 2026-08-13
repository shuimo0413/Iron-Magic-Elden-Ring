"""生成辉石连续光轨的 32x32 白色透明度纹理。

纹理在运行时由 Java 顶点颜色着色：
- U 轴：中心亮、左右柔和淡出，消除硬矩形边；
- V 轴：弹头端亮，尾端平滑淡出，形成连续彗星尾。

仅使用 Python 标准库，输出到模组资源目录。
"""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path


WIDTH = 32
HEIGHT = 32
OUTPUT_PATH = (
    Path(__file__).resolve().parents[1]
    / "src/main/resources/assets/elden_ring_spells/textures/entity/glintstone/trail_beam.png"
)


def smoothstep(edge0: float, edge1: float, value: float) -> float:
    """Return a smooth 0..1 interpolation between two edges."""
    normalized = max(0.0, min(1.0, (value - edge0) / (edge1 - edge0)))
    return normalized * normalized * (3.0 - 2.0 * normalized)


def png_chunk(chunk_type: bytes, payload: bytes) -> bytes:
    """Encode one PNG chunk."""
    checksum = zlib.crc32(chunk_type)
    checksum = zlib.crc32(payload, checksum)
    return (
        struct.pack(">I", len(payload))
        + chunk_type
        + payload
        + struct.pack(">I", checksum & 0xFFFFFFFF)
    )


def build_rgba_rows() -> bytes:
    """Build filtered PNG scanlines with a soft linear alpha mask."""
    rows = bytearray()
    for y in range(HEIGHT):
        rows.append(0)  # PNG filter type: None
        along = y / (HEIGHT - 1)
        # Head (top) stays bright; final 18% smoothly vanishes at the tail.
        tail_fade = 1.0 - smoothstep(0.72, 1.0, along)
        longitudinal_energy = (1.0 - along * 0.35) * tail_fade

        for x in range(WIDTH):
            across = abs((x + 0.5) / WIDTH * 2.0 - 1.0)
            # Thin white core plus broad soft shoulder.
            core = math.exp(-((across / 0.18) ** 2))
            halo = math.exp(-((across / 0.62) ** 2)) * 0.55
            alpha = max(core, halo) * longitudinal_energy
            rows.extend((255, 255, 255, round(max(0.0, min(1.0, alpha)) * 255)))
    return bytes(rows)


def main() -> None:
    """Write the 32x32 RGBA PNG."""
    signature = b"\x89PNG\r\n\x1a\n"
    header = struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 6, 0, 0, 0)
    png = (
        signature
        + png_chunk(b"IHDR", header)
        + png_chunk(b"IDAT", zlib.compress(build_rgba_rows(), level=9))
        + png_chunk(b"IEND", b"")
    )
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_bytes(png)
    print(f"Wrote {OUTPUT_PATH} ({WIDTH}x{HEIGHT})")


if __name__ == "__main__":
    main()
