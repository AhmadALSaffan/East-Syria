---
name: East Syria
description: A dark, warm-toned Android catalog of Syrian landmarks — lit artifacts on basalt.
colors:
  clay: "#D17A5A"
  amber: "#F5A623"
  teal: "#009688"
  sand: "#D4B896"
  basalt: "#1A1A1A"
  basalt-raised: "#2A2A2A"
  basalt-high: "#3A3A3A"
  bone: "#FFFFFF"
  bone-dim: "#B0B0B0"
  bone-hint: "#949494"
  warm-gray: "#B8A896"
  ash: "#808080"
  hairline: "#333333"
  published: "#4CAF50"
  pending: "#FFA726"
  canvas: "#16120F"
  surface: "#221C17"
  surface-high: "#2E2620"
  line: "#362E27"
  ivory: "#F5EDE3"
  ivory-dim: "#B5A796"
  ivory-mute: "#9A8D7E"
  terracotta: "#E2703A"
  terracotta-soft: "#F09A6E"
  gold: "#F0B44C"
  wayfinding: "#2BA89A"
typography:
  display:
    fontFamily: "serif"
    fontSize: "56sp"
    fontWeight: 700
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "serif"
    fontSize: "36sp"
    fontWeight: 700
    letterSpacing: "normal"
  title:
    fontFamily: "sans-serif"
    fontSize: "24sp"
    fontWeight: 700
    letterSpacing: "normal"
  title-small:
    fontFamily: "sans-serif"
    fontSize: "18sp"
    fontWeight: 700
    letterSpacing: "normal"
  body:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: 1.4
    letterSpacing: "normal"
  label:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    letterSpacing: "normal"
  label-small:
    fontFamily: "sans-serif"
    fontSize: "10sp"
    fontWeight: 700
    letterSpacing: "0.05em"
rounded:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  full: "999dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  xxl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.clay}"
    textColor: "{colors.basalt}"
    rounded: "{rounded.md}"
    padding: "0 24dp"
    height: "56dp"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.clay}"
    rounded: "{rounded.md}"
    padding: "0 24dp"
    height: "48dp"
  button-text:
    backgroundColor: "transparent"
    textColor: "{colors.bone-dim}"
    rounded: "{rounded.sm}"
    padding: "0 16dp"
    height: "40dp"
  card:
    backgroundColor: "{colors.basalt-raised}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"
  card-feature:
    backgroundColor: "{colors.basalt-raised}"
    rounded: "{rounded.xl}"
    padding: "{spacing.lg}"
    width: "280dp"
    height: "320dp"
  input-search:
    backgroundColor: "{colors.basalt-raised}"
    textColor: "{colors.bone}"
    rounded: "{rounded.md}"
    padding: "0 16dp"
    height: "56dp"
  badge-category:
    backgroundColor: "transparent"
    textColor: "{colors.clay}"
    typography: "{typography.label-small}"
    rounded: "{rounded.md}"
    padding: "6dp 12dp"
  badge-published:
    backgroundColor: "#1A4CAF50"
    textColor: "{colors.published}"
    typography: "{typography.label-small}"
    rounded: "{rounded.xs}"
    padding: "4dp 8dp"
  badge-pending:
    backgroundColor: "#1AFFA726"
    textColor: "{colors.pending}"
    typography: "{typography.label-small}"
    rounded: "{rounded.xs}"
    padding: "4dp 8dp"
  chip-category:
    backgroundColor: "{colors.basalt-raised}"
    textColor: "{colors.bone}"
    rounded: "{rounded.md}"
    padding: "0 16dp"
    height: "44dp"
  plate-featured:
    backgroundColor: "{colors.basalt}"
    textColor: "{colors.bone}"
    rounded: "{rounded.xs}"
    width: "176dp"
    height: "240dp"
  chip-filter:
    backgroundColor: "{colors.basalt-high}"
    textColor: "{colors.bone-dim}"
    rounded: "{rounded.xl}"
    padding: "0 16dp"
    height: "32dp"
  chip-filter-selected:
    backgroundColor: "{colors.clay}"
    textColor: "{colors.bone}"
    rounded: "{rounded.xl}"
    padding: "0 16dp"
    height: "32dp"
  nav-bar:
    backgroundColor: "{colors.basalt-raised}"
    textColor: "{colors.bone-dim}"
    typography: "{typography.label-small}"
  fab-map:
    backgroundColor: "{colors.teal}"
    textColor: "{colors.bone}"
    rounded: "{rounded.full}"
    size: "56dp"
---

# Design System: East Syria

## Overview

**Creative North Star: "The Museum After Hours"**

Walk into a gallery after closing and the room has gone to near-black, but the objects are still lit. The walls recede; the artifacts do not. That is exactly what this app does, literally, in code: a photograph sits on a #1A1A1A surface with nothing competing for it — no shadow, no border, no decoration — and a serif name underneath like a wall label. The interface is the dark room. The landmark is the exhibit.

