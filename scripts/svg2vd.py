#!/usr/bin/env python3
"""SVG -> Android VectorDrawable for the equipment icon subset:
rect/circle/ellipse/path, linear+radial gradients (objectBoundingBox or userSpaceOnUse),
fill/stroke with url(#id), opacity, stroke-width, one rotate() transform on ellipses."""
import math, re, sys, xml.etree.ElementTree as ET
from pathlib import Path

NS = "{http://www.w3.org/2000/svg}"
def tag(e): return e.tag.replace(NS, "")

def rect_path(a):
    x, y, w, h = (float(a.get(k, 0)) for k in ("x", "y", "width", "height"))
    r = float(a.get("rx", a.get("ry", 0)))
    if r == 0: return f"M{x},{y}h{w}v{h}h{-w}z"
    return (f"M{x+r},{y}h{w-2*r}a{r},{r} 0 0 1 {r},{r}v{h-2*r}a{r},{r} 0 0 1 {-r},{r}"
            f"h{-(w-2*r)}a{r},{r} 0 0 1 {-r},{-r}v{-(h-2*r)}a{r},{r} 0 0 1 {r},{-r}z")

def ellipse_path(cx, cy, rx, ry, rot=0.0):
    # four arcs; rotation handled by rotating the start point and arc x-axis-rotation
    pts = []
    for ang in (0, 90, 180, 270):
        t = math.radians(ang); px, py = rx*math.cos(t), ry*math.sin(t)
        r = math.radians(rot); qx, qy = px*math.cos(r)-py*math.sin(r), px*math.sin(r)+py*math.cos(r)
        pts.append((cx+qx, cy+qy))
    d = f"M{pts[0][0]:.3f},{pts[0][1]:.3f}"
    for px, py in pts[1:] + pts[:1]:
        d += f"A{rx},{ry} {rot} 0 1 {px:.3f},{py:.3f}"
    return d + "z"

def path_bbox(d):
    """Bounding box from the on-curve points and control points of a path.
    Arc radii, rotation and flags are skipped, which is what the naive number
    scan got wrong. Slightly generous on curves; fine for placing a gradient."""
    toks = re.findall(r"[MmLlHhVvCcSsQqTtAaZz]|-?\d*\.?\d+(?:e-?\d+)?", d)
    xs, ys = [], []; cx = cy = sx = sy = 0.0; cmd = None; i = 0
    def take(n):
        nonlocal i; vals = [float(v) for v in toks[i:i+n]]; i += n; return vals
    while i < len(toks):
        if re.match(r"[A-Za-z]", toks[i]): cmd = toks[i]; i += 1
        rel = cmd.islower(); c = cmd.upper()
        if c == "Z": cx, cy = sx, sy; continue
        if c == "H": (x,) = take(1); cx = cx + x if rel else x
        elif c == "V": (y,) = take(1); cy = cy + y if rel else y
        elif c in "ML T":
            x, y = take(2); cx, cy = (cx + x, cy + y) if rel else (x, y)
            if c == "M": sx, sy = cx, cy
        elif c == "C":
            x1, y1, x2, y2, x, y = take(6)
            pts = [(x1, y1), (x2, y2), (x, y)]
            if rel: pts = [(cx + px, cy + py) for px, py in pts]
            for px, py in pts[:2]: xs.append(px); ys.append(py)
            cx, cy = pts[2]
        elif c in "SQ":
            x1, y1, x, y = take(4)
            pts = [(x1, y1), (x, y)]
            if rel: pts = [(cx + px, cy + py) for px, py in pts]
            xs.append(pts[0][0]); ys.append(pts[0][1]); cx, cy = pts[1]
        elif c == "A":
            _rx, _ry, _rot, _f1, _f2, x, y = take(7); cx, cy = (cx + x, cy + y) if rel else (x, y)
        else: raise SystemExit(f"unsupported path command {cmd}")
        xs.append(cx); ys.append(cy)
    return (min(xs), min(ys), max(max(xs) - min(xs), 0.5), max(max(ys) - min(ys), 0.5))

def hexcol(c, opacity=1.0):
    c = c.strip()
    if c == "#fff": c = "#ffffff"
    if c == "#000": c = "#000000"
    if opacity >= 0.999: return c.upper()
    a = max(0, min(255, round(opacity*255)))
    return f"#{a:02X}{c[1:].upper()}"

def stops(g):
    out = []
    for s in g:
        if tag(s) != "stop": continue
        off = s.get("offset", "0"); off = float(off[:-1])/100 if off.endswith("%") else float(off)
        out.append((off, s.get("stop-color")))
    return out

