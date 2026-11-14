# Equipment icons

The nine thumbnails on the equipment choice, one per `Equipment` value, in the
catalog's order. Rendered vectors in the app's own tokens: brand `#D37200` on
slate `#243036` in light, `#E8963A` on `#E8EDEF` in dark, with gradients for
volume and a contact shadow. Approved 14 November 2026.

- `svg/`, `svg-dark/` — the masters. Edit these.
- `pdf/`, `pdf-dark/` — what the iOS asset catalog holds, rendered with
  `rsvg-convert -f pdf`.
- `app/src/main/res/drawable{,-night}/ic_equipment_*.xml` — what Android
  holds, produced by `scripts/svg2vd.py`.

To change one: edit the SVG, then

```sh
python3 scripts/svg2vd.py design/equipment-icons/svg      app/src/main/res/drawable       ic_equipment_
python3 scripts/svg2vd.py design/equipment-icons/svg-dark app/src/main/res/drawable-night ic_equipment_
for f in design/equipment-icons/svg/*.svg; do rsvg-convert -f pdf -o "design/equipment-icons/pdf/$(basename "${f%.svg}").pdf" "$f"; done
```

and copy the PDFs into the matching `Equipment*.imageset` in the iOS repo.

The converter handles the subset these files use: rect, circle, ellipse, path,
linear and radial gradients, opacity, one `rotate()` on an ellipse. Two traps
it exists to avoid: a horizontal line has a zero-height bounding box, so a
vertical gradient on it renders nothing (bars are rects for that reason), and
`aapt` wants gradients in user space, so the converter resolves
`objectBoundingBox` against a real path bounding box.
