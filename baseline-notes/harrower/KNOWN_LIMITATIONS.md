# Scope and remaining work — 0.5.1

This update changes Grasp and its rider positioning. The approved model, textures, other attacks, movement and death/rubble resources are retained from 0.5.0. Full Leyline Convergence modpack play, the original arena, shoreline and uneven-ground release remain user checks.

Native tests use an isolated Minecraft 1.21.1 / NeoForge 21.1.259 client and server. A test-only addon supplies a synthetic White leyline adapter and rescuer for controlled checks. That proves the Grasp suppression/release code paths, not compatibility with the current Core build or a full multiplayer rescue encounter. The addon is not part of the runtime JAR.

Grasp's five pulses total 40 raw HP before armor and mitigation, followed by normal fall damage. Armor, health, fall protection and other mods can change the result. The drop is real gravity, so nearby terrain can shorten the fall. Rescue/emergency release still search for safe ground around the original capture area.

The existing optional Core adapter was last audited against Core 0.9.3.8. Current Core integration was not re-audited. Actual Hollow Core/Hollowforged reward drops remain pending; no placeholder loot is added.

The corpse retains a rigid terrain fit and can bridge abrupt terrain changes. Its individual fragments settle independently. These existing behaviors are outside the Grasp change.
