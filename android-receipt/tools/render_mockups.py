#!/usr/bin/env python3
"""Renders labeled design mockups of the WaImart screens.

These are NOT screenshots: this environment has no Android runtime. The receipt
mockup mirrors ReceiptView.buildLayout() (page width 372 dp, 11.9 dp item line,
9.6 dp body text, 0.68/0.80/1.0 column ratios, 92% wide 62 dp Code 128 block) and
the editor mockup mirrors ItemDetailsActivity.buildUi(). Receipt strings, totals
and the editor batch come from the app's own ReceiptModel via MockupDump.java, and
the barcode uses the real Code 128 module widths dumped by Code128Test.

Usage:
  python3 render_mockups.py --outdir build/mockups --data /tmp/mockdata.txt \
      --bits /tmp/bits.txt --fonts /tmp/fonts
"""
import argparse
import os

from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------- app colours
BLUE_BAR = (11, 107, 193, 255)
BLUE_DARK = (10, 78, 140, 255)
ORANGE = (233, 161, 59, 255)
ORANGE_DARK = (154, 106, 22, 255)
MAGENTA = (255, 0, 204, 255)
INK = (20, 20, 20, 255)
PAPER = (255, 255, 255, 255)
SCREEN_BG = (255, 255, 255, 255)
EDITOR_BG = (0, 0, 0, 255)
FOOTER_BG = (233, 233, 233, 255)
FOOTER_INK = (107, 107, 107, 255)
CAPTION_INK = (17, 17, 17, 255)
SUBTITLE_INK = (102, 102, 102, 255)

# ------------------------------------------------- app geometry (in dp, from Java)
SCREEN_W_DP = 390
SCREEN_H_DP = 844
STATUS_H_DP = 24
APPBAR_H_DP = 58

PAGE_W_DP = 372.0          # ReceiptView.PAGE_W_DP
ITEM_LINE_DP = 11.9        # ReceiptView.ITEM_LINE_DP
BODY_DP = 9.6              # body text size used across ReceiptView
UP_GAP_DP = 14.0
TOTALS_GAP_DP = 16.0
SOLD_H_DP = 28.0
BARCODE_H_DP = 62.0

TOOLBAR_BUTTON_H_DP = 44
TOOLBAR_PAD_DP = 8
EDITOR_FONT_DP = 14
EDITOR_LINE_DP = 17.5

SCALE = 3  # render pixels per dp


class Fonts:
    def __init__(self, folder):
        self.folder = folder
        self.cache = {}

    def _path(self, names):
        for name in names:
            path = os.path.join(self.folder, name)
            if os.path.exists(path) and os.path.getsize(path) > 0:
                return path
        return None

    def get(self, kind, px):
        key = (kind, px)
        if key in self.cache:
            return self.cache[key]
        px = max(6, int(round(px)))
        choices = {
            "black": ["Roboto-Black.ttf"],
            "bold": ["Roboto-Bold.ttf"],
            "regular": ["Roboto-Regular.ttf"],
            "mono": ["RobotoMono-Regular.ttf"],
            "mono_bold": ["RobotoMono-Bold.ttf"],
        }
        path = self._path(choices[kind])
        variable = {"black": None, "bold": None, "regular": None, "mono": None, "mono_bold": None}
        if path is None:
            # variable-font fallbacks
            if kind in ("mono", "mono_bold"):
                path = self._path(["RobotoMonoVar.ttf", "RobotoMono[wght].ttf"])
                variable[kind] = {"mono": "Light", "mono_bold": "Bold"}[kind]
            else:
                path = self._path(["RobotoVar.ttf", "Roboto[wdth,wght].ttf"])
                variable[kind] = {"black": "Black", "bold": "Bold", "regular": "Regular"}[kind]
        if path is None:
            font = ImageFont.load_default()
        else:
            font = ImageFont.truetype(path, px)
            name = variable[kind]
            if name:
                try:
                    font.set_variation_by_name(name)
                except Exception:
                    pass
        self.cache[key] = font
        return font


def baseline_for_center(center_y, font):
    ascent, descent = font.getmetrics()
    return center_y + (ascent - descent) / 2.0


def draw_tracked(draw, x, baseline, text, font, tracking, fill):
    """Left-aligned text with extra letter spacing (Paint.setLetterSpacing)."""
    cursor = x
    for ch in text:
        draw.text((cursor, baseline), ch, font=font, fill=fill, anchor="ls")
        cursor += font.getlength(ch) + tracking
    return cursor


