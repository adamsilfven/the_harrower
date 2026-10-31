# Harrower: Locked Endgame Loot Engineering Handoff

Target: standalone public Harrower mod, Minecraft Java 1.21.1, NeoForge, Java 21.
Status: DESIGN LOCKED. This is an implementation brief, not a claim of a built or tested JAR.

## Nonnegotiable preservation
- Harrower drops Hollow Core(s), never Hollowforged equipment.
- Preserve all five existing Hollowforged crafting recipes, gear assets, mechanics and abilities unchanged: Crown of the Hollow, Carapace of the Unbroken, Greaves of the Stillborn, Treads of the World, Worldshard.
- No new custom loot items. All additional drops are vanilla Minecraft items.
- No Dragon Egg. Do not add a Dragon Egg roll.
- Preserve existing Harrower combat, summoning ritual, and other unrelated systems.

## Guaranteed on each successful Harrower kill
- Hollow Core: 1, always.
- Enchanted Golden Apples: 2–4, always.
- Netherite Ingots: 4–8, always.
- Experience: 1,000–1,500 XP total awarded for kill (avoid double-counting existing boss XP; inspect baseline).

## Independent bonus rolls per kill
Every row below is independent, and multiple rows may succeed on the same kill. No substitution by a three-tier selection system.

| Reward | Chance | Quantity |
| --- | ---: | ---: |
| Enchanted Netherite Sword | 65% | 1 |
| Enchanted Netherite Armor Piece | 80% | 1 |
| Additional Enchanted Golden Apples | 60% | 2–4 |
| Nether Star | 50% | 1–2 |
| Elytra | 35% | 1 |
| Totem of Undying | 75% | 2–4 |
| Heavy Core (vanilla) | 40% | 1 |
| Enchanted Netherite Pickaxe | 45% | 1 |

### Equipment enchantments
- Sword: Sharpness V, Looting III, Unbreaking III, Mending, Fire Aspect II.
- Armor: Protection IV, Unbreaking III, Mending, plus appropriate slot-specific enchantments. Choose one of the four Netherite armor slots; no custom armor. The precise slot weighting and slot-specific enchantments were not separately locked: use an even 25% slot distribution and vanilla-compatible maximum-level utility enchantments as an implementation default, and document exact choices.
- Pickaxe: Efficiency V, Unbreaking III, Mending, plus **either** Fortune III or Silk Touch, never both. Equal 50/50 selection is an implementation default; document it.
- Items should use valid Minecraft 1.21.1 enchantment components and vanilla enchantment compatibility. Do not invent illegal enchantment combinations.

## Hollow Core bonus, independent of all vanilla rolls
- Guaranteed 1 Hollow Core per kill.
- If the Harrower is killed within the qualifying Leyline area, add a **single independent chance** to drop **exactly one additional Hollow Core**, at **5 percentage points per active Leyline**, maximum 25% for five active Leylines.
- Evaluate the count of active Leylines **at the instant the Harrower dies**, not at summon or fight start.
- This is a single probability roll, not five separate 5% rolls.
- Without Leyline Convergence installed, or without qualifying Leylines, the bonus chance is 0%; base Core remains guaranteed.
- Optional LC integration must never be a hard dependency for the public standalone mod.
- Inspect the actual existing Leyline ownership, radius, and qualification APIs and the killer attribution logic before coding. Do not guess mod IDs, class names, or data schemas. If eligibility is ambiguous, document it rather than silently changing the rule.

## Implementation constraints
- Use the existing consolidated Harrower mod, not a separate loot addon or replacement of LC Core.
- Ensure exactly one reward event per real death; protect against double-processing, repeated death ticks, client/server duplication, or multiple players claiming the same kill.
- Run loot logic server-side; support multiplayer, normal death attribution, and server restart safety as applicable.
- Maintain compatibility with vanilla loot handling and modpack loot table overrides where feasible; document chosen method.
- Preserve default gameplay loot independence from player Luck/Looting unless explicitly supported by existing design; do not silently amplify the listed chances/quantities.
- Ensure all extra items can actually drop and be collected, with equipment NBT/components and enchantment glint working correctly.

## Deliverables and verification
1. ONE compiled installable NeoForge 1.21.1 JAR, containing all pre-existing Harrower content and new loot logic.
2. Full updated source and reproducible build instructions.
3. Changelog identifying all modified classes/resources and preservation of unrelated content.
4. Build logs, JAR integrity/contents checks, and any actual runtime test evidence. Distinguish compilation, static verification, and in-game testing.
5. One consolidated in-game checklist: guaranteed quantities; each independent bonus; correct enchantments; no Dragon Egg; guaranteed Hollow Core; Leyline bonus at 0/1/5 active Leylines at kill time; outside-Leyline no bonus; no LC dependency; no duplicate rewards.

Do not redesign the loot table, add items, replace recipes, or ask for micro-approvals. Complete in one consolidated pass; report any genuine blocking incompatibility.
