# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

East Syria has **no single primary user** — confirmed by the user. The visitor-facing side is designed to work for all of these at once, and no one of them may be optimised for at the others' expense:

- **Visitors planning a trip** — deciding where to go, browsing landmarks before or during a visit. Jobs: orientation, planning, getting there.
- **Syrians and the diaspora** — people with a personal connection to the region, exploring and reconnecting with heritage sites. Jobs: discovery, recognition, sharing.
- **Locals on the ground** — finding what is near them right now. Jobs: proximity, map, practical visiting facts.
- **Heritage documentation** — the catalog record itself is a legitimate end, not only a means to a visit.

Because the audiences are equally weighted, a screen that only serves the trip-planner (or only the browser-for-pleasure) is incomplete. Landmark records already carry both the evocative layer (description, imagery, tags) and the practical layer (visiting hours, entry fee, best time to visit, coordinates); both must stay first-class.

**Second audience — administrators.** A separate signed-in role (`role: "admin"` on the user record) curates the catalog: adding, editing, publishing, and reviewing engagement analytics. This is a real working audience with its own task-completion needs, not a hidden debug surface.

## Product Purpose

A mobile catalog of Syrian landmarks and heritage sites — historical sites, nature and rivers, local culture, museums, cuisine, and handicrafts — that lets a person discover a place, understand what it is, and find out how to visit it.

Success means: a person opens the app without a destination in mind and leaves with one they can act on; and the catalog behind it stays accurate and curated because administrators can maintain it from inside the app.

## Positioning

Every landmark is a **bilingual, curated record, not a scraped listing**. Each entry stores parallel English and Arabic name, category, short and long description, city, governorate, status, and tags — authored by an administrator through the app's own workflow and gated by a publish state, so unpublished drafts never reach visitors. Combined with the culture-and-craft categories (cuisine, handicrafts, local culture) alongside the expected historical sites, the product covers heritage as *lived practice*, not only as ruins and museums.

## Operating Context

- **Visitor flow:** launch → sign in → main page (featured + trending landmarks) → categories, map, search, or saved → landmark detail → directions.
- **Landmark detail** carries a per-landmark English/Arabic toggle; the entry shows both scripts at once (primary name in the active language, the other beneath it).
- **Map** is a real navigational surface, not a decoration: Google Maps with clustering and a filters bottom sheet; landmarks carry latitude/longitude.
- **Category browsing** runs through six fixed categories with a filter bottom sheet: `historical_sites`, `nature_rivers`, `local_culture`, `museums`, `cuisine`, `handicrafts`.
- **Saved landmarks** persist per user at `users/{uid}/savedLandmarks/{landmarkId}`; bookmarking is available from the detail screen and from category list rows.
- **Admin flow:** launch → role lookup routes to the admin dashboard → landmark list with status badges → add / edit form (bilingual field pairs, image upload) → publish → Reports.
- **Reports** is a period-scoped analytics screen (tabbed time ranges) showing total landmarks, user totals, growth percentages, a custom-drawn engagement chart, and a most-popular-landmarks list.
- Content is authored in two languages by the same admin in one form; every Arabic field sits beside its English counterpart.

## Capabilities and Constraints

**Stack (existing):** Kotlin, Android XML layouts with ViewBinding (no Compose), Material Components / Material 3 themes, `minSdk 31`, `targetSdk 36`. Firebase Auth (email/password + Google via Credential Manager, plus an email verification step through JavaMail), Firebase Realtime Database, Firebase Storage. Google Maps SDK with `android-maps-utils` clustering, Play Services Location, Glide, CircleImageView. One Activity per screen; navigation is explicit `Intent`s, not a nav graph.

**Data:** three Realtime Database roots — `landmarks`, `users`, `notifications`. Role is stored on the user record and cached in `SharedPreferences` (`user_prefs`, key `role_$uid`) so launch routing does not wait on the network.

