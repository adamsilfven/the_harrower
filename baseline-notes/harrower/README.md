# Harrower 0.5.1 — crushing Grasp

Minecraft Java 1.21.1 · NeoForge 21.1.259 · Java 21.

Grasp now captures at 1.8 seconds, lifts the victim into the fist and keeps them captive for twelve seconds before a final 1.5-second raise and release. Five crush pulses land every 2.5 seconds after capture. Each pulse deals 8 raw HP (4 hearts) before armor and other mitigation; White leyline suppression reduces that to 6.8 raw HP. This redistributes the previous 40 raw HP finisher across the five pulses. The final release adds a real fall, with normal Minecraft fall damage and protection.

The victim's torso follows a socket exported from the actual hand animation. The old 1.6-block forward rider offset and seated riding pose are removed. Their head and shoulders remain exposed above the grip. Both hands use mirrored animation and socket data. Each squeeze has a small wrist contraction and low stone-grinding sound.

The final drop releases at the raised hand instead of teleporting the player back to their original ground position. Teammate rescue still requires 40 HP of actual damage; captured-player damage does not count. Rescue lowers safely, and boss death, reset, logout or victim death releases the captive safely. A missed grab skips the extended hold.

The approved 0.5.0 rocky model, face, textures, other attacks, suppression rules, movement and accepted death/rubble sequence are retained. Actual reward drops remain pending. The existing optional Core adapter was audited against Core 0.9.3.8; current Core compatibility was not re-audited in this pass.

See INSTALL.txt, TEST_REPORT.md and TEST_CHECKLIST.md. Editable source and Blockbench model are included. The free-format .bbmodel is authoritative for faceted geometry; the Bedrock geometry file contains bounding boxes only.
