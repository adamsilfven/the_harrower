# The Harrower 1.0.0

Minecraft Java 1.21.1 • NeoForge 21.1.259 • Java 21.

ONE installable consolidated JAR includes Harrower, Hollowforged, Hollow Fragment, the Hollow Cairn ritual, the approved stone-heart Hollow Core and the locked endgame loot. The three original internal mod IDs remain unchanged. LC Core is optional and is not modified.

## Install

Close Minecraft. Replace the previous combined Harrower JAR with `harrower-neoforge-1.21.1-1.0.0.jar` on server and clients. Remove separate/older Harrower, Hollowforged and Hollow Fragment runtime JARs to prevent duplicate mod IDs. Keep LC Core and unrelated mods unchanged. The three component mod-list entries all belong to this one JAR.

## Hollow Core

Existing registry ID: `lc_hollowforged:hollow_core`. Existing stacks and all five crafting recipes remain valid. The approved hollow-heart art is used as a true 16×16 RGBA inventory/hotbar texture, with a shallow 110-element stone relief for held/world views. Both hands have explicit first/third-person transforms based on vanilla item placement. Dropped items use a 90-degree X rotation to lie flat. Item frames have an explicit fixed transform. The tooltip describes the heart's origin and its crafting purpose. Rarity remains Epic; stack limit remains 64.

The existing recipes for Crown, Carapace, Greaves, Treads and Worldshard are unchanged byte-for-byte and each consumes one Core in its existing center slot. Obtaining a Core unlocks those existing recipes in the vanilla recipe book. No new Core-crafting recipe or new item ID is introduced.

## Locked loot

Every normal Harrower death awards one Core, 2–4 Enchanted Golden Apples, 4–8 Netherite Ingots, and 1,000–1,500 total XP. Nine independent bonus rolls may succeed together: curated weapon 65%; Netherite armor 80%; additional apples 60%; Nether Star 50%; Elytra 35%; Totems 75%; vanilla Heavy Core 40%; specialist Netherite tool 45%; rare vanilla find 35%. Weapon families are sword, axe, mace, trident, bow and crossbow. Armor has utility variants and occasional Thorns III; tools include Fortune/Silk Touch pickaxes, axes and shovels. Rare finds include six armor trim templates, Netherite Upgrade templates and four desirable enchanted books. Current weights, quantities and exact enchantments are in `VARIED_LOOT_SPECIFICATION.md`. The older `LOOT_SPECIFICATION.md` is retained as historical input and is superseded by the user-approved variety changes. No Dragon Eggs, Hollowforged gear drops or new custom loot.

Vanilla drops use the native entity table `data/lc_harrower/loot_table/entities/harrower.json`; packs can override that table normally. The guaranteed Core and fixed XP total use a server-side guarded reward hook. All item rolls, enchantments and XP are committed at death, then persisted on the corpse. Nothing is paid immediately: items and XP spill from the remains at death tick 410 (about 20.5 seconds), during the existing crumble phase. Saving/unloading pauses the corpse normally; loading preserves its death clock and exact pending rewards. No terrain or death-animation changes are made. The boss's native XP reward remains its original zero, avoiding double-counting. Default chances have no Luck/Looting multipliers. Global `doMobLoot=false` and canceled drop events are respected.

## Optional Leyline bonus

One extra-Core roll is made at death: 5% per qualifying active color, capped at 25%. Qualification uses distinct active Leylines owned by the credited killer, within 64 blocks of the death position in the same dimension. A tamed attacker credits its owner. If no player owner is credited, the recorded summoner is used. Other players' Leylines do not count. This follows the existing personal-ownership/radius semantics; it is not a count of every player's monuments in the world.

The count is sampled at the death event, saved in boss NBT, and never inferred from summon-time state or historical suppression. Later activation/removal does not change that death's roll. Online ownership uses the audited land access/advancement/structure APIs; offline owners use the audited Spark completion API and persisted advancement state. Missing/unrecognized optional APIs fail closed for the bonus while preserving the guaranteed Core. The supplied 0.9.4.5-journal.3 land/Spark APIs were inspected; the land classes match the previously audited land bytecode. This read-only loot query does not alter the existing combat adapter or inject new Core mixins.

Without LC installed, one Core remains guaranteed and the bonus is zero. Full installed-LC gameplay is still a separate client/server verification item; API/bridge-contract validation is not claimed as an installed-pack test.

## Build

Install Java 21 and Python 3. Run `bash scripts/build.sh`, or `scripts\build.cmd` on Windows with `python3` available. Gradle 8.14.3, ModDevGradle 2.0.148 and NeoForge 21.1.259 are pinned. The wrapper compiles/tests, independently packages the final JAR and checks every unrelated entry against the actual rc.3 baseline. Output is under `build/libs`.

`src/validation` contains non-shipping integration probes. Put the final JAR in `run-verify/mods`, accept Minecraft's EULA for a disposable local test server, and run `./gradlew -PbinaryValidation runVerify`. For the actual server-scheduler delay check, follow with `./gradlew -PbinaryValidation -PdelayedValidation runVerify`. It edits an arena, runs checks against the installed binary and stops. Never use this probe on a real world. Require explicit pass markers; exit status alone is insufficient.

Model previews are rendered from the actual JSON geometry, not Minecraft screenshots. See `VALIDATION.md` and `TESTING_CHECKLIST.md` for actual evidence and remaining client checks.