def wrap(text, font, max_width):
    lines, line = [], ""
    for word in text.split(" "):
        if not word:
            continue
        candidate = word if not line else line + " " + word
        if not line or font.getlength(candidate) <= max_width:
            line = candidate
        else:
            lines.append(line)
            line = word
    if line:
        lines.append(line)
    return lines or [""]


def parse_data(path):
    data = {"store": [], "item": [], "editor": []}
    with open(path, "r", encoding="utf-8") as handle:
        for raw in handle:
            parts = raw.rstrip("\n").split("|")
            key = parts[0]
            if key == "store":
                data["store"].append(parts[1])
            elif key == "item":
                data["item"].append({"desc": parts[1], "upc": parts[2], "price": parts[3]})
            elif key == "editor":
                data["editor"].append(parts[1] if len(parts) > 1 else "")
            else:
                data[key] = parts[1] if len(parts) > 1 else ""
    return data


# ------------------------------------------------------------------- chrome
def draw_status_bar(draw, top, fonts, color, clock="5:18"):
    width = SCREEN_W_DP * SCALE
    draw.rectangle([0, top, width, top + STATUS_H_DP * SCALE], fill=color)
    font = fonts.get("regular", 13 * SCALE)
    draw.text((14 * SCALE, baseline_for_center(top + STATUS_H_DP * SCALE / 2, font)),
              clock, font=font, fill=PAPER, anchor="ls")

    # signal bars, wifi, battery
    right = width - 14 * SCALE
    bar_w, bar_gap = 3 * SCALE, 2 * SCALE
    for i in range(4):
        h = (4 + i * 3) * SCALE
        x = right - 78 * SCALE + i * (bar_w + bar_gap)
        draw.rectangle([x, top + STATUS_H_DP * SCALE / 2 + 5 * SCALE - h,
                        x + bar_w, top + STATUS_H_DP * SCALE / 2 + 5 * SCALE], fill=PAPER)
    # wifi
    wifi_cx = right - 42 * SCALE
    wifi_cy = top + STATUS_H_DP * SCALE / 2 + 3 * SCALE
    for radius in (11, 7):
        draw.arc([wifi_cx - radius * SCALE / 3, wifi_cy - radius * SCALE / 3,
                  wifi_cx + radius * SCALE / 3, wifi_cy + radius * SCALE / 3],
                 start=200, end=340, fill=PAPER, width=int(2 * SCALE))
    draw.ellipse([wifi_cx - 1.5 * SCALE, wifi_cy - 1.5 * SCALE,
                  wifi_cx + 1.5 * SCALE, wifi_cy + 1.5 * SCALE], fill=PAPER)
    # battery
    bx0 = right - 22 * SCALE
    bx1 = right
    by0 = top + STATUS_H_DP * SCALE / 2 - 5 * SCALE
    by1 = top + STATUS_H_DP * SCALE / 2 + 5 * SCALE
    draw.rectangle([bx0, by0, bx1, by1], outline=PAPER, width=int(1.4 * SCALE))
    draw.rectangle([bx1 + SCALE, (by0 + by1) / 2 - 2 * SCALE, bx1 + 2.4 * SCALE,
                    (by0 + by1) / 2 + 2 * SCALE], fill=PAPER)
    inner_w = (bx1 - bx0) * 0.62
    draw.rectangle([bx0 + 1.5 * SCALE, by0 + 1.5 * SCALE, bx0 + inner_w, by1 - 1.5 * SCALE],
                   fill=PAPER)


def draw_app_bar(draw, top, fonts, title):
    width = SCREEN_W_DP * SCALE
    height = APPBAR_H_DP * SCALE
    draw.rectangle([0, top, width, top + height], fill=BLUE_BAR)
    chevron = fonts.get("regular", 34 * SCALE)
    draw.text((10 * SCALE, baseline_for_center(top + height / 2, chevron) + 3 * SCALE),
              "\u2039", font=chevron, fill=PAPER, anchor="ls")
    font = fonts.get("bold", 21 * SCALE)
    draw.text((44 * SCALE, baseline_for_center(top + height / 2, font)), title,
              font=font, fill=PAPER, anchor="ls")