The mood is **warm, grounded and reverent**. The darkness here is respect, not drama — these are heritage sites, and the design treats them as objects worth the quiet. Every colour that isn't neutral is warm and drawn from the material world the app is about: clay, amber, sand. Nothing neon, nothing cold, nothing that competes with a photograph. The one deliberate exception is a muted teal, and it only ever appears on wayfinding — the map button and the active navigation tab — so that "where am I / how do I get there" reads as a different kind of thing from "look at this."

Restraint is the operating register. Surfaces are flat and get their depth from lightness alone. Buttons prefer outline and text over fill. Colour is earned rather than issued by default: on a well-built screen, the warm accents occupy a small fraction of the pixels, and the photography carries the rest. When in doubt, this system takes something away.

**Key Characteristics:**
- Near-black basalt canvas with tonally lighter surfaces — no shadows except one sanctioned element
- Two warm accents with strictly separate jobs: clay acts, amber highlights
- Serif reserved for the app's own voice; Roboto for everything the interface says
- Photography is the content; containers recede to hold it
- 16dp is the default corner and the default gutter — the system's resting measurement

## Colors

A warm, earth-drawn palette on a neutral near-black — clay and amber over basalt, with a single cool accent held back for wayfinding.

### Primary
- **Clay** (`#D17A5A`): The action colour and the brand's voice. Filled primary buttons, outlined category badges, the OTP field's stroke, selected filter chips, "View Details" affordances, and the theme's declared `colorPrimary`. If something is interactive and coloured, it is this colour. Muted enough to sit on basalt without glare.

### Secondary
- **Amber** (`#F5A623`): The highlight colour, and *only* the highlight colour. Star ratings, the gold avatar ring, and moments where warmth is decorative rather than actionable. It is never a button, never a link, never a selected state.

### Tertiary
- **Teal** (`#009688`): Wayfinding. The docked map FAB and the checked state of the bottom navigation. The system's only cool colour, and its coolness is the signal: this is orientation, not content.
- **Sand** (`#D4B896`): A pale warm neutral used at display scale — the "Categories" screen title and the beige secondary button. Reads as limestone against the basalt.

### Neutral
- **Basalt** (`#1A1A1A`): The canvas. Every screen's root background. Named for the black volcanic stone of the Hauran; it is the wall the exhibits hang on.
- **Basalt Raised** (`#2A2A2A`): The first surface above the canvas — cards, search fields, bottom sheets, the navigation bar, unselected chips. This single step of lightness is how the system communicates "this is a container."
- **Basalt High** (`#3A3A3A`): The second step, for elements sitting *on* a raised surface — rating pills, circular icon buttons, inline chips. Rarely needed; if a screen requires a third step, the hierarchy is wrong.
- **Bone** (`#FFFFFF`): Primary text and icons on dark surfaces. Landmark names, headings, active states.
- **Bone Dim** (`#B0B0B0`): Secondary text — descriptions, supporting copy, field labels, inactive navigation labels.
- **Bone Hint** (`#949494`): Placeholder text inside fields, and nothing else. It is the lightest gray that still clears 4.5:1 against `basalt-raised`; `ash` measures 3.6:1 there and must never be used for a hint.
- **Warm Gray** (`#B8A896`): A warm-tinted secondary text tone for places where neutral gray reads too clinical next to clay.
- **Ash** (`#808080`): Tertiary and meta text — letterspaced eyebrow labels, provenance lines, timestamps.
- **Hairline** (`#333333`): Dividers and separators. Never used as a fill.

### Semantic
- **Published** (`#4CAF50`) and **Pending** (`#FFA726`): Admin catalog status only. Both render through the tinted-badge pattern described in Components, never as flat fills.

### Named Rules

**The Two Warmths Rule.** Clay acts; amber highlights. Clay is the only warm colour permitted on an interactive control. Amber is the only warm colour permitted on a rating or ornament. They never swap jobs, and they never appear in the same component.

**The Lit Object Rule.** Colour belongs to content, not chrome. The photograph and its accents carry the warmth; surfaces stay neutral basalt. A screen where the containers are more colourful than the landmarks has inverted the system.

**The One Cool Thing Rule.** Teal means wayfinding and nothing else. The moment teal appears on a non-navigational element, it stops being a signal and becomes decoration.

## Typography

