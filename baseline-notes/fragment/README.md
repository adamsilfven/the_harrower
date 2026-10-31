# Hollow Fragment standalone addon 1.0.1

Minecraft Java 1.21.1 • NeoForge 21.1.259+ • Java 21

Replace the previous Hollow Fragment addon JAR with `hollow-fragment-addon-1.0.1.jar`. Keep LC Core and its normal dependencies unchanged. Install the addon on clients and the server where applicable. Use `/give @s hollow_fragment_addon:hollow_fragment`.

The item stacks to 16 and displays the name Hollow Fragment with gray italic lore: “It weighs nothing. The ground beneath it disagrees.” No Cairn interaction, catalyst consumption, recipes, particles, or other gameplay changes are included.

Version 1.0.1 corrects the model's invalid empty elements and UV coordinate scale. All visible voxel positions, face layout, display transforms, and PNG texture bytes are preserved. See VALIDATION.md for the demonstrated failure and exact repair.

With Java 21 installed, run `./gradlew clean build` (Windows: `gradlew.bat clean build`). The Gradle 8.10.2 wrapper downloads dependencies on first use. Output: `build/libs/hollow-fragment-addon-1.0.1.jar`. Java auto-download is not configured.

Gradle `check`/`build` runs an actual Minecraft BlockModel parser regression check. It verifies that the original fixture fails with the empty-face exception and that the corrected model parses successfully. This runs without a graphical client; it does not test rendering. The original broken model exists only under test resources and is not packaged in the installable addon.
