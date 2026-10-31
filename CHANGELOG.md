# 1.0.0 — public release

- Promote the approved 0.6.2-rc.2 gameplay without balance, recipe, combat, ritual or loot changes.
- Set Harrower version to 1.0.0 and replace development-facing component descriptions.
- Include the existing component license notes in the runtime JAR.
- Repeat clean compilation and installed-JAR server validation; verify byte-for-byte preservation against the approved build.

# 0.6.2-rc.2 — varied rewards and crumble-time delivery

- Preserve the guaranteed Core, 2–4 Enchanted Golden Apples, 4–8 Netherite Ingots, 1,000–1,500 XP, and single death-time Leyline bonus roll.
- Keep the previous bonus-roll chances/quantities, broadening the 65% weapon slot to six equally weighted families and the 45% tool slot to specialist Netherite tools. Add curated, mutually compatible weapon/armor variants, including occasional Thorns III armor. Add one independent 35% rare-find roll for vanilla templates or max-level desirable books. Exact weights/loadouts are in VARIED_LOOT_SPECIFICATION.md.
- Add DelayedDeathLoot: capture full rolled item-entity NBT and XP at death; deliver at the existing crumble's death tick 410. No early payout, reroll at release, timer reset on normal load, or repeat payout. Successful item insertions are removed from escrow immediately; blocked insertions retry during the remaining corpse lifetime. XP is paid once after item delivery completes.
- Release from the rubble center on the existing terrain, using the existing forward-offset pile location. No animation, boss AI, damage, movement, summoning, recipe, equipment ability, Core texture/model, or LC Core changes.
- Runtime changes from the previous JAR are limited to EncounterRewards (replace immediate XP/drop delivery with escrow), the native entity loot table, version metadata, and the new DelayedDeathLoot class. CombatChecks changes only its opt-in reward fixture. All other previous compiled classes/resources are preserved byte-for-byte.
- Add 10,000-roll variety/enchantment validation, serialized pending/paid-state checks, exact pre/post-crumble item and XP checks, and a real server-scheduler test through tick 410. Test probes do not ship.

# 0.6.2-rc.1 — complete Hollow Core and locked endgame loot

## Hollow Core

- Preserve `lc_hollowforged:hollow_core`, Epic rarity, stack limit 64, and existing saved stacks.
- Replace only the old Core texture/model with the approved hollow-heart icon (16×16 RGBA) and a 110-pixel shallow stone relief. Add explicit first-/third-person transforms for both hands, a flat 90° ground transform, fixed/frame and head transforms, and a separate generated GUI/hotbar model.
- Add `HollowCoreItem` tooltip via two translation keys. Keep existing item registration under its original ID.
- Add five vanilla recipe-unlock advancements. All five actual Hollowforged recipe JSONs remain byte-for-byte identical to rc.3; center-Core requirement and native recipe behavior are maintained.

## Loot

- New native Harrower entity loot table: guaranteed 2–4 Enchanted Golden Apples and 4–8 Netherite Ingots; eight independent bonus pools at the locked chances and quantities.
- Sword: Sharpness V, Looting III, Unbreaking III, Mending, Fire Aspect II.
- All armor: Protection IV, Unbreaking III, Mending. Equal 25% slot weighting. Helmet adds Respiration III/Aqua Affinity; leggings add Swift Sneak III; boots add Feather Falling IV/Depth Strider III/Soul Speed III; chestplate has the base set. No incompatible Frost Walker combination or arbitrary curses.
- Pickaxes: Efficiency V, Unbreaking III, Mending; equal Fortune III/Silk Touch I selection, never both.
- Elytra is vanilla and unenchanted as specified. Stars, Totems and Heavy Cores are vanilla. No Dragon Eggs, Hollowforged equipment drops or additional custom items.
- Replace the old legitimate-hook/200-tick restriction with one guaranteed Core for every normal Harrower death. One additional-Core chance uses 5% per active qualifying color at the death instant, maximum 25%. Snapshot and payout guards persist in entity NBT.
- Add a fresh, read-only optional LC land/Spark query with audited API fingerprints. Credited player/tamed owner takes priority, then summoner; distinct owned active colors within the existing 64-block radius count. No Core modification or hard LC dependency.
- Award one guarded total of 1,000–1,500 XP alongside normal drops. Original boss native XP reward is zero. Respect global mob-loot rule/drop cancellation. No Luck/Looting scaling of default listed rewards.

## Exact scope

Existing modified runtime classes: `Hollowforged` (register the Core subclass), `EncounterRewards` (new locked death rewards), and its nested `Color` class (source/debug metadata). `CombatChecks` changes only the opt-in regression fixture to reflect the superseded reward rule. Existing modified resources: Core model, Core texture, English tooltip translations, and version metadata. New classes/resources implement the Core tooltip, optional loot query, native table and recipe discovery.

Every rc.3 boss combat, movement, animation, sound, summoning, Fragment, gear ability/asset and actual recipe entry is preserved. LC Core is unchanged. Internal versions: Harrower 0.6.2-rc.1; Hollowforged 1.0.2; Hollow Fragment 1.0.1. No separate addon JAR.
