#!/usr/bin/env python3
"""Generate Play Store icon variants and feature graphic for Dial It Back."""

import re, subprocess, pathlib

OUT = pathlib.Path(__file__).parent
PHONE_SVG = OUT / "publicdomainq-oldphone.svg"

# Phone SVG original viewBox dimensions
PHONE_W, PHONE_H = 732, 524

SCHEMES = {
    "sky":      {"bg": "#B8D9F0", "phone": "#1A2244", "dial": "#E8F5FF", "label": "Sky Blue"},
    "vintage":  {"bg": "#F5EDDA", "phone": "#3A1800", "dial": "#FFF8F0", "label": "Vintage Cream"},
    "garden":   {"bg": "#C4D9CA", "phone": "#162B1E", "dial": "#EAF4EC", "label": "Garden Green"},
    "lavender": {"bg": "#E4D5EA", "phone": "#2A0D35", "dial": "#F7EFF9", "label": "Soft Lavender"},
    "sky-warm": {"bg": "#B8D9F0", "phone": "#3A1800", "dial": "#E8F5FF", "label": "Sky + Warm Brown"},
}

# ── Helpers ────────────────────────────────────────────────────────────────────

def extract_inner(svg_text):
    """Strip the outer <svg> wrapper and return only the inner elements."""
    inner = re.sub(r'<\?xml[^>]+\?>', '', svg_text)
    inner = re.sub(r'<!DOCTYPE[^>]+>', '', inner)
    m = re.search(r'<svg[^>]*>(.*)</svg>', inner, re.DOTALL)
    return m.group(1).strip() if m else inner


def recolour(inner, phone_col, dial_col):
    """Swap original hardcoded colours for scheme colours."""
    # Phone body / holes / text — dark near-black
    inner = re.sub(r'#1A1919', phone_col, inner, flags=re.IGNORECASE)
    # Dial face — near-white
    inner = re.sub(r'#FFFFFE', dial_col, inner, flags=re.IGNORECASE)
    return inner


def render(svg_str, out_path, w, h):
    svg_path = out_path.with_suffix(".svg")
    svg_path.write_text(svg_str)
    subprocess.run(
        ["rsvg-convert", "-w", str(w), "-h", str(h), "-o", str(out_path), str(svg_path)],
        check=True,
    )
    print(f"  ✓  {out_path.name}  ({out_path.stat().st_size // 1024} KB)")


# ── Icon (512×512) ─────────────────────────────────────────────────────────────
# Fit phone with 44px padding on each side, centred vertically.

def icon_svg(scheme, inner_raw):
    bg, phone, dial = scheme["bg"], scheme["phone"], scheme["dial"]
    inner = recolour(inner_raw, phone, dial)

    pad = 44
    avail_w = 512 - pad * 2
    scale = avail_w / PHONE_W                       # ≈ 0.581
    scaled_h = PHONE_H * scale
    tx = pad
    ty = (512 - scaled_h) / 2

    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <rect width="512" height="512" fill="{bg}"/>
  <g transform="translate({tx:.1f},{ty:.1f}) scale({scale:.4f})">
    {inner}
  </g>
</svg>"""


# ── Feature graphic (1024×500) ─────────────────────────────────────────────────
# Phone in left ~38% of width, well-padded. Text block right of centre line.

def feature_svg(scheme, inner_raw):
    bg, phone, dial = scheme["bg"], scheme["phone"], scheme["dial"]
    inner = recolour(inner_raw, phone, dial)

    # Phone: fit into a 350×(500-80) box, left-aligned with 35px left margin
    ph_pad_v = 60
    avail_h = 500 - ph_pad_v * 2
    scale = min(380 / PHONE_W, avail_h / PHONE_H)   # fit by whichever axis is tighter
    scaled_w = PHONE_W * scale
    scaled_h = PHONE_H * scale
    tx = 30
    ty = (500 - scaled_h) / 2

    # Text: left edge well clear of phone, centre of remaining space
    text_left = tx + scaled_w + 40
    text_cx = (text_left + 990) / 2

    # Vertically centre the two-line text block (approx total text height ~120px)
    name_y   = 500 / 2 - 20
    tag_y    = name_y + 68

    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 500"
     font-family="Helvetica Neue, Helvetica, Arial, sans-serif">
  <rect width="1024" height="500" fill="{bg}"/>

  <g transform="translate({tx:.1f},{ty:.1f}) scale({scale:.4f})">
    {inner}
  </g>

  <text x="{text_cx:.0f}" y="{name_y:.0f}" text-anchor="middle"
        font-size="80" font-weight="800" letter-spacing="-1.5"
        fill="{phone}">Dial It Back</text>

  <text x="{text_cx:.0f}" y="{tag_y:.0f}" text-anchor="middle"
        font-size="27" font-weight="400" letter-spacing="0.5"
        fill="{phone}" opacity="0.6">Simple. Focused. Just what you need.</text>
</svg>"""


# ── Generate all ───────────────────────────────────────────────────────────────

inner_raw = extract_inner(PHONE_SVG.read_text())

print("\nIcons (512×512)…")
for name, scheme in SCHEMES.items():
    render(icon_svg(scheme, inner_raw), OUT / f"icon-{name}.png", 512, 512)

print("\nFeature graphics (1024×500)…")
for name, scheme in SCHEMES.items():
    render(feature_svg(scheme, inner_raw), OUT / f"feature-{name}.png", 1024, 500)

print(f"\nDone — files in {OUT}")