def draw_logo(draw, page_x, page_y, fonts, size):
    """Original WaImart lockup: receipt badge mark, wordmark, slogan.

    Mirrors ReceiptView.drawLogo(): 34 dp mark, 9 dp gap, 34 dp wordmark with
    -0.015 tracking, 8.6 dp slogan at 0.22 tracking, lockup centred on 372 dp.
    """
    word_font = fonts.get("black", 34 * size)
    head, tail = "Wa", "mart"
    head_w = word_font.getlength(head)
    tail_w = word_font.getlength(tail)
    cap_w = 34 * size * 0.34
    joint = 34 * size * 0.02

    mark = 34 * size
    gap = 9 * size
    total = mark + gap + head_w + joint + cap_w + joint + tail_w
    left = PAGE_W_DP / 2 * size - total / 2
    center_y = page_y + 46 * size
    ascent, descent = word_font.getmetrics()
    baseline = center_y + (ascent - descent) / 2.0
    word_left = page_x + left + mark + gap
    draw.text((word_left, baseline), head, font=word_font, fill=INK, anchor="ls")
    cap_left = word_left + head_w + joint
    cap_h = 34 * size * 0.715
    stem = 34 * size * 0.135
    bar = 34 * size * 0.11
    cap_top = baseline - cap_h
    draw.rounded_rectangle([cap_left, cap_top, cap_left + cap_w, cap_top + bar],
                           radius=34 * size * 0.02, fill=BLUE_BAR)
    draw.rounded_rectangle([cap_left + (cap_w - stem) / 2, cap_top, cap_left + (cap_w + stem) / 2, baseline],
                           radius=34 * size * 0.02, fill=BLUE_BAR)
    draw.rounded_rectangle([cap_left, baseline - bar, cap_left + cap_w, baseline],
                           radius=34 * size * 0.02, fill=BLUE_BAR)
    draw.text((cap_left + cap_w + joint, baseline), tail, font=word_font, fill=INK, anchor="ls")

    # mark: rounded badge with a torn bottom edge and a white W
    box = (page_x + left, center_y - mark / 2, page_x + left + mark, center_y + mark / 2)
    draw.rounded_rectangle(box, radius=mark * 0.26, fill=BLUE_BAR)
    notch_r = mark * 0.085
    for i in range(3):
        cx = box[0] + mark * (0.22 + 0.28 * i)
        draw.ellipse([cx - notch_r, box[3] - notch_r, cx + notch_r, box[3] + notch_r], fill=PAPER)

    points = [(0.21, 0.30), (0.325, 0.66), (0.50, 0.42), (0.675, 0.66), (0.79, 0.30)]
    xy = [(box[0] + px_ * mark, box[1] + py_ * mark) for px_, py_ in points]
    stroke_w = 0.115 * mark
    draw.line(xy, fill=PAPER, width=max(1, int(round(stroke_w))), joint="curve")
    for x, y in (xy[0], xy[-1]):
        draw.ellipse([x - stroke_w / 2, y - stroke_w / 2, x + stroke_w / 2, y + stroke_w / 2], fill=PAPER)

    slogan_font = fonts.get("bold", 8.6 * size)
    slogan = "KEEP EVERY LINE."
    draw_tracked(draw, word_left, baseline + 18 * size, slogan,
                 slogan_font, 0.22 * 8.6 * size, (91, 107, 122, 255))