**Display Font:** `serif` (Android's Noto Serif)
**Body Font:** `sans-serif` (Roboto)

**Character:** A serif that speaks for the place and a sans that speaks for the app. The serif appears only where East Syria says its own name or names a section — big, tightly tracked, unmistakably editorial. Everything functional is Roboto, and the contrast between the two is the entire typographic idea. There are no bundled font files; both faces are platform system fonts, which keeps Arabic rendering on the OS's own fallback stack rather than a Latin-only webfont.

### Hierarchy
- **Display** (serif, bold, 56sp, `letterSpacing -0.02`): The launch screen wordmark only. "Explore / East Syria." Appears once in the entire app.
- **Headline** (serif, bold, 36–40sp): Screen-level brand titles — the home header, the "Categories" title. One per screen, at the top, never repeated.
- **Title** (sans-serif, bold, 24sp): Landmark names on feature cards. The detail screen's hero name runs larger at 32sp as the one sanctioned outlier.
- **Title Small** (sans-serif, bold, 18–20sp): Landmark names in list rows and section headings within a screen.
- **Body** (sans-serif, regular, 14sp): Descriptions and supporting copy. 13sp in compact rows where two lines must fit a 120dp thumbnail.
- **Label** (sans-serif, regular, 12sp): The most-used size in the app — metadata, captions, field labels, secondary facts.
- **Label Small** (sans-serif, bold, 10sp, `letterSpacing 0.05`, uppercase): Category badges, provenance lines ("UNESCO WORLD HERITAGE"), bottom navigation labels.

### Named Rules

**The Serif Is the Building Rule.** Serif belongs to the app's name and to screen titles. It never sets body copy, labels, buttons, form fields or data. If serif is being used to make something feel important, use hierarchy instead.

**The Bold-Is-Not-Hierarchy Rule.** Weight alone does not create a level. A screen where most text is bold has no hierarchy, only noise. Establish rank with size and colour first; reserve bold for the single most important string in a given block.

**The Arabic Parity Rule.** Arabic text is never set smaller than its English counterpart, and never given a tighter line height. Where the two appear together — the detail screen shows both names — they are typographic equals.

## Layout

The system rests on a **16dp measurement**: 16dp screen gutters, 16dp card padding, 16dp between stacked cards. Compact rows (a 120dp thumbnail beside two lines of text) tighten to 12dp padding; hero and full-bleed sections open to 24dp or 32dp. The vertical rhythm runs 4 / 8 / 12 / 16 / 24 / 32dp, with 4dp and 8dp doing the intra-component work (icon-to-label, title-to-subtitle) and 16dp and 24dp separating components from each other.

Screens are single-column and vertically scrolling. The home screen composes **horizontal rails of fixed-size feature cards (280 × 320dp)** above a vertical list — the rail is for browsing, the list is for comparing. Category and search screens are pure vertical lists of full-width cards. The detail screen inverts the model entirely: a 620dp collapsing hero image scrolls under a transparent app bar, with content sliding up beneath it against a basalt content scrim.

Navigation is a bottom bar of four destinations — Explore, Saved, Updates, Profile — with a **deliberately empty fifth slot in the centre** that the map FAB docks into. The bar is `basalt-raised`, its labels always visible at 10sp.

Layouts are phone-first and single-column throughout; the only responsive resource that varies is the FAB margin (16dp → 48dp at ≥600dp width, 200dp at ≥1240dp). There is no tablet-specific navigation.

### Named Rules

**The Sixteen Rule.** 16dp is the resting measurement — gutter, card padding, and inter-card gap. Depart from it only with a reason you could name out loud.

**The Empty Slot Rule.** The centre position of the bottom navigation belongs to the map FAB and stays empty in the menu. Never fill it with a fifth destination.

## Elevation & Depth

**This system does not use shadows.** Of the card surfaces in the app, effectively all are set to zero elevation; depth is communicated entirely by **tonal layering** — `basalt` canvas, `basalt-raised` containers, `basalt-high` for the rare element sitting on a container. One step of lightness means one step forward. The vocabulary is only three deep on purpose: a screen that needs a fourth layer has a structural problem, not a styling one.

There is exactly one sanctioned exception: the **docked map FAB**, which carries 16dp elevation and lifts to 20dp on press. It earns this because it physically overlaps the navigation bar and must read as floating above it rather than punched into it.

Photographic depth is handled separately, by scrims rather than shadows. A vertical gradient from transparent to `#CC000000` sits over hero imagery so that overlaid text stays legible regardless of the photograph beneath it; a lighter variant ending at `#99000000` is used where the overlay is shorter.

### Shadow Vocabulary
- **FAB lift** (`elevation: 16dp`, `pressedTranslationZ: 20dp`): The docked map button, and nothing else.

### Named Rules

**The Flat Basalt Rule.** Surfaces never cast shadows. Depth is lightness. The docked map FAB is the only element in the app permitted an elevation value, and adding a second exception requires retiring the first.

**The Scrim, Not the Shadow Rule.** Text over a photograph is made legible with a gradient scrim, never with a text shadow, a blur, or a translucent box.

## Shapes

Corners are **generous but not soft**. The default is 16dp — the resting radius for cards and containers. The scale departs from it in only two directions: **up** to 24dp for hero-scale objects (the 280 × 320dp feature card, the top corners of bottom sheets, filter chips, pill tabs), and **down** to 12dp for functional surfaces (search fields, image mounts inside cards, primary buttons, icon chips), 8dp for the segmented language toggle, and 4dp for status badges. Anything genuinely circular — the map FAB, avatars, circular icon buttons — is fully round.

Borders are hairlines or nothing. Where a stroke exists it is 1dp, and it is almost always the same hue as the fill it surrounds: the app's badge pattern is a **10%-alpha tint of a colour with a 1dp solid stroke of that same colour**, which is how both the admin status badges and the outlined category badge are built. Two exceptions carry deliberate weight: the 4dp gold avatar ring and the 2dp white stroke on the transparent launch button.

### Named Rules

**The Sixteen-Corner Rule.** 16dp is the default radius. Move up to 24dp for hero objects and sheets, down to 12dp for functional surfaces, down to 4dp for badges. Never sideways into 14dp, 15dp, 20dp or 25dp — those are drift, not decisions.

**The Same-Hue Hairline Rule.** A bordered element's stroke is the same hue as its tinted fill, at 1dp. A tinted fill with a contrasting border is not part of this system.

## Components

The overall register is **refined and restrained**: thinner strokes, filled surfaces used sparingly, and colour that has to earn its place. Outline and text buttons are the default; a filled button marks the single most important action on a screen.

### Buttons
- **Shape:** Softly rounded (12dp), 56dp tall on primary actions, 48dp minimum everywhere else.
- **Primary (filled):** Clay fill with **basalt text, not bone**, bold, sentence case (`textAllCaps="false"` throughout — this system never shouts in buttons). Dark ink on clay measures 5.5:1; white on clay reaches only 3.16:1 and scrapes the large-text floor. The dark-on-warm pairing is also the more restrained of the two. One per screen.
- **Outline:** Transparent fill, 1dp clay stroke, clay text. The default for secondary actions.
- **Text:** No fill, no stroke, bone-dim text. For tertiary and dismissive actions.
- **States:** Material ripple on press. Because surfaces are flat, buttons express press through ripple and colour, never through lift.

### Chips
- **Style:** Fully pill-shaped (24dp radius on a 32dp-tall chip), used for filters in the map, search and filter sheets.
- **Unselected:** `basalt-high` fill, bone-dim text, no stroke.
- **Selected:** Clay fill, bone text, with the Material check icon visible.

### Cards / Containers
- **Corner Style:** 16dp default; 24dp for the feature card.
- **Background:** `basalt-raised`, flat against the `basalt` canvas.
- **Shadow Strategy:** None. See Elevation & Depth.
- **Border:** None by default. Cards are defined by tone, not outline.
- **Internal Padding:** 16dp; 12dp for compact thumbnail rows.
- **Imagery:** Photographs sit in a nested 12–16dp mount inside the card, `centerCrop`, on a `basalt-raised` backing so a slow-loading image reads as a considered empty frame rather than a hole.

### Inputs / Fields
- **Style:** A `basalt-raised` container at 12dp radius with a transparent `EditText` inside — no underline, no outline, no floating label. Search fields stand 56dp tall with a leading icon at 16dp inset.
- **Text:** Bone at 16sp; hints in bone-dim.
- **Focus:** Cursor and keyboard only; the container does not change. The one field type that carries a stroke at rest is the OTP box (1dp clay), because a six-box row needs its own boundaries.

### Navigation
- **Bottom bar:** `basalt-raised`, four labelled destinations at 10sp with 24dp icons, always-visible labels. Active state is teal (icon and label together); inactive is bone-dim.
- **Screen back:** A 40dp rounded square (10dp radius) in a dark surface tone with a centred back chevron, floating over content on image-led screens. It supplements the system Back gesture — it never replaces it.
- **Detail screen:** No visible top app bar. A transparent `AppBarLayout` over a 620dp collapsing hero, with a `basalt` content scrim engaging as it collapses.

### Badges
Two shapes for two jobs. **Category badges** are outline-only: transparent fill, 1dp clay stroke, 12dp radius, 10sp bold clay text, 6dp × 12dp padding. **Status badges** (admin) are tinted: a 10%-alpha fill of the status colour with a 1dp solid stroke of that same colour at 4dp radius. Provenance lines like "UNESCO WORLD HERITAGE" are not badges at all — they are 11sp bold ash text with 0.05 tracking, and they carry no container.

### The Arch Plate (signature)
The home screen's featured landmarks are photographs seen **through** the pointed arch, not photographs inside cards. A single `VectorDrawable` with two subpaths and `fillType="evenOdd"` — an outer rectangle and the arch — paints the canvas colour everywhere except the opening, and sits over the `ImageView` as a mask. The fill is `basalt`, never a card surface, so no container survives around the image: the plate reads as an opening cut in the wall. 176 × 240dp, captioned beneath with a clay category label, a serif name and the city in ash. This is the same geometry as the auth screens' backdrop, which is the point — the arch you walked through on the fourth onboarding stop is the frame the catalog is seen through.

### The Section Header (signature)
Title, then a hairline rule running across the gap, then the action. `Featured ————— SEE ALL`. The rule is the same measured-hairline device as the onboarding distance rule and the sign-up step rule; the header is a ruled line on a working surface, not a label with a link floated right.

### The Register Row
Full-width list rows separated by 1dp hairlines — no cards, no elevation, no gaps between items. A 76dp square photo mount (12dp radius), serif name at 17sp over two lines maximum, city and governorate in `bone-dim`, and an optional rating. Density is the point: the register is for comparing, the plates are for browsing.

### The Plinth Label and Record
The landmark detail screen treats the place as an object on display. The photograph runs full-bleed and **dissolves into the canvas through a scrim** — no card floats over it, because a container there would contradict the flat-tonal system and hide the image's lower third. The label sits beneath the object on `basalt`: clay category, serif name, the other-script name in `warm-gray` at `textAlignment="viewStart"` so the bilingual pair reads as one block, then place, a hairline, and the rating.

Facts render as a **single label-and-value register**, not stat tiles — 11sp letterspaced label in `ash` at the start, value in `bone` at the end, 1dp hairline between rows, trailing divider suppressed. The register is built from data at runtime and **a fact with no value is never added**, so the screen has no `N/A` state. It renders every key present, not a fixed three, so new fields appear without code changes; list values are joined as prose rather than printed with brackets.

### The Instrument Group
Map controls are **one grouped column**, not a scatter of floating circles: a `basalt-raised` block at 12dp radius with a 1dp hairline edge, 48dp flat buttons stacked inside it and separated by hairlines. Wayfinding controls (my-location) sit apart from it as a flat teal circle. Nothing here is elevated — the docked map FAB remains the only element in the app with a shadow, and four shadowed FABs on one screen is what that rule exists to prevent.

Paired with it is **the readout**: a small `basalt-raised` pill with a hairline edge stating what is currently shown against the whole (`8 of 8 shown`). A filter result that appears in a toast and vanishes is not a reading; state that persists belongs on screen.

### The Docked Map FAB (signature)
A 56dp teal circle carrying the map icon, anchored to the top-centre of the bottom navigation bar and overlapping it, with the menu's centre slot left deliberately empty behind it. It is the only elevated element in the app (16dp, lifting to 20dp on press) and the only place teal appears alongside content. It is the product's thesis in one control: the map is always one reach away.

### The Language Toggle (signature)
A segmented pair inside a `basalt-raised` 12dp container: two equal-width 40dp buttons, "English" and "عربي", at 8dp radius. The active side is a solid bone fill with basalt text; the inactive side is a text button in bone-dim. Both labels are set in their own script — the Arabic option is never labelled "Arabic."

## Home v2 (main page redesign)

The main page has been redesigned onto a **warm palette** and supersedes the rules above wherever they conflict on that screen. **Every screen is now on this palette.** The legacy token names (`basalt*`, `bone*`, `clay*`, `amber`, `ash`, `hairline`, `background_dark`, `surface_dark` and the rest) still exist in `colors.xml` but carry the v2 values, so a screen that has not been redesigned cannot look out of place; new work uses the v2 names (`canvas`, `surface`, `surface-high`, `line`, `ivory*`, `terracotta*`, `gold`, `wayfinding`). The base theme is a fixed dark Material theme (`Theme.MaterialComponents.NoActionBar`), so dialogs, text fields, cursors and ripples never fall back to their light versions when the phone is in light mode.

**Direction:** dark, photograph first. The screen opens on a place, not a form.

**Palette roles.** `canvas` is the page; `surface` and `surface-high` are the two tonal steps above it; `line` is every hairline. Text is `ivory` (primary), `ivory-dim` (secondary), `ivory-mute` (meta, 5.2:1 on `surface`). **Terracotta acts** (primary button, links, category icons), **gold marks** (ratings only), **wayfinding teal is the map button and nothing else**. Ink on terracotta and on teal is `canvas` (5.8:1); white on either fails 4.5:1.

**Type.** Names are set in **Amiri** (bundled, `@font/amiri`, regular and bold): one face that carries Latin and Arabic, so a landmark's two names are typographic equals. Every Amiri text view sets `includeFontPadding="false"` and an explicit `lineHeight` (about 1.15x for Latin, 1.4x for Arabic); without both, Amiri's tall metrics leave gaps as large as the type. Roboto still carries every control and label.

**The Hero.** A 480dp full-bleed pager of featured landmarks (falling back to trending, then to anything published) running under the status bar. An 8% flat dim, a top scrim and a 380dp scrim into `canvas` sit over the photograph. On the melt: name (36sp), the Arabic name when the record has one (24sp, `viewStart`), then category and city in one meta line (never above the name), a gold rating when set, one filled terracotta pill "View landmark", and page dots (active dot 22dp wide, animated). Empty and error states reuse the same panel and copy names the problem and the recovery.

**The Floating Search.** A 56dp near-opaque pill over the hero. A solid `canvas` backing fades in behind it between 170dp and 250dp of scroll, so search stays reachable. Its height is set in code (status-bar inset + 76dp); a `match_parent` backing inside the `wrap_content` bar swallowed the whole screen.

**Category Tiles.** 64dp squircle tiles (20dp radius, `surface-high` to `surface` gradient, 1dp `line`) with a terracotta icon and a 12sp label; the index tile ("All") is outlined instead of filled so it reads as a different kind of thing.

**Trending Cards.** The photograph is the card: 220dp, 24dp radius, name (Amiri 25sp) and place on a scrim at the foot, a gold rating chip top-end that hides when unset. The section header is a title, a `line` rule and a "View map" action.

**Loading and empty.** Skeleton silhouettes of the hero and two cards breathe together (alpha 1 to 0.55, 900ms); nothing animates when the system has animations off. The trending section is absent entirely when nothing is trending.

**The Pill Navigation.** `PillNavBar`: fixed, full width less 20dp, 64dp tall, `surface` with a hairline and a soft shadow (elevation 14dp, the sanctioned exception to the flat rule, with the map button). Four icon-only tabs (Explore, Saved, Updates, Profile). Tap to commit; press and slide to carry the selector, which trails the finger, swells 12% and previews each tab (icons warm to `terracotta-soft` and lift by proximity); release commits the tab under the finger. The selector overshoots slightly when it lands. The commit reports its direction, and the next Activity slides in from that side (`slide_in_right` and friends). The Map is a separate 56dp teal button floating above the bar, end-aligned: wayfinding is one reach away and is never a tab.

**Retired on the main page:** the arch plate mask, serif-as-system-font, the docked centre FAB and its empty nav slot, the "Featured" rail (the hero is the featured surface), the ruled all-caps "SEE ALL" action, and the header count line (the count now closes the page).

## Search and Landmark Detail (warm palette)

Search and the landmark detail screen have been moved onto the Home v2 palette and type, and use the shared `Theme.EastSyria.Warm` (canvas window background, transparent system bars, terracotta primary). Everything under Home v2 applies; the notes below are only what these two screens add.

**Search.** A back chip (48dp round, `#CC221C17` with a `line` hairline) and the same floating pill as the home screen, now holding a real field, so tapping the home pill and arriving here reads as one gesture. Under it, filter chips (40dp pills, `surface` with a `line` stroke; chosen is a terracotta fill with `canvas` ink) and a live count ("8 landmarks", or "1 result for "bridge"") that announces politely to screen readers. **Filters are derived from the catalog**: one chip per category that exists among published landmarks, most common first, plus "All", and the row is absent when there is only one category. Results are dense rows made for comparing: an 84dp photo (16dp radius), the name in Amiri 22sp with the matched text in `terracotta-soft`, "category · city" on one line, a gold rating when set, and a hairline that starts where the text does. Loading shows five row silhouettes; empty and error share one centred panel whose copy names the query or the failure and offers exactly one recovery ("Clear search" or "Try again"); an empty catalog offers none. The keyboard hides when the list is dragged, and the list and panel lift above the keyboard and gesture bar.

**Landmark Detail.** The photograph runs 500dp, full-bleed, and melts into `canvas`; the name (Amiri 40sp), the other-script name (26sp) and one meta line (category, place, gold rating) sit on the melt exactly as on the home hero. The photograph drifts at half scroll speed. Three round chips float over it (back, share, save); once the hero has scrolled away a solid backing fades in behind them and the landmark's name fades in between them. **The one filled action, "Get directions", floats at the foot** as a 56dp terracotta pill with a soft shadow, and is absent when a record has no coordinates. The language toggle is a segmented pill on `surface`; its active side takes the same terracotta wash as the home navigation's selector. Arabic mode switches the description to Amiri 19sp, right to left, and never smaller than the English it replaces. The record is a label-and-value register (label 14sp `ivory-mute`, value 15sp `ivory`, `line` hairlines, no tiles, no all-caps tracking); the practical facts (hours, entry, best time) carry a terracotta icon. Tags are quiet `surface` pills, not actions. Save and share confirm with a Snackbar, never a Toast. Loading shows the hero silhouette; a missing or unpublished landmark and a network failure each get their own panel, and only the network failure offers "Try again".

**A pitfall worth recording.** A fade-in backing for a floating bar must not be a child that matches its parent's height inside a wrap_content bar: a plain View takes all available height there, the bar swallows the screen, and the page goes blank once the backing fades in. Give the backing an explicit height (bar height plus the status bar inset), set in code.

## The Shared Toolkit

Every screen after the home page is built from the same small set of parts (`com.example.eastsyria.Ui`). Reach for these before drawing anything new.

- **`ScreenHeader`.** The back chip (48dp, `#CC221C17` with a `line` hairline), the screen name in Amiri 30sp, and up to three optional trailing controls (a text action and two icon chips). It pads itself for the status bar.
- **`StatePanel`.** A centred empty / error / signed-out message: an Amiri title, one line of explanation and at most one outlined action that names its own outcome ("Try again", "Clear search", "Sign in"). The copy names the situation; it never says "Oops".
- **`SkeletonPulse`.** The one loading animation: silhouettes of the real content breathing between alpha 1 and 0.55 over 900ms. It does not run when the system has animations off. There are no spinners on list screens.
- **`warmEdgeToEdge()` and `padBottomForInsets()`.** Edge to edge with light system-bar icons, and a list that clears the gesture bar and the keyboard.
- **`showSnack()`.** Transient feedback is a Snackbar in `surface-high`, never a Toast. Destructive or reversible actions carry an **Undo** (removing a saved place, deleting a landmark).
- **The landmark row** (`item_search_result`). Photo 84dp at 16dp radius, Amiri name, "category · city", a gold rating, and optionally a description and a bookmark in place of the chevron. Search, Saved, category lists and the map all use it, so a place looks the same wherever it appears.
- **The filter chip** (`Widget.EastSyria.FilterChip`). A 40dp pill on `surface`; chosen is a terracotta fill with `canvas` ink. **Filters are derived from the catalog, never typed in.**
- **Screen states.** Every list-bearing screen has four faces: loading (skeleton), content, empty (with the reason) and failed (with the retry). A screen that can be reached signed out has a fifth.

## Screens

**Categories.** Six 188dp photo tiles, each showing the highest-rated place in its category and its real count. The tile's name and icon are static, so the grid is complete on the first frame; only the photograph and the count wait for the catalog. The map is one wide teal door beneath, the only teal on the screen. Membership in a category is decided in one place (`CategoryRules`).

**Category list.** The landmark row with a description and a bookmark. The count line states how many places, and offers "Reset filters" only while a filter is active. The filter sheet offers rating and curated flags only: a "condition" filter (intact, ruins, destroyed) was removed because no record has a condition field, so choosing it always emptied the list.

**Saved.** Newest first, one catalog read matched against the saved ids (not one read per landmark), live, so removing a bookmark anywhere updates it. Removing offers Undo.

**Updates.** Grouped into Recent and Earlier with an Amiri title and a rule. Unread updates carry a terracotta dot and full-weight titles; read ones recede. Times are localised relative times. The invented "NEW ARRIVAL" label on featured updates is gone.

**Profile.** A portrait with a terracotta ring, the name in Amiri, "Explorer from" and "Member since"; the saved places as portrait photographs; the account register, with the three rows that lead nowhere dimmed and labelled "Soon". No control is styled as live if it does nothing.

**Personal details.** The warm text field throughout, with inline errors; the sign-in email is shown but not editable here. Loading and saving dim the form and block touches.

**Map.** A back chip and the floating search pill, a category rail built from the catalog, and a persistent readout ("8 of 8 shown"). Pins are drawn in terracotta from a vector. The teal my-location circle is the only coloured control. The selected place is one card that leads to the place. Distance filtering is offered only when the phone has a location.

**Curator dashboard.** Three figures (landmarks, published, pending) in one register, the two frequent actions, search, and every landmark with its status and edit / delete controls. **Delete always asks first and can be undone.** The "view all" control that led nowhere and the users / settings tabs that led nowhere are removed, and the list now shows every landmark rather than the first ten.

**Reports.** Counted from the records, and only from them. New landmarks and users per bucket of the chosen period, the four totals, and the five highest-rated landmarks. The previous screen showed placeholder figures, "views" that were really review counts, and a weekly engagement chart drawn from fixed multipliers; nothing in the app records a view, so none of that survives.

**Forms (add and edit landmark).** Sections are `surface` cards with a hairline and an Amiri heading; fields are a step above the card and take a terracotta edge while focused; the one filled action is a terracotta pill.

**Auth.** The onboarding, sign-in, sign-up and verification screens keep their layout on the new palette, with Amiri titles and light status-bar icons. Sign-in failures are stated in words a visitor can act on ("That email and password don't match"), and the technical detail goes to the log.

## Why the home page used to sit on skeletons

Each cold start fetched the whole catalog over the network before painting. `EastSyriaApp` now turns on disk persistence and keeps `landmarks` synced, so a returning launch paints from the device in about a second and the network result updates it in place. Persistence has to be enabled before the database is first used, which is why it lives in the Application class.

## The Dock, the Passport, the Onboarding and the Fields

**The Dock (`NavDock`).** Every top-level screen (Explore, Saved, Updates, Profile) carries the same bottom dock: four icon tabs in a 68dp `surface` pill (soft shadow, hairline), with **the map button set into the middle** on a canvas-coloured ring that reads as a notch cut into the bar. The map is a 64dp teal circle that rises 26dp above the pill and is never a tab; wayfinding is one reach away from every top-level screen. The selector carries the earlier behaviour: tap a tab to open it, press and slide to carry the selector (it trails the finger, swells, previews each tab), release to commit; it overshoots slightly when it lands. The commit slides the next screen in from the side the finger moved toward. Explore is the only root: choosing it clears everything above it, and a tab opened from another tab replaces it, so Back always lands on Explore. Top-level screens show no back arrow. Their lists and content clear the dock (128 to 136dp of bottom padding plus the gesture bar).

**The Passport (Profile).** The cover is a `surface-high` to `surface` card with a gold hairline frame, a large faint arch bleeding off the corner, the "East Syria" wordmark, a gold-ringed portrait, the name in Amiri 32sp, "Explorer from", and **three counted facts**: member since (from `createdAt`), places saved, and kinds of place among them. Below, the saved places are **stamps**: portrait photographs with a rubber-stamp roundel in the corner on a dark disc. With nothing saved there is an outlined dashed slot waiting for the first stamp. The account register is one card; rows that lead nowhere are dimmed and labelled "Soon".

**The Onboarding.** Four stops, each a **photograph from the catalog** (bundled as `onb_*.jpg`, so it is instant and works offline) cropped to a portrait window and melting into the canvas, with a piece of drawn art over it: a pointed gold arch, a wall label holding a landmark's name in English and Arabic exactly as the catalog holds it, a dotted route from a radar ring to a pin, and a passport stamp. The photograph trails the swipe and the art runs ahead of it. An eight-pointed star in a rule marks where the picture ends and the words begin. Skip is a translucent pill so it reads on any photograph. The copy claims only what the product does.

**Text fields.** Outlined on `surface`: a 1dp hairline at rest, a 2dp terracotta edge while focused, 16dp corners, 56dp tall, a terracotta start icon, and the hint centred inside the box. Labels above the fields are sentence case in medium weight (tracked capitals were dropped: they shouted at a form that is only asking a question and break Arabic joins). Errors are `terracotta-soft` with an icon, worded as what to do. **The city field leads with the Syrian flag** (green, white and black bands, three red stars on the white), and so does every row in its list, on a rounded `surface-high` popup.

## Do's and Don'ts

### Do:
- **Do** put every screen on `basalt` (`#1A1A1A`) and every container on `basalt-raised` (`#2A2A2A`). One step of lightness per level of nesting.
- **Do** reach for clay (`#D17A5A`) for anything interactive and amber (`#F5A623`) for anything rated or ornamental — the Two Warmths Rule.
- **Do** set text on a clay fill in basalt, not bone. Measure before choosing: warm mid-tone accents rarely carry white text at 4.5:1.
- **Do** keep serif for the app's name and screen titles only; set everything else in Roboto.
- **Do** default to 16dp corners, 16dp gutters and 16dp card padding, and step the vertical rhythm in 4 / 8 / 12 / 16 / 24 / 32dp.
- **Do** build tinted badges as a 10%-alpha fill with a 1dp stroke of the same hue.
- **Do** scrim photographs with a transparent-to-`#CC000000` gradient before setting text over them.
- **Do** use `start`/`end` rather than `left`/`right` on every margin, padding and constraint, and pull every user-facing string from `strings.xml` — the product's recorded Arabic-UI obligation depends on both.
- **Do** keep every touch target at 48dp or larger, including the 40dp icon buttons currently sitting under that floor.

### Don't:
- **Don't** introduce a fourth surface tone. Three steps (`#1A1A1A` / `#2A2A2A` / `#3A3A3A`) is the whole depth vocabulary; a screen needing more has a structure problem.
- **Don't** add a shadow. Only the docked map FAB carries elevation, and that exception is not transferable.
- **Don't** use `#FF6B35`, `#FFA726`, `#FF9500` or `#FFB800` as accents. The four-orange sprawl is drift; interactive elements converge on clay `#D17A5A` and highlights on amber `#F5A623`. `#FFA726` survives only as the semantic *pending* status colour.
- **Don't** use `#2D3748` or `#374151` (the current `input_background` and `divider_dark`). They are cool blue-slate values in a warm system and read visibly wrong beside clay — migrate them to `basalt-raised` and `hairline`.
- **Don't** collapse `#2A2A2A`, `#2C2C2C` and `#2D2D2D` into three separate tokens. They are visually indistinguishable; `basalt-raised` is the one true value.
- **Don't** revive the dead palette entries in `colors.xml` — `beige_light`, `card_beige`, `card_green`, `peach`, `cyan`, `pink`, `gray_light` are declared but referenced nowhere, and `cyan`/`pink`/`peach` belong to no world this app inhabits.
- **Don't** ship a control that looks live and does nothing. Where a destination does not exist yet, dim the row to 35%, remove its ripple and focus, and replace its chevron with a `SOON` label — a dead control styled identically to a working one is the interface lying about itself.
- **Don't** hardcode a filter's label separately from what it filters. The map shipped "Ancient Cities", "Fortresses" and "Roman Era" wired to `Historical`, `Archaeological` and `Nature`; one was pre-selected and there was no "All", so the map opened on a silent subset. Derive filters from the data they filter.
- **Don't** hardcode per-item content into a layout that renders many items. The detail screen shipped three "Architectural Features" cards and a "Historical Significance" panel written for one fortress and drawn on every landmark in the catalog; Palmyra claimed a moat. If a section cannot be driven by the record, it is not a section.
- **Don't** print `N/A`. A fact with no value is a row that should not exist, and a rating of `0.0` is a mark that should not be drawn.
- **Don't** render a metric the product cannot produce. The trending card once showed `4.8 (1.2k)`; nothing in this app can author a review, so the count was social proof invented by the layout. A rating hides entirely when unset rather than showing `0.0`, and counts stay gone until reviews exist.
- **Don't** set text bold by default. Bold is for the single most important string in a block, not for every string in it.
- **Don't** hardcode colours in layout XML. Where a value like `#CC8B6A` or `#A0A0A0` appears inline today, it should resolve to a named token.
- **Don't** treat `themes.xml` as the source of visual truth. Screens hardcode their root background, and the theme files still disagree with each other — the tokens above, not the theme resources, describe what actually renders.
- **Don't** redefine a style in `values-night/` that already exists in `values/`. The night copy silently shadows the base one and drift becomes invisible until it ships: a duplicate `OtpEditTextStyle` there kept the verification screen's six cells on the retired blue-slate drawable long after the base style was replaced. Night overrides carry only what genuinely differs at night.
- **Don't** use `textAllCaps` on buttons. Sentence case is the established voice.