def gradient_xml(g, bbox, attr, opacity):
    kind = "linear" if tag(g) == "linearGradient" else "radial"
    units = g.get("gradientUnits", "objectBoundingBox")
    def px(v, lo, span): return float(v) if units == "userSpaceOnUse" else lo + (float(v[:-1])/100 if str(v).endswith("%") else float(v))*span
    bx, by, bw, bh = bbox
    s = "".join(f'      <item android:offset="{o}" android:color="{hexcol(c, opacity)}"/>\n' for o, c in stops(g))
    if kind == "linear":
        x1 = px(g.get("x1", "0"), bx, bw); y1 = px(g.get("y1", "0"), by, bh)
        x2 = px(g.get("x2", "1"), bx, bw); y2 = px(g.get("y2", "0"), by, bh)
        head = f'<gradient android:type="linear" android:startX="{x1:.3f}" android:startY="{y1:.3f}" android:endX="{x2:.3f}" android:endY="{y2:.3f}">'
    else:
        cx = px(g.get("cx", "50%"), bx, bw); cy = px(g.get("cy", "50%"), by, bh)
        r = float(g.get("r", "50%")[:-1])/100 * max(bw, bh) if units != "userSpaceOnUse" else float(g.get("r"))
        head = f'<gradient android:type="radial" android:centerX="{cx:.3f}" android:centerY="{cy:.3f}" android:gradientRadius="{r:.3f}">'
    return f'    <aapt:attr name="android:{attr}">\n      {head}\n{s}      </gradient>\n    </aapt:attr>\n'

def convert(src: Path) -> str:
    root = ET.parse(src).getroot()
    grads = {g.get("id"): g for d in root.iter(f"{NS}defs") for g in d}
    vb = [float(v) for v in root.get("viewBox").split()]
    out = ['<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    xmlns:aapt="http://schemas.android.com/aapt"',
           f'    android:width="{int(vb[2])}dp" android:height="{int(vb[3])}dp"',
           f'    android:viewportWidth="{vb[2]:g}" android:viewportHeight="{vb[3]:g}">']
    for e in root:
        t = tag(e)
        if t == "defs": continue
        a = e.attrib; sw = a.get("stroke-width"); op = float(a.get("opacity", 1))
        if t == "rect":
            d = rect_path(a); x, y, w, h = (float(a.get(k, 0)) for k in ("x", "y", "width", "height")); bbox = (x, y, w, h)
        elif t == "circle":
            cx, cy, r = (float(a[k]) for k in ("cx", "cy", "r")); d = ellipse_path(cx, cy, r, r); bbox = (cx-r, cy-r, 2*r, 2*r)
        elif t == "ellipse":
            cx, cy, rx, ry = (float(a[k]) for k in ("cx", "cy", "rx", "ry")); rot = 0.0
            m = re.match(r"rotate\(([-\d.]+)", a.get("transform", "")); rot = float(m.group(1)) if m else 0.0
            d = ellipse_path(cx, cy, rx, ry, rot); bbox = (cx-rx, cy-ry, 2*rx, 2*ry)
        elif t == "path":
            d = a["d"]; bbox = path_bbox(d)
        else:
            raise SystemExit(f"{src.name}: unsupported element <{t}>")
        attrs = [f'android:pathData="{d}"']; children = ""
        for key, attr in (("fill", "fillColor"), ("stroke", "strokeColor")):
            v = a.get(key)
            if not v or v == "none": continue
            m = re.match(r"url\(#(.+)\)", v)
            if m: children += gradient_xml(grads[m.group(1)], bbox, attr, op)
            else: attrs.append(f'android:{attr}="{hexcol(v, op)}"')
        if sw: attrs.append(f'android:strokeWidth="{sw}"')
        if a.get("stroke") and a.get("stroke") != "none":
            attrs.append('android:strokeLineCap="round" android:strokeLineJoin="round"')
        if children: out.append(f'  <path {" ".join(attrs)}>\n{children}  </path>')
        else: out.append(f'  <path {" ".join(attrs)}/>')
    out.append("</vector>")
    return "\n".join(out) + "\n"

if __name__ == "__main__":
    src_dir, dst_dir, prefix = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3]
    dst_dir.mkdir(parents=True, exist_ok=True)
    for f in sorted(src_dir.glob("*.svg")):
        (dst_dir / f"{prefix}{f.stem}.xml").write_text(convert(f)); print("wrote", dst_dir / f"{prefix}{f.stem}.xml")
