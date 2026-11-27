# Toasty Travel Tools — design tokens

Source: Figma "Toasty-mockups" (file `T0VZb4FeT9IhNekwPlQRUz`), page 1.
Frames sampled: 01 Your campsite collection (list/cards, bottom nav), 02 Capture the
essentials (form/inputs), 18 Reject with a clear reason (detail record, destructive
confirmation, warning banner), 19 Reach the right entrants (selectable rows, checkbox),
25 Message your guest (chat bubbles, composer).

The file defines **no Figma variables** (`get_variable_defs` returned `{}`); every value
below was read from raw fills/strokes/text styles and is consistent across all frames.
Figma has **no dark mode**; the dark scheme is derived.

## Palette → Material 3 roles (light)

| Figma hex | Where it appears in Figma | M3 role(s) |
|---|---|---|
| `#254D3D` forest green | primary buttons, send button, owner chat bubble, checkbox, links, active nav | `primary` |
| `#FFFFFF` | text on primary / error buttons | `onPrimary`, `onSecondary`, `onError` |
| `#E8EEE3` sage | status chip fill, guidance box, selected row, outer canvas | `primaryContainer`, `surfaceVariant`, `outlineVariant` |
| `#254D3D` | text on sage | `onPrimaryContainer` |
| `#A66334` clay | section eyebrows, annotation rule, warning text | `secondary`, `onSecondaryContainer` |
| `#F6E9D9` sand | warning/info banner fill | `secondaryContainer` |
| `#17392D` deep forest | page and card titles | `tertiary` |
| `#F7F5ED` cream | screen background | `background`, `surface` |
| `#FFFEF9` paper | cards, inputs, bottom nav, guest chat bubble | `surfaceContainerLowest…Highest` |
| `#25372E` ink | body text, field labels | `onBackground`, `onSurface` |
| `#69756C` moss grey | secondary text, helpers, inactive nav | `onSurfaceVariant` |
| `#DCE1D7` hairline | 1dp borders on cards, inputs, secondary buttons, dividers | `outline` |
| `#A14036` brick | destructive button, destructive border, reject link | `error` |
| `#F6E0DC` (derived) | — | `errorContainer` |

Dark scheme is derived from the same hues (forest/sage/clay/brick) and is listed in
`view/theme/Color.kt`.

## Type scale — Lato

Figma uses **Lato** (Regular, Bold, ExtraBold). Lato is bundled as TTFs under
`res/font/` (OFL, from github.com/google/fonts). Google's Lato release has no
ExtraBold (800) cut, so **ExtraBold is mapped to Lato Black (900)**. Line height
1.35 in Figma → rounded sp below. Letter spacing is 0 everywhere in Figma.

| M3 style | Size / line | Weight | Figma usage |
|---|---|---|---|
| headlineLarge | 28 / 32 | Black (≈ExtraBold) | page title ("My campsites") |
| headlineMedium | 24 / 30 | Black | — (interpolated) |
| headlineSmall | 22 / 28 | Black | section title |
| titleLarge | 20 / 27 | Bold | record name, confirmation title, TopAppBar title |
| titleMedium | 16 / 22 | Bold | list row name |
| titleSmall | 14 / 19 | Bold | card sub-heading |
| bodyLarge | 15 / 20 | Regular | summary text, text-field input |
| bodyMedium | 14 / 19 | Regular | chat text, body |
| bodySmall | 12 / 16 | Regular | helper and footnote text |
| labelLarge | 15 / 20 | Bold | button labels |
| labelMedium | 12 / 16 | Bold | field labels, inline links |
| labelSmall | 11 / 14 | Bold | status chip, eyebrow, nav label |

Display styles keep M3 sizes in Lato Black (unused by Figma).

## Shapes (corner radii)

| Token | Radius | Figma usage | M3 consumers |
|---|---|---|---|
| extraSmall | 10dp | inputs | OutlinedTextField, menus, snackbar |
| small | 10dp | buttons, guidance boxes, send button | used explicitly for Button/OutlinedButton |
| medium | 14dp | cards, records | Card |
| large | 16dp | confirmation panel | sheets, nav drawer |
| extraLarge | 20dp | — (Figma has no modal; slightly above `large` to read as modal) | AlertDialog, DatePickerDialog |

Other Figma radii: status chip 6dp, checkbox 5dp, chat bubble 14dp with a 4dp "tail" corner.

## Spacing

The Figma scale is 4-based: **4, 6, 8, 10, 12, 14, 16, 20, 24**.
- Screen horizontal padding: 20dp
- Vertical gap between stacked blocks: 12dp
- Card inner padding: 16dp (row items: 14dp)
- Field label → input gap: 6dp
- Bottom action area: 10dp top, 16dp bottom

## Elevation

Flat. No shadows anywhere in Figma: separation comes from 1dp `#DCE1D7` borders and
paper-on-cream fills. Cards use 0dp elevation plus a 1dp `outline` border.

## Components

- **Button (primary)**: fill `primary`, label white Lato Bold 15, height 48, radius 10, full-width at the bottom of the page.
- **Button (secondary)**: fill paper `#FFFEF9`, 1dp `outline` border, label `primary`, radius 10 → `OutlinedButton`.
- **Button (destructive)**: fill `error`, white label, radius 10.
- **Text link**: Bold 12–13, `primary` (or `error` for destructive), trailing "→" → `TextButton`.
- **Card**: paper fill, 1dp `outline` border, radius 14, padding 16, no elevation.
- **TextField**: paper fill, 1dp `outline` border, radius 10, min height 46, padding 12, input text 14 `onSurface`; label Bold 12 above; helper Regular 12 `onSurfaceVariant` below.
- **Top bar**: no coloured bar. Cream background, small Bold 11 back/eyebrow row, then a 28 Black title → TopAppBar on `surface`, title in `titleLarge`.
- **List row (selectable)**: radius 12, padding 14, selected = sage fill, unselected = paper fill; checkbox 22dp, radius 5, `primary`.
- **Status chip**: sage fill, `primary` Bold 11, radius 6, padding 9×5.
- **Guidance banner**: sage fill + `primary` text (info) or sand fill + clay text (warning), radius 10, padding 12, 17dp leading icon.
- **Dialog / confirmation**: paper panel, radius 16, padding 18, 1dp border (`error` for destructive).
- **Chat bubbles**: guest = paper + border + ink text; self = `primary` fill + white text; 14dp corners with 4dp tail; meta line Regular 11 `onSurfaceVariant`.
- **Bottom navigation**: paper, 1dp top border, 76dp, active item = `primary` Black label + 16×2 indicator.