**Confirmed capabilities:** email/password and Google sign-up with verification; featured and trending rails; six-category browse with filters; full-text landmark search; map with filters and clustering; bookmarking; in-app notifications; profile and personal details; admin add/edit/delete with image upload; publish/pending status gating; analytics reports.

**Constraints:**
- Permissions already requested: internet, network state, fine and coarse location, media images read. Location is central to the map and proximity, so location-denied is a real state, not an edge case.
- `MAPS_API_KEY` is injected from `local.properties`; the map cannot render without it.
- `android:supportsRtl="true"` is already declared in the manifest.

**Open product decisions — record, do not resolve by inventing:**
- **Geographic scope is undecided.** The app is named "East Syria", but `res/values/arrays.xml` ships all 50 Syrian cities and landmark records take a free-text governorate. Whether the catalog covers eastern Syria (Deir ez-Zor, Ar-Raqqah, Al-Hasakah, the Euphrates) or the whole country is deliberately unsettled. Do not write copy, taglines, or empty-state text that silently commits to either reading.
- Ratings and review counts exist on the landmark model (`rating`, `reviewCount`) but no review-authoring capability exists. Do not present these as user-generated unless that capability is built.
- The night-mode toggle on the launch screen (`fabNightMode`) is wired to an empty function.

## Brand Commitments

- **Name:** East Syria (`app_name`). No confirmed logo, wordmark, voice guide, or palette specification was established — the colors and styles currently in `res/values/` are incumbent implementation, not a ratified identity.
- Nothing about the current visual treatment has been declared binding.

## Evidence on Hand

- Real landmark content lives in Firebase Realtime Database and is authored through the admin screens; it is not in the repository.
- Bundled imagery in `res/drawable/` (`background.png`, `app_icon.png`, category and landmark art) plus a full custom icon set.
- **Absences that must not be fabricated:** there are no testimonials, no user reviews, no download or usage numbers, no press, no partnerships or institutional endorsements, no pricing, and no publisher or ministry affiliation. `res/values/strings.xml` still contains the Android template's `lorem_ipsum` placeholder — it is scaffolding, not content.

## Product Principles

1. **Serve every audience on the same screen.** With no single primary user, a landmark must read as both an invitation (why this place matters) and a plan (where, when, how much, how to get there). Dropping either layer breaks the product for half its users.
2. **Both languages are equal citizens.** Arabic is not a translation bolted onto English. Content is authored in pairs, and the interface must never treat Arabic as secondary or as an afterthought in layout, typography, or line length.
3. **Curation is the product.** Published records are vouched for. The publish gate, the admin workflow, and the accuracy of practical fields are the reason to trust the app over a search engine.
4. **The map is infrastructure, not garnish.** Location, proximity, and directions are core to visiting and to locals; they are never reduced to an illustrative graphic.
5. **Claim nothing that is not true.** No invented reviews, counts, partners, or coverage — including in empty states, marketing copy, and placeholder data.

## Accessibility & Inclusion

- **Bilingual UI and RTL are a known future obligation, not a current requirement.** Confirmed by the user: today the interface chrome is English-only (no `values-ar`, no RTL-mirrored layouts) while landmark *content* toggles between English and Arabic. Future work must not introduce anything that blocks a full Arabic UI later — avoid hardcoded strings in layouts and code, avoid `left`/`right` in favour of `start`/`end`, and keep layouts tolerant of Arabic's longer running text and different typographic metrics.
- Arabic text already renders inside an English interface today, so Arabic legibility (type size, line height, font fallback) is a present concern, not a deferred one.
- No further product-specific accessibility standard has been established. Platform baselines (touch target size, contrast, TalkBack labelling, dynamic type) still apply as ordinary Android quality.

## Project Context

This is a **portfolio piece** — built to demonstrate design and engineering ability to others. The work is judged on craft and on the impression it makes, so unfinished states, placeholder text, and inconsistency are visible failures rather than acceptable shortcuts. `applicationId` is still `com.example.eastsyria` and no release signing or shrinking is configured; store release is not currently a constraint.
