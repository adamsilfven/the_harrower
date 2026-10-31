# Harrower 0.5.1 verification

## Build and source

Gradle build passed on Java 21 / NeoForge 21.1.259 using cached Minecraft artifacts. Compilation, resources, runtime and source JAR packaging completed. There are two existing annotation deprecation warnings and no automated Java unit test cases.

Existing model-source and strike-coordinate validators pass: 471 elements, 19 bones and 21 clips; valid UUIDs, hierarchy, UVs, animation keys, textures and all 13 strike-envelope tracks. The mirrored Grasp grip exports cover 347 frames per hand and raise the socket over five blocks above its held height before release.

Runtime geometry, texture PNGs, sounds and death-support data are byte-identical to 0.5.0. Only the two Grasp clips and their envelope/socket exports change among attack resources. Java changes are limited to Grasp timing/damage/hold/release, exact grip transforms, passenger attachment and standing riding pose. The passenger attachment getter uses the same hand socket as the rider placement.

## Native checks

The final packaged JAR is checked by hash against both native client and server installations. The isolated test uses Minecraft 1.21.1 / NeoForge 21.1.259. Captures show a real player in the right hand and a living humanoid test victim in the left. HUD is hidden; all screenshots are actual native game frames.

The right-hand test captures a player with 100 HP, no armor and natural regeneration disabled. Five successful 8-HP pulses occur at animation ticks 86, 136, 186, 236 and 286. Capture is tick 36. Release is tick 306, 270 ticks (13.5 seconds) after capture, following twelve seconds of pickup/holding and a 1.5-second raise. Release height is about 10.19 blocks on flat ground. The player lands with 53 HP: 40 HP from crush pulses and 7 HP from normal fall damage, with no extra direct-damage finisher.

The mirrored left-hand test uses a synthetic White leyline bridge and an unarmored humanoid victim with 100 HP. All five pulses report 6.8 HP and the same clock timing; the final drop executes at tick 306. This exercises the real outgoing damage and armor pipeline. It is not a test against the current Core JAR.

Additional native scenarios verify that a missed grasp recovers without the long hold; a synthetic server teammate providing 40 HP at the received-damage seam triggers rescue without a damaging drop; and reset/boss death release a captured victim to ground. These checks exercise the release paths. They do not replace a full multiplayer test of the teammate damage event path. Victim-death and logout emergency hooks remain wired as before.

Build log, native logs, screenshot originals, audit output and the test-only helper source accompany the release. The helper is not included in runtime/mods and is not a dependency.

## Scope

Full-pack gameplay, the original arena, camera comfort with the user's equipment, live Core integration and uneven-ground release remain user checks. No shoreline, pathing or death/rubble adjustments were made in this pass.
