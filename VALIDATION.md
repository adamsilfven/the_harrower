# Harrower 1.0.0 release validation

Clean build succeeded with Java 21.0.6, Gradle 8.14.3, ModDevGradle 2.0.148 and NeoForge 21.1.259 for Minecraft 1.21.1. Cached dependencies were used offline.

The final installed JAR passed native dedicated-server validation: 119 package/ritual/loot checks and 294 equipment/crafting assertions. Loot validation includes 10,000 seeded native rolls, compatible enchantments, stack limits, reward ranges, delayed payout, pending/paid NBT serialization and duplicate prevention. See server-release.log and the assertion reports.

The final archive passes ZIP CRC checks. Entry comparison against the user-approved 0.6.2-rc.2 JAR proves every gameplay class, asset, recipe and loot resource is byte-identical. Only META-INF/neoforge.mods.toml changes, and META-INF/LICENSE_NOTES.md is added. The three internal registry namespaces remain lc_harrower, lc_hollowforged and hollow_fragment_addon. No LC Core file is modified or bundled.

The baseline's separate real scheduler test observed no early payout, payout at native death tick 410, and 20 subsequent ticks without duplication. Its log is included as baseline-server-natural-scheduler.log, explicitly inherited evidence for identical gameplay code; that scheduler probe was not repeated for the metadata promotion. The user reported successful in-game loot testing of that baseline.

No new rendered-client test is claimed. Full installed-LC integration, client visual alignment, multiplayer presentation and live restart/chunk unload remain gameplay checklist items. NBT reconstruction tests are narrower than a complete live server restart. Normal Minecraft save guarantees apply; no atomic crash/rollback guarantee is claimed. Persistent third-party item spawn cancellation or forced corpse removal can prevent delivery. See VALIDATION_0.6.2-rc.2.md for prior probe details.
