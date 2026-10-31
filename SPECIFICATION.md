# Harrower Hollow Cairn Ritual — Consolidated Mod Engineering Handoff

**Status:** Approved gameplay design; implementation NOT completed or tested here. **Deliverable required:** one compiled, installable NeoForge 1.21.1 JAR containing existing Harrower, Hollowforged and Hollow Fragment plus the summoning ritual. No extra ritual addon, no LC Core replacement.

## Actual supplied baseline
- `reference-harrower-0.6.0-rc.1.jar` is the user's supplied compiled reference. SHA-256: `dbffdc60e25de10e222b17c3d43e3fba7d9d31caa8bb208649558fa371f07647`.
- JAR metadata identifies three mod IDs inside the **same** JAR: `lc_harrower` version `0.6.0-rc.1`, `lc_hollowforged` version `1.0.1`, and `hollow_fragment_addon` version `1.0.1`. Preserve these IDs and existing content unless migration is demonstrably necessary and explicitly approved.
- Existing assets include `assets/hollow_fragment_addon/models/item/hollow_fragment.json` and two item textures. Existing classes include `com/leyline/harrower/client/HarrowerHollowing.class`, `com/leyline/harrower/combat/HollowingEffects.class`, and `com/leylineconvergence/hollowfragment/HollowFragmentAddon.class`. Inspect their source/bytecode to identify the actual soul VFX and item registry. Do not invent API names or assert the existing effects can be called unchanged until inspected.
- Source is NOT included in this reference JAR. Obtain matching editable source/build setup, or reconstruct safely and disclose provenance. Do not silently replace existing approved boss behavior with approximations.

## LOCKED structure and interaction
- Horizontal, flat **4×3 outer footprint**, **10 polished-deepslate full blocks** making a complete rectangular perimeter, with the **two center positions empty**. No extra altar blocks, custom keystone, pillars, platform, or rare resources. Orientation along either horizontal axis. All blocks same Y elevation.
- Top-down, 4 columns × 3 rows (`D` = polished deepslate; `.` = opening):

  `DDDD`

  `D..D`

  `DDDD`

- Player throws **one** `hollow_fragment_addon:hollow_fragment` item entity into either opening cell. This is a physical dropped item, NOT right-clicking a UI. Use the existing registered item, do not add a duplicate.
- Upon accepted activation the fragment disappears/one item is consumed; the opening appears **unnaturally black** without permanently changing the underlying terrain or breaking the Cairn. Rendering should work from ordinary gameplay camera angles, remain within the two-cell void, and be synchronized to nearby players.
- **30–50 souls** surge upward from the black opening in staggered bursts, reusing the actual Harrower Hollowing soul visual language/assets rather than generic purple particles. Follow with a short, eerie silence, then Harrower emerges nearby.
- Target timing from prior approved sequence: ~0–2 s offering/blackening; ~2–8 s souls; ~8–11 s silence; ~11–15 s arrival. Fine animation timings can be tuned within this arc, not redesigned.
- Spawn **approximately 12–18 blocks from the activating player**, on a verified safe, accessible surface with sufficient room for the enormous boss, facing player and acquiring target almost immediately. Harrower should not overlap player or Cairn. Prefer a visible nearby spawn location. Do not teleport into caves, water, walls, cliffs or inaccessible terrain.
- Reusable Cairn. Each successful ritual requires one new Fragment. No requirement for Convergence or five Leylines. Harrower never naturally spawns due to this feature. Existing `/harrower summon` developer behavior should continue to work without ritual.

## Implementation safety and edge cases
1. **Server authoritative**: validate structure, item entity, world/dimension eligibility, activation player, and available spawn site *before* consuming anything. A failed attempt leaves the Fragment recoverable and gives concise player feedback where feasible. Do not automatically eat items dropped into malformed structures.
2. Support stacked items: consume exactly **one** and leave the remainder. Prevent item duplication and multiple activations from the same entity tick.
3. Lock out concurrent ritual activations in the same Cairn and avoid overlapping nearby rituals or boss spawns. Do not destroy another player's fragment.
4. Persist/safely cancel active ritual state across chunk unload, server stop/restart, player disconnect, and dimension changes. Specify refund or deferred consumption behavior; no item loss without successful spawn. A robust design reserves the fragment then commits consumption at successful spawn, while showing it disappear immediately; refund on cancellation. Avoid dupes.
5. Validate room against the **actual Harrower collision dimensions and pathing**, not an assumed generic mob footprint. Never alter boss collision or pathfinding to make a bad spawn fit.
6. Sync black-void visuals, soul burst, sounds, and entity spawn to all tracking clients. Keep server gameplay independent of client-only classes. Avoid global muting of Minecraft audio: the ritual's silence should be local and non-destructive.
7. Do not grief terrain, replace player blocks, force-load chunks, or leave permanent black blocks. Handle obstructed opening (solid blocks, fluids), broken Cairn during ritual, and competing dropped items.
8. Preserve existing boss phases, animations, Hollowing mechanics, suppression, loot, Hollowforged gear, controls, assets, item model, tooltips, and mod IDs. Limit modifications to necessary summoning implementation and registrations.

## Implementation approach (suggested, not locked API)
- Add a server-side ritual coordinator/state machine keyed by dimension and normalized frame origin, and a robust dropped-item entry detector. Determine attribution to player using legitimate item owner/thrower tracking or nearby player rules; define behavior if no identifiable player is present. No ritual should be triggered by random item drift without an eligible nearby player.
- Locate a safe Harrower spawn point *before* starting effects. Reserve it, revalidate at arrival, and refund on failure. Consider player movement between start and spawn; prefer the original activating player as aggro target if still eligible.
- Use client-bound network events for a two-cell dark plane/void effect and soul VFX, or other minimally invasive rendering. Reuse Hollowing VFX implementation after inspecting it; avoid duplicated low-quality approximations.
- Keep configuration minimal; user approved the gameplay defaults. Do not introduce extra mandatory items or construction rules.

## Validation and release gate
- Clean Gradle/Java 21 NeoForge 21.1.x build for Minecraft 1.21.1; verify mod metadata and packed classes/resources, no duplicate registries or missing item models. Deliver source plus a single replacement JAR and SHA-256 checksums.
- Actual client + dedicated server tests where available, with log evidence. Test both frame orientations, throw in either opening, one and stacked Fragments, incomplete/obstructed frame, invalid spawn, cramped space, nearby water/cliffs, ritual break, two players, two competing Cairns, disconnect/restart/chunk unload, repeat summons, and developer command.
- Confirm souls visually match Hollowing, count 30–50, black opening and silence are visible/audible, boss appears close and immediately attacks, and no approved gear/boss behavior regresses.
- State explicitly which tests were executed and which remain for the user's fresh-world in-game test. **Do not claim in-game verification without logs/screenshots or actual run evidence.**

## Scope boundaries
No redesign of the Hollow Fragment, Hollow Cairn, Harrower, or Hollowforged gear. No standalone ritual addon. No new dimension or progression gate. No edits to LC Core. Do not request repeated micro-approvals: deliver one consolidated implementation and a concise test checklist.