# --------------------------------------------------------------- receipt screen
def render_receipt(img, fonts, data, bits):
    draw = ImageDraw.Draw(img)
    width = SCREEN_W_DP * SCALE
    draw.rectangle([0, 0, width, SCREEN_H_DP * SCALE], fill=SCREEN_BG)

    draw_status_bar(draw, 0, fonts, BLUE_DARK)
    draw_app_bar(draw, STATUS_H_DP * SCALE, fonts, "Receipt Detail")

    top = (STATUS_H_DP + APPBAR_H_DP) * SCALE
    fit = min((SCREEN_W_DP - 6) / PAGE_W_DP, 1.6)
    scale = fit * SCALE                       # page dp -> px
    page_x = (width - PAGE_W_DP * scale) / 2

    def px(x):
        return page_x + x * scale

    def w(text, size_dp, kind="mono"):
        return fonts.get(kind, size_dp * scale).getlength(text)

    y = 4 * scale
    draw_logo(draw, page_x, top + y, fonts, scale)
    y += 88 * scale

    for line in data["store"]:
        font = fonts.get("mono", BODY_DP * scale)
        baseline = top + y + (ITEM_LINE_DP * scale) / 2 + (font.getmetrics()[0] - font.getmetrics()[1]) / 2
        draw.text((px(PAGE_W_DP / 2), baseline), line, font=font, fill=INK, anchor="ms")
        y += ITEM_LINE_DP * scale

    font = fonts.get("mono", BODY_DP * scale)
    baseline = top + y + (ITEM_LINE_DP * scale) / 2 + (font.getmetrics()[0] - font.getmetrics()[1]) / 2
    draw.text((px(PAGE_W_DP / 2), baseline), data["st"], font=font, fill=INK, anchor="ms")
    y += ITEM_LINE_DP * scale + UP_GAP_DP * scale

    for item in data["item"]:
        desc_font = fonts.get("mono", BODY_DP * scale)
        lines = wrap(item["desc"], desc_font, 0.68 * PAGE_W_DP * scale)
        upc = item["upc"]
        own_line = False
        upc_x = 0
        if upc:
            upc_w = desc_font.getlength(upc)
            upc_x = 0.80 * PAGE_W_DP * scale - upc_w
            if upc_x < desc_font.getlength(lines[0]) + 5 * scale:
                own_line = True
        count = len(lines) + (1 if own_line else 0)
        ascent = desc_font.getmetrics()[0]
        first = top + y + ascent + 1 * scale
        for i, line in enumerate(lines):
            draw.text((px(2), first + i * ITEM_LINE_DP * scale), line, font=desc_font, fill=INK)
        if upc:
            line_index = len(lines) if own_line else 0
            draw.text((px(0.80 * PAGE_W_DP) - desc_font.getlength(upc), first + line_index * ITEM_LINE_DP * scale),
                      upc, font=desc_font, fill=INK)
        price_w = desc_font.getlength(item["price"])
        draw.text((px(PAGE_W_DP - 2) - price_w, first), item["price"], font=desc_font, fill=INK)
        y += count * ITEM_LINE_DP * scale

    y += TOTALS_GAP_DP * scale

    def total_row(label, value, rate=None):
        nonlocal y
        row_font = fonts.get("mono", BODY_DP * scale)
        baseline = top + y + (ITEM_LINE_DP * scale) / 2 + (row_font.getmetrics()[0] - row_font.getmetrics()[1]) / 2
        label_right = PAGE_W_DP * (0.55 if rate else 0.80)
        draw.text((px(label_right), baseline), label, font=row_font, fill=INK, anchor="rs")
        if rate:
            draw.text((px(PAGE_W_DP * 0.60), baseline), rate, font=row_font, fill=INK, anchor="ls")
        if value:
            draw.text((px(PAGE_W_DP - 2), baseline), value, font=row_font, fill=INK, anchor="rs")
        y += ITEM_LINE_DP * scale

    total_row("SUBTOTAL", data["subtotal"])
    total_row("TAX  1", data["tax1"], data["rate1"])
    total_row("TAX 12", data["tax2"], data["rate2"])
    total_row("TOTAL", data["total"])
    total_row("VISA CREDIT TEND", data["total"])
    total_row("VISA  **** **** **** " + data["visa"], "")
    total_row("CHANGE DUE", "0.00")

    y += 12 * scale
    sold_font = fonts.get("mono_bold", 16 * scale)
    baseline = top + y + (SOLD_H_DP * scale) / 2 + (sold_font.getmetrics()[0] - sold_font.getmetrics()[1]) / 2
    sold_text = "# ITEMS SOLD " + data["sold"]
    sold_w = sum(sold_font.getlength(c) for c in sold_text) + 0.08 * 16 * scale * (len(sold_text) - 1)
    draw_tracked(draw, page_x + (PAGE_W_DP * scale - sold_w) / 2, baseline, sold_text,
                 sold_font, 0.08 * 16 * scale, INK)
    y += SOLD_H_DP * scale

    y += 6 * scale
    tc_font = fonts.get("mono", BODY_DP * scale)
    baseline = top + y + (ITEM_LINE_DP * scale) / 2 + (tc_font.getmetrics()[0] - tc_font.getmetrics()[1]) / 2
    draw.text((px(PAGE_W_DP / 2), baseline), "TC# " + data["tc"], font=tc_font, fill=INK, anchor="ms")
    y += ITEM_LINE_DP * scale

    bar_y = top + y + 3 * scale
    bar_h = BARCODE_H_DP * scale
    bar_x0 = px(PAGE_W_DP * 0.04)
    bar_x1 = px(PAGE_W_DP * 0.96)
    module = (bar_x1 - bar_x0) / len(bits)
    x = bar_x0
    index = 0
    while index < len(bits):
        if bits[index] == "1":
            run = 1
            while index + run < len(bits) and bits[index + run] == "1":
                run += 1
            draw.rectangle([x, bar_y, x + run * module, bar_y + bar_h], fill=(17, 17, 17, 255))
            x += run * module
            index += run
        else:
            x += module
            index += 1
    y += bar_h

    y += 10 * scale
    stamp_font = fonts.get("mono", BODY_DP * scale)
    baseline = top + y + (ITEM_LINE_DP * scale) / 2 + (stamp_font.getmetrics()[0] - stamp_font.getmetrics()[1]) / 2
    draw.text((px(PAGE_W_DP / 2), baseline), data["ts"], font=stamp_font, fill=INK, anchor="ms")


