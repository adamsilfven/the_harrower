# Harrower Summoning: Locked Design & Engineering Handoff

**Project:** Leyline Convergence, Minecraft Java 1.21.1 / NeoForge  
**Status:** Gameplay and presentation direction APPROVED / LOCKED, implementation NOT claimed  
**Scope:** Hollow Cairn construction, catalyst item interaction, ritual effects, boss emergence. Preserve all unrelated Harrower and LC systems.

## 1. Approved design

The **Hollow Cairn** is a **flat, horizontal, reusable ten-block stone frame** with an empty opening in the middle. No large floor, pillars, roof, temple, or custom altar. Its construction should be easy and inexpensive for players who have already completed substantial progression. The agreed conceptual layout is a **4×3 perimeter**, consisting of ten frame blocks surrounding a **two-block interior opening**:

```text
Top-down, one horizontal Y level:
S S S S
S . . S
S S S S

S = stone frame block (10 total)
. = empty opening (2 block spaces)
```

The **ten-block layout is approved**. The prior discussion proposed polished deepslate or a mix of polished deepslate and deepslate bricks. The user said to lock it in after being asked about the material and 4×3 frame; treat **polished deepslate as the default implementation choice** for a simple, uniform, recognizable frame. If a mixed-block pattern is used, do not increase cost or introduce new blocks without approval. Do not confuse the hollow opening with a solid platform.

**Interaction:** The player **throws/drops the exploration catalyst item into the opening**. The thrown item entity is the trigger, not a right-click GUI, menu, or manual ritual button. The item vanishes as the ritual begins and the opening turns **completely black**, not purple or brightly glowing. The frame survives for repeated summons; each successful summoning consumes **one catalyst**. The exact catalyst item identity and loot acquisition remain part of the broader exploration-catalyst design and must be verified against the approved code/assets rather than invented.

**Ritual sequence:**
1. **0–2 seconds, Offering:** Recognize a catalyst item entity within the two-block opening. Confirm a valid cairn, clear nearby boss emergence location, no other ritual/boss conflict, and valid summoner before consuming exactly one catalyst. The opening becomes solid-looking, featureless black visually (without creating an unsafe permanent physical obstruction).
2. **2–8 seconds, Release:** Approximately **30–50 souls** erupt **upward** from the opening in overlapping bursts, reusing the **same soul visual assets/effect language as the Harrower's existing Hollowing attack**. Do not invent a different soul design. Use a rising rumble and existing stone-grinding audio as appropriate.
3. **8–11 seconds, Silence:** Last souls fade, opening stays black, rumble cuts abruptly, brief unnatural silence.
4. **11–15 seconds, Arrival:** Harrower appears in a **validated clear location near the summoning player**, preferably **12–18 blocks away**, facing them and acquiring aggro almost immediately. No prolonged cinematic idle. The boss does not emerge inside the 4×3 cairn. The black opening effect ends and the original reusable frame remains intact.

**Environment/behavior:** Harrower is enormous (~10–12 blocks tall, 8–10 blocks shoulder width). Prevalidate full bounding box and a realistic, traversable route to the summoner, avoiding suffocation, walls, ceilings, water hazards and terrain that would break his pathfinding. Do not break blocks or force-load chunks to create a spawn. Do not consume the catalyst if a safe spawn cannot be found. The structure is a trigger, not a boss arena.

**Re-entrancy:** Prevent a second catalyst from launching a concurrent ritual or summoning a second Harrower while the first ritual is active. Preserve server-side authority and avoid item duplication or loss. A ritual interrupted by unload/restart must not leave a permanent black hole, trapped item, ghost boss, or duplicated summons.

## 2. Existing Harrower mechanics to preserve

