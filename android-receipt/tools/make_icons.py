"""Generates the launcher icons (pure python, no image libraries needed)."""
import os
import struct
import zlib

BLUE = (11, 107, 193, 255)
WHITE = (255, 255, 255, 255)
INK = (28, 28, 28, 255)
CLEAR = (0, 0, 0, 0)

SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def blank(size):
    return [[CLEAR for _ in range(size)] for _ in range(size)]


def rounded_rect(px, size, x0, y0, x1, y1, radius, color):
    for y in range(max(0, y0), min(size, y1)):
        for x in range(max(0, x0), min(size, x1)):
            dx = 0
            dy = 0
            if x < x0 + radius:
                dx = x0 + radius - x
            elif x > x1 - 1 - radius:
                dx = x - (x1 - 1 - radius)
            if y < y0 + radius:
                dy = y0 + radius - y
            elif y > y1 - 1 - radius:
                dy = y - (y1 - 1 - radius)
            if dx * dx + dy * dy <= radius * radius:
                px[y][x] = color


def write_png(path, px):
    size = len(px)
    raw = bytearray()
    for row in px:
        raw.append(0)
        for pixel in row:
            raw.extend(pixel)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as handle:
        handle.write(png)


def make_icon(size):
    px = blank(size)
    rounded_rect(px, size, 0, 0, size, size, int(size * 0.22), BLUE)
    # receipt body
    left = int(size * 0.26)
    right = int(size * 0.74)
    top = int(size * 0.18)
    bottom = int(size * 0.82)
    rounded_rect(px, size, left, top, right, bottom, int(size * 0.05), WHITE)
    # text lines
    line_color = (150, 150, 150, 255)
    y = top + int(size * 0.12)
    for _ in range(3):
        rounded_rect(px, size, left + int(size * 0.07), y, right - int(size * 0.10), y + max(2, int(size * 0.03)), 1, line_color)
        y += int(size * 0.09)
    # barcode
    bar_top = y + int(size * 0.01)
    bar_bottom = min(bottom - int(size * 0.06), bar_top + int(size * 0.13))
    bar_x = left + int(size * 0.07)
    widths = [2, 1, 3, 1, 2, 1, 1, 3, 1, 2]
    index = 0
    while bar_x < right - int(size * 0.08):
        w = max(1, int(size * 0.012) * widths[index % len(widths)])
        if index % 2 == 0:
            rounded_rect(px, size, bar_x, bar_top, min(bar_x + w, right), bar_bottom, 0, INK)
        bar_x += w + max(1, int(size * 0.008))
        index += 1
    return px


def main():
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "res")
    for density, size in SIZES.items():
        folder = os.path.join(root, "mipmap-" + density)
        os.makedirs(folder, exist_ok=True)
        write_png(os.path.join(folder, "ic_launcher.png"), make_icon(size))
        print("wrote", os.path.join(folder, "ic_launcher.png"))


if __name__ == "__main__":
    main()