# ---------------------------------------------------------------- editor screen
def render_editor(img, fonts, data):
    draw = ImageDraw.Draw(img)
    width = SCREEN_W_DP * SCALE
    height = SCREEN_H_DP * SCALE
    draw.rectangle([0, 0, width, height], fill=FOOTER_BG)

    draw_status_bar(draw, 0, fonts, ORANGE_DARK)

    bar_top = STATUS_H_DP * SCALE
    bar_h = (TOOLBAR_PAD_DP * 2 + TOOLBAR_BUTTON_H_DP) * SCALE
    draw.rectangle([0, bar_top, width, bar_top + bar_h], fill=ORANGE)

    labels = ["Save", "Duplicate", "Undo", "Redo"]
    inner_w = width - 8 * SCALE                       # bar padding 4dp each side
    button_w = inner_w / 4
    label_font = fonts.get("mono_bold", 14 * SCALE)
    for i, label in enumerate(labels):
        left = 4 * SCALE + i * button_w + 4 * SCALE
        right = 4 * SCALE + (i + 1) * button_w - 4 * SCALE
        top = bar_top + TOOLBAR_PAD_DP * SCALE
        bottom = top + TOOLBAR_BUTTON_H_DP * SCALE
        draw.rounded_rectangle([left, top, right, bottom], radius=2 * SCALE, fill=MAGENTA,
                               outline=(42, 42, 42, 255), width=max(1, int(SCALE)))
        text_w = label_font.getlength(label)
        draw.text(((left + right) / 2 - text_w / 2,
                   baseline_for_center((top + bottom) / 2, label_font)),
                  label, font=label_font, fill=PAPER, anchor="ls")

    # footer with the helper text, measured like the TextView
    hint_font = fonts.get("regular", 11.5 * SCALE)
    hint_text = ("One item per line as description$price. Lines starting with ## are ignored. "
                 "Save adds the items to the receipt; your list is kept as a draft until then.")
    hint_lines = wrap(hint_text, hint_font, (SCREEN_W_DP - 28) * SCALE)
    line_h = 13.8 * SCALE
    footer_h = (16 * SCALE) + len(hint_lines) * line_h
    footer_top = height - footer_h
    draw.rectangle([0, footer_top, width, height], fill=FOOTER_BG)
    ty = footer_top + 8 * SCALE + hint_font.getmetrics()[0]
    for line in hint_lines:
        draw.text((14 * SCALE, ty), line, font=hint_font, fill=FOOTER_INK, anchor="ls")
        ty += line_h

    rule_h = 3 * SCALE
    draw.rectangle([0, footer_top - rule_h, width, footer_top], fill=ORANGE)

    editor_top = bar_top + bar_h
    editor_bottom = footer_top - rule_h
    draw.rectangle([0, editor_top, width, editor_bottom], fill=EDITOR_BG)

    # gold stripe (GradientDrawable LEFT_RIGHT over 20 dp)
    stops = [(0.0, (62, 49, 15)), (0.35, (138, 109, 31)), (0.7, (184, 145, 42)), (1.0, (74, 58, 18))]
    stripe_w = 20 * SCALE
    for x in range(int(stripe_w)):
        t = x / max(1, stripe_w - 1)
        for i in range(len(stops) - 1):
            a, b = stops[i], stops[i + 1]
            if a[0] <= t <= b[0]:
                f = (t - a[0]) / (b[0] - a[0])
                color = tuple(int(a[1][k] + (b[1][k] - a[1][k]) * f) for k in range(3)) + (255,)
                draw.line([x, editor_top, x, editor_bottom], fill=color)
                break

    text_font = fonts.get("mono", EDITOR_FONT_DP * SCALE)
    left = 20 * SCALE + 12 * SCALE
    right = width - 10 * SCALE
    ty = editor_top + 12 * SCALE + text_font.getmetrics()[0]
    for logical in data["editor"]:
        for line in wrap(logical, text_font, right - left):
            if ty > editor_bottom - EDITOR_FONT_DP * SCALE:
                break
            draw.text((left, ty), line, font=text_font, fill=PAPER)
            ty += EDITOR_LINE_DP * SCALE


