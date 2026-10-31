# User-approved varied Harrower loot — 0.6.2-rc.2

This supersedes only the equipment selection in the earlier locked table and adds the approved rare-find category. It also implements the user's delayed-drop request. No new custom items, Dragon Eggs or Hollowforged equipment drops.

## Guarantees and unchanged independent rolls

Each kill commits one Hollow Core, 2–4 Enchanted Golden Apples, 4–8 Netherite Ingots, and 1,000–1,500 total XP. The death-time owned-Leyline bonus remains one additional-Core roll at 5% per qualifying active color, maximum 25%, using the existing eligibility and attribution rules.

Independent bonus rows: weapon 65%; Netherite armor 80%; extra apples 60% (2–4); Nether Star 50% (1–2); Elytra 35% (1); Totems 75% (2–4); vanilla Heavy Core 40% (1); Netherite tool 45% (1); rare find 35% (1). Multiple or all rows can succeed together. Default Luck/Looting does not change them. The following internal selection weights are implementation defaults, not additional user-locked percentages.

## Weapon row — equal 1/6 per family

All have Unbreaking III and Mending except the Infinity bow, which has Unbreaking III and Infinity instead of Mending.

- Netherite sword: Sharpness V, Looting III, Sweeping Edge III, Fire Aspect II.
- Netherite axe: Sharpness V, Efficiency V.
- Mace: Wind Burst III, plus equal Density V or Breach IV. Never both.
- Trident: Impaling V, plus equal Loyalty III + Channeling, or Riptide III. Never Loyalty/Channeling with Riptide.
- Bow: Power V, Punch II, Flame, plus equal Mending or Infinity. Never both.
- Crossbow: Quick Charge III, plus equal Multishot or Piercing IV. Never both.

## Armor row — equal 25% per slot

Every piece has Protection IV, Unbreaking III and Mending. Independently within each slot's curated entries, 25% also have Thorns III.

- Helmet: Respiration III, Aqua Affinity.
- Chestplate: the common set, with the occasional Thorns addition.
- Leggings: Swift Sneak III.
- Boots: Feather Falling IV, Soul Speed III, plus equal Depth Strider III or Frost Walker II, never both.

## Tool row

Netherite pickaxe 50%, axe 25%, shovel 25%. Every tool has Efficiency V, Unbreaking III and Mending, plus equal Fortune III or Silk Touch. Never both.

## Rare-find row — eleven equally weighted choices

Six armor trims: Silence, Spire, Tide, Ward, Vex, Eye. One Netherite Upgrade smithing template. Four enchanted-book options: Mending, Swift Sneak III, Soul Speed III, Wind Burst III. Each book contains exactly its one curated enchantment. All items are vanilla.

## Physical payout timing

Native item rolls, enchantment components, Core count and XP are fixed at death. Full item-entity NBT and XP are saved on the corpse. At death tick 410 (about 20.5 Minecraft seconds), during the existing crumble phase, items spill from the remains and XP is awarded once. They are not rolled again. Saving/unloading preserves the pending payload and death clock without force-loading chunks. The death animation, its 600-tick duration and the rest of the encounter are unchanged.

Native datapack overrides still replace the vanilla entity loot table before it is captured. Global mob-loot disabling and already-canceled drop events retain their prior handling. Arbitrary third-party event cancellation, corrupted/externally rolled-back world saves and forced entity deletion are outside the normal payout guarantee. If a third-party mod rejects item spawning, those entries retry while the corpse remains loaded/alive as an entity; XP waits until item delivery completes.