- The Harrower never naturally spawns; the cairn is the deliberate survival summoning mechanism. Existing development `/harrower summon` access should remain for testing, without needing the ritual.
- The exploration catalyst is acquired through exploration and is consumed per successful summoning; the structure itself is reusable.
- Do **not** require all five Leylines to summon him. His established Leyline suppression and combat rules remain unchanged.
- Preserve his locked appearance, phases, attacks, Hollowing damage, existing soul visuals, suppression behavior, aftermath, and Hollowforged drop mechanics. Do not modify any unrelated assets, items, quest rewards, or equipment.
- Do not turn this into a new multiblock quest chain or require rare materials for the cairn.

## 3. Engineering requirements

**Recognition:** Detect the exact horizontal 4×3 frame in either X/Z orientation, all ten required blocks present at one Y level, two unobstructed central cells. Do not accidentally match incomplete frames or nearby unrelated stonework. Decide and document whether frame can be embedded in terrain; ensure black aperture VFX are visible and item detection works.

**Item entity handling:** Listen on logical server for item entities entering either opening cell. Check item type, count, active ritual state and safe spawn site. Consume one catalyst only after all preflight checks pass. If a stack is thrown, preserve remaining items without duplication. Reject invalid items without deleting them. Avoid triggering twice from the same item entity/tick.

**VFX and sound:** Reuse Hollowing soul visuals directly where technically possible, adjusting emission origin, upward velocity, burst timing, and count. A **30–50-soul** effect must be performance-bounded and multiplayer-visible, with no permanent visual state. Black aperture is temporary client-side presentation, not a permanent replacement of world blocks. Provide suitable audio attenuation to nearby players and avoid unintended global audio changes.

**Spawn safety:** Precompute/validate candidate spawn positions at 12–18 blocks when possible; fall back only to genuinely safe nearby positions if needed, or decline summoning without consuming the item. Check entity collision and practical route, not only empty air. Face summoner and initialize target promptly. The known Harrower footing/pathing issues make this a release-critical test.

**State and cleanup:** Track one ritual per cairn and enforce an appropriate global active-Harrower restriction if the existing boss design requires it. On interruption, restore presentation, release pending state and ensure catalyst consumption policy cannot be exploited. Do not make claims of testing without logs/evidence.

## 4. Acceptance tests (not yet performed)

1. Build exact 4×3 ten-block polished-deepslate frame: detected; remove any required block: not detected.
2. Both orientations work. Two center cells stay empty before activation.
3. Drop wrong item: nothing consumed or triggered. Drop one valid catalyst: exactly one ritual.
4. Drop stack of catalysts: exactly one consumed; remainder preserved, no repeated activation.
5. Black opening is **black**, not purple; returns to normal after ritual or interruption.
6. 30–50 Hollowing-style souls visibly shoot **upward** in overlapping bursts; no substitute particle design.
7. Timings approximately 0–2 / 2–8 / 8–11 / 11–15 seconds; synchronized nearby multiplayer presentation.
8. Harrower spawns near player, facing and aggroing quickly, with no clipping or immediate pathfinding failure.
9. Obstructed/unsafe arena: no ritual, no catalyst loss; no terrain alteration or forced chunk loads.
10. Cairn remains intact and reusable after victory/failure. Repeated summon requires another catalyst.
11. Second catalyst during ritual does not duplicate boss or consume unnecessarily.
12. Restart/unload/interruption cleans up black aperture, state, particles, and pending boss; no ghost rituals.
13. Existing `/harrower summon`, Hollowing effects, suppression, combat, and Hollowforged drops remain unchanged.
14. Validate on actual fresh-world NeoForge 1.21.1 build and provide evidence rather than assertions.

## 5. Boundaries / unverified items

This handoff captures the user-approved visual/gameplay intent. **It is not a source patch, compiled JAR, or proof of in-game testing.** The exact exploration catalyst item and loot source must be obtained from current project assets or separately finalized; do not invent an item ID. The previously proposed polished-deepslate uniform block is the practical default; exact current registry and code integration need inspection. Preserve approved files byte-for-byte except the named summoning scope. Report changed files, hashes, build/test results and known limitations.
