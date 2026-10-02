#!/usr/bin/env python3
"""Compose store listing cards from raw app screenshots.

The card borrows its grammar from the app itself rather than from any other
listing: the small orange tag the Pro screens use, a left-set screen title
with the rule the plan screen draws under its heading, and the wordmark in the
corner. The phone sits off-centre, leaning the way the wordmark's arrow does.
Rendered with rsvg-convert using the fonts shipped inside the Android app.
"""
import base64, os, subprocess, sys, struct

HERE = os.path.dirname(os.path.abspath(__file__))
FONTS_CONF = os.path.join(HERE, "fonts.conf")
STORE = os.environ.get("TRAINR_STORE_SOURCE", "")
OUT = os.path.dirname(HERE)

DARK = "#101519"
PANEL = "#20282E"
RIM = "#3A454D"
ORANGE = "#D37200"
ORANGE_ON_DARK = "#E8963A"
WHITE = "#F5F7F8"
INK_ON_ORANGE = "#101519"


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    assert head[:8] == b"\x89PNG\r\n\x1a\n", path
    return struct.unpack(">II", head[16:24])


def data_uri(path):
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;")


def fit(W, lines, base, margin):
    # Fugaz One caps run about 0.64em wide; keep the longest line inside the margins.
    longest = max(len(text) for text in lines)
    return min(base, (W - 2 * margin) / (longest * 0.64))


def wordmark(x, y, width, mark):
    ww, wh = png_size(mark)
    return f'<image x="{x:.1f}" y="{y:.1f}" width="{width:.1f}" height="{width * wh / ww:.1f}" href="{data_uri(mark)}"/>'


def tag(x, y, text, size):
    pad = size * 0.55
    w = len(text) * size * 0.74 + pad * 2
    h = size * 1.6
    return (
        f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{size*0.35:.1f}" fill="{ORANGE_ON_DARK}"/>'
        f'<text x="{x + pad:.1f}" y="{y + h*0.72:.1f}" font-family="Rubik Light" font-weight="bold" font-size="{size:.1f}" '
        f'fill="{INK_ON_ORANGE}" letter-spacing="{size*0.06:.1f}">{esc(text)}</text>'
    )


def title(x, y, lines, size):
    out = []
    for i, text in enumerate(lines):
        out.append(
            f'<text x="{x:.1f}" y="{y + i*size*1.06:.1f}" font-family="Fugaz One" font-size="{size:.1f}" '
            f'fill="{WHITE}">{esc(text)}</text>'
        )
    rule_y = y + (len(lines) - 1) * size * 1.06 + size * 0.42
    out.append(f'<rect x="{x:.1f}" y="{rule_y:.1f}" width="{size*1.9:.1f}" height="{size*0.09:.1f}" fill="{ORANGE_ON_DARK}"/>')
    return "\n".join(out), rule_y


def phone(shot, x, y, width, tilt, notch, clip_id="screen"):
    sw, sh = png_size(shot)
    screen_h = width * sh / sw
    bezel = width * 0.035
    fw, fh = width + bezel * 2, screen_h + bezel * 2
    r_frame = width * 0.16
    r_screen = r_frame - bezel * 0.9
    cx, cy = x + fw / 2, y + fh / 2
    parts = [
        f'<g transform="rotate({tilt} {cx:.1f} {cy:.1f})">',
        f'<rect x="{x - 3:.1f}" y="{y - 3:.1f}" width="{fw + 6:.1f}" height="{fh + 6:.1f}" rx="{r_frame + 3:.1f}" fill="{RIM}"/>',
        f'<rect x="{x:.1f}" y="{y:.1f}" width="{fw:.1f}" height="{fh:.1f}" rx="{r_frame:.1f}" fill="#0B0E11"/>',
        f'<clipPath id="{clip_id}"><rect x="{x + bezel:.1f}" y="{y + bezel:.1f}" width="{width:.1f}" height="{screen_h:.1f}" rx="{r_screen:.1f}"/></clipPath>',
        f'<image clip-path="url(#{clip_id})" x="{x + bezel:.1f}" y="{y + bezel:.1f}" width="{width:.1f}" height="{screen_h:.1f}" '
        f'preserveAspectRatio="none" href="{data_uri(shot)}"/>',
    ]
    if notch:
        pw, ph = width * 0.28, bezel * 1.9
        parts.append(f'<rect x="{cx - pw/2:.1f}" y="{y + bezel * 1.25:.1f}" width="{pw:.1f}" height="{ph:.1f}" rx="{ph/2:.1f}" fill="#0B0E11"/>')
    parts.append("</g>")
    return "\n".join(parts)


def card(W, H, shot, label, lines, notch, mark):
    m = W * 0.08
    size = fit(W, lines, W * 0.105, m)
    body, rule_y = title(m, H * 0.215, lines, size)
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
        f'<rect width="{W}" height="{H}" fill="{DARK}"/>',
        f'<rect x="{-W*0.1:.1f}" y="{H*0.44:.1f}" width="{W*1.2:.1f}" height="{H:.1f}" rx="{W*0.12:.1f}" fill="{PANEL}" transform="rotate(-4 {W/2:.1f} {H*0.44:.1f})"/>',
        wordmark(m, H * 0.055, W * 0.30, mark),
        tag(m, H * 0.135, label, W * 0.030),
        body,
        phone(shot, x=W * 0.16, y=rule_y + H * 0.055, width=W * 0.80, tilt=-4, notch=notch),
        "</svg>",
    ]
    return "\n".join(svg)


