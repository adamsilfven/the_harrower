# The Harrower

**Minecraft Java 1.21.1 · NeoForge 21.1.259 · Java 21**

The Harrower is an enormous, grounded stone horror with three escalating combat phases, crushing attacks, and a particular dislike of being summoned. He doesn't spawn naturally. If you want this fight, you have to invite him into your world.

One JAR contains the Harrower, Hollowforged equipment, Worldshard, Hollow Fragment, Hollow Cairn ritual, and Hollow Core. This is a standalone release.

## Install

Put `harrower-neoforge-1.21.1-1.0.0.jar` in your `mods` folder. Install the **same JAR on the server and every client**. Remove older or separate Harrower, Hollowforged, and Hollow Fragment JARs to prevent duplicate registrations. Do not extract the JAR into the mods folder.

## Summoning: the Hollow Cairn

Build a **flat 4 × 3 frame of ten Polished Deepslate blocks**, leaving the two central spaces empty. Both orientations work. The opening and the block of air above it must remain clear.

```text
Top view

P P P P
P . . P
P P P P

P = Polished Deepslate
. = empty space
```

Drop **one Hollow Fragment** into either opening. The ritual needs a living player who dropped the item, a non-Peaceful world, and open, accessible ground nearby with room for something considerably larger than you. If a valid spawn location cannot be found, the ritual won't proceed. The Cairn is reusable; every successful summoning takes another Fragment.

The opening turns black, souls rise, and the Harrower arrives nearby. He will not be pleased.

## The fight and its rewards

The Harrower has three combat phases, heavy melee attacks, area pressure, and a Grasp that can leave you wishing you'd built the Cairn a little farther from home. Bring your best gear and an escape plan.

Defeating him guarantees **one Hollow Core, 2–4 Enchanted Golden Apples, 4–8 Netherite Ingots, and 1,000–1,500 XP**. Independent bonus rolls can also produce enchanted weapons, Netherite armor, tools, Elytra, Totems of Undying, Nether Stars, a Heavy Core, and other valuable vanilla finds. Bonus rolls can succeed together. No Dragon Eggs or finished Hollowforged equipment drop directly from the boss.

The reward isn't paid out the moment his health reaches zero. The items and XP emerge from the remains during the existing death-and-crumble sequence, roughly 20.5 seconds after death. The exact loot weights and enchantments are in [VARIED_LOOT_SPECIFICATION.md](VARIED_LOOT_SPECIFICATION.md).

Use the Hollow Core to craft the **Crown of the Hollow**, **Carapace of the Unbroken**, **Greaves of the Stillborn**, **Treads of the Worldbreaker**, and **Worldshard**. The five crafting recipes are included in the mod.

## Building from source

The Gradle wrapper pins **Gradle 8.14.3**, **ModDevGradle 2.0.148**, and **NeoForge 21.1.259**. Use Java 21 and Python 3.

On Linux or macOS: `bash gradlew clean build`

On Windows: `gradlew.bat clean build`

The project packages the release JAR under `build/libs/`. To check it against the approved baseline, run:

```text
python3 scripts/verify_baseline.py build/libs/harrower-neoforge-1.21.1-1.0.0.jar
```

See [VALIDATION.md](VALIDATION.md) for the original release's verified test coverage and limits. Historical design and engineering notes are retained for provenance; they are not current player instructions.

## License

Harrower and Hollowforged are All Rights Reserved. The Hollow Fragment component retains its existing MIT terms. See [LICENSE_NOTES.md](LICENSE_NOTES.md).