# -------------------------------------------------------------------- canvas
def compose(screen_name, subtitle, bullets, render, fonts, data, bits, out_path):
    width = SCREEN_W_DP * SCALE
    screen_h = SCREEN_H_DP * SCALE
    top_band = 150
    bottom_band = 60 + 46 * len(bullets) + 90
    total_h = top_band + screen_h + bottom_band

    canvas = Image.new("RGBA", (width, total_h), (247, 247, 248, 255))
    draw = ImageDraw.Draw(canvas)

    title_font = fonts.get("bold", 42)
    sub_font = fonts.get("regular", 26)
    bullet_font = fonts.get("regular", 27)

    # app name in the mono face so the capital I in "WaImart" is unambiguous
    name_font = fonts.get("mono_bold", 40)
    draw.text((40, 48), "WaImart", font=name_font, fill=CAPTION_INK)
    draw.text((40 + name_font.getlength("WaImart") + 16, 48), "\u2014  " + screen_name,
              font=title_font, fill=CAPTION_INK)
    draw.text((40, 104), subtitle, font=sub_font, fill=SUBTITLE_INK)

    screen = Image.new("RGBA", (width, screen_h), SCREEN_BG)
    render(screen)
    canvas.paste(screen, (0, top_band))

    y = top_band + screen_h + 30
    draw.text((40, y), "What this mockup shows", font=fonts.get("bold", 28), fill=CAPTION_INK)
    y += 44
    for bullet in bullets:
        draw.text((44, y), "\u2022  " + bullet, font=bullet_font, fill=(60, 60, 60, 255))
        y += 46
    draw.text((40, y + 6), "Mockup rendered from the app's layout constants \u2014 not a device screenshot.",
              font=fonts.get("regular", 24), fill=(140, 140, 140, 255))

    canvas.convert("RGB").save(out_path, "PNG")
    print("wrote", out_path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--outdir", default="build/mockups")
    parser.add_argument("--data", default="/tmp/mockdata.txt")
    parser.add_argument("--bits", default="/tmp/bits.txt")
    parser.add_argument("--fonts", default="/tmp/fonts")
    args = parser.parse_args()

    os.makedirs(args.outdir, exist_ok=True)
    fonts = Fonts(args.fonts)
    data = parse_data(args.data)
    bits = open(args.bits, "r", encoding="utf-8").read().strip().splitlines()[0]

    compose(
        "Receipt Detail",
        "Receipt screen: WaImart lockup, editable receipt, real Code 128 barcode.",
        [
            "Pinch out to zoom in, pinch closed to zoom out.",
            "Two-finger press and drag moves the entire receipt.",
            "Tap a highlighted field to edit it (store block, ST#/OP# line, item, UPC, tax rate).",
            "Tap the barcode (or the TC# line) to open the item editor.",
        ],
        lambda screen: render_receipt(screen, fonts, data, bits),
        fonts, data, bits,
        os.path.join(args.outdir, "receipt-detail.png"),
    )

    compose(
        "Item editor",
        "Opened from the barcode: Save / Duplicate / Undo / Redo over the black editor.",
        [
            "Opens with your draft restored, or the batch entered before the last save.",
            "One item per line as description$price; ## lines are ignored.",
            "Save appends the items, recalculates tax/total/ITEMS SOLD and returns to the receipt.",
            "Draft autosaves, so leaving the screen or the app never loses your list.",
        ],
        lambda screen: render_editor(screen, fonts, data),
        fonts, data, bits,
        os.path.join(args.outdir, "item-editor.png"),
    )


if __name__ == "__main__":
    main()