def closing_card(W, H, mark, lines, footer):
    m = W * 0.08
    ww, wh = png_size(mark)
    mark_w = W * 0.74
    mark_h = mark_w * wh / ww
    y_mark = H * 0.30
    size = fit(W, lines, W * 0.10, m)
    body, rule_y = title(m, y_mark + mark_h * 0.66 + W * 0.17, lines, size)
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
        f'<rect width="{W}" height="{H}" fill="{DARK}"/>',
        f'<rect x="{-W*0.1:.1f}" y="{H*0.70:.1f}" width="{W*1.2:.1f}" height="{H:.1f}" rx="{W*0.12:.1f}" fill="{ORANGE}" transform="rotate(-4 {W/2:.1f} {H*0.70:.1f})"/>',
        f'<image x="{m:.1f}" y="{y_mark:.1f}" width="{mark_w:.1f}" height="{mark_h:.1f}" href="{data_uri(mark)}"/>',
        body,
    ]
    fy = H * 0.82
    for i, line in enumerate(footer):
        svg.append(
            f'<text x="{m:.1f}" y="{fy + i * W*0.065:.1f}" font-family="Rubik Light" font-weight="bold" font-size="{W*0.048:.1f}" fill="{INK_ON_ORANGE}">{esc(line)}</text>'
        )
    svg.append("</svg>")
    return "\n".join(svg)


def feature_graphic(W, H, mark, shot):
    m = W * 0.06
    size = H * 0.13
    body, _ = title(m, H * 0.63, ["A WEEK BUILT", "AROUND YOU"], size)
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
        f'<rect width="{W}" height="{H}" fill="{DARK}"/>',
        f'<rect x="{W*0.58:.1f}" y="{-H*0.2:.1f}" width="{W*0.7:.1f}" height="{H*1.4:.1f}" rx="{H*0.12:.1f}" fill="{PANEL}" transform="rotate(-8 {W*0.58:.1f} {H/2:.1f})"/>',
        wordmark(m, H * 0.06, W * 0.26, mark),
        tag(m, H * 0.35, "WORKOUT PLANNER", H * 0.05),
        body,
        phone(shot, x=W * 0.70, y=H * 0.10, width=W * 0.23, tilt=-8, notch=False, clip_id="fg"),
        "</svg>",
    ]
    return "\n".join(svg)


def render(svg, out_png, W, H):
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    tmp = out_png + ".svg"
    with open(tmp, "w") as f:
        f.write(svg)
    env = dict(os.environ, FONTCONFIG_FILE=FONTS_CONF, PANGOCAIRO_BACKEND="fontconfig")
    subprocess.run(["rsvg-convert", "-w", str(W), "-h", str(H), "-o", out_png, tmp], check=True, env=env)
    os.remove(tmp)


# file stem, tag, headline lines
CARDS = [
    ("01-welcome",  "POCKET TRAINER",  ["YOUR WEEK,", "WRITTEN FOR YOU"]),
    ("02-setup",    "YOUR SETUP",      ["TELL IT WHAT", "YOU TRAIN WITH"]),
    ("03-plan",     "WEEKLY PLAN",     ["A WEEK BUILT", "AROUND YOU"]),
    ("04-day",      "EVERY SESSION",   ["LOG EVERY SET.", "KNOW EVERY MOVE."]),
    ("05-progress", "PROGRESS",        ["WATCH THE WEEK", "FILL IN"]),
]
CLOSING = ["NO ACCOUNT.", "NO SIGN-UP."]
FOOTER = ["Built around your goals.", "Progress from what you lift."]

SETS = {
    "app-store": (os.path.join(STORE, "app-store/screenshots/6.9-inch"), True,
                  [(1320, 2868, "6.9-inch"), (1284, 2778, "6.5-inch")]),
    "play": (os.path.join(STORE, "play/screenshots"), False,
             [(1080, 1920, "phone"), (1440, 2560, "tablet-7-inch"), (1800, 3200, "tablet-10-inch")]),
}


def main(only=None):
    mark = os.path.join(HERE, "wordmark-dark.png")
    for store, (raw, notch, sizes) in SETS.items():
        if only == "closing" and store != "app-store":
            continue
        for W, H, sub in sizes:
            if only and only not in (store, sub, "closing"):
                continue
            for name, label, lines in ([] if only == "closing" else CARDS):
                out = os.path.join(OUT, store, sub, f"{name}.png")
                render(card(W, H, os.path.join(raw, f"{name}.png"), label, lines, notch, mark), out, W, H)
                print("wrote", os.path.relpath(out, STORE))
            out = os.path.join(OUT, store, sub, "06-trainr.png")
            render(closing_card(W, H, mark, CLOSING, FOOTER), out, W, H)
            print("wrote", os.path.relpath(out, STORE))
    if not only or only == "play":
        out = os.path.join(OUT, "play", "feature-graphic-1024x500.png")
        render(feature_graphic(1024, 500, mark, os.path.join(SETS["play"][0], "03-plan.png")), out, 1024, 500)
        print("wrote", os.path.relpath(out, STORE))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
