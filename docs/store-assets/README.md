# App Review screenshot correction — October 2026

Replace screenshot 06 in the English (U.S.) iPhone 6.9-inch and 6.5-inch sets
with the matching `app-store/<size>/06-trainr.png`. The original art and typography
are preserved. The footer now describes the feature without any pricing claim.
Check inherited device sizes and other localizations in Media Manager before submission.

Rebuild the two closing cards with `python3 docs/store-assets/tools/make_cards.py closing`.
Requires `rsvg-convert` (librsvg), fontconfig and the fonts already in this repository.
The generator composes SVG artwork; it does not alter the app screenshots.
For full sets, set `TRAINR_STORE_SOURCE` to a store asset directory containing
`app-store/screenshots/6.9-inch` and run with `app-store`. Raw screenshots are
not included here. The first five original cards contain no pricing claims.
