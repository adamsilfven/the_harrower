# 0.5.1 — crushing Grasp

- Replace the brief hold/40-HP finisher with a twelve-second pickup-and-hold, five 8-HP crush pulses every 2.5 seconds, and a final 1.5-second raise.
- Release at the raised hand and let normal gravity/fall damage take over.
- Follow exact hand-space torso sockets; remove the forward seat offset and seated riding pose.
- Add restrained wrist squeeze motions and low stone-grinding cues.
- Preserve teammate rescue and emergency release, and skip the long hold on a miss or lethal pulse.
- Keep the approved model, textures, other attacks, movement and death/rubble unchanged.

# 0.5.0 — mountain stone refinement

- Add 16 tapered shoulder/upper-back shards, including the ridge behind the head.
- Bevel 36 larger existing stones without changing the face or texture images.
- Support editable Blockbench mesh geometry in the exporter and native renderer.
- Use actual mesh vertices for fragment floor contact.
- Retain all animation tracks, combat, sounds, movement and accepted 0.4.9 death/rubble behavior.
- Verify native client/server rendering and include thirteen actual Minecraft captures.

# 0.4.9

- Lower the prone hips and settle the shins/feet without changing cube dimensions or flattening the body.
- Fit the resting corpse against the full body footprint; account for entity scale.
- Replace 0.3-block shrink-in-place with terrain-aware falling stone pieces and floor-anchored shrink.
- Extend the body rendering window to let the last delayed pieces finish landing. Keep the approved rubble and fade.
- Preserve Hollowing, combat balance, other animations and 0.4.8 bank recovery.
- Add native client death-stage evidence, ascent/descent regression results and production-helper landing checks.

# 0.4.8

- Replace whole-body vertical compression with individual, uniformly shrinking corpse fragments that tumble and move downward.
- Preserve the fitted ground pose, pile, fade and approved Hollowing.
- Monitor pursuit progress, create turning room with a controlled dry backstep, and retain recovery paths while they make progress.
- Make path-planning bounds consistent; retain physical collision and combat values.
