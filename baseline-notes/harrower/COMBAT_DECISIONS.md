# Combat implementation decisions — October 25, 2026

Source specification: HARROWER_COMBAT_IMPLEMENTATION_HANDOFF.md (uploaded October 25).

User selected in this chat:
- Grasp rescue requires 40 HP actual received damage from teammates during the extended Grasp hold. Damage from the captured player does not count.
- Summoner plus explicit opt-in participants before first damage; freeze health scaling at that point. Preserve progress across wipes/disconnects. Reset only by explicit command.

Engineering interpretation for review:
- No additional hard arena wall or forced player teleport. Approved 64-block follow range controls acquisition; an active encounter waits when nobody eligible is in range. No automatic health restoration.
- Approved movement attribute 0.26 is retained; a gait multiplier controls actual advance speed to preserve the heavy motion pass. Green applies dynamically after that multiplier.
- White's obsolete anti-healing-reduction clause remains a no-op. No healing debuff will be invented.

Integration evidence:
The real Core archive `leyline-convergence-core-0.9.3.7-balance.1.jar` was located and audited, including EndgameService, LandService, LandRegistry, Binding, installation ownership, progression checks, and offline advancement loading. A fingerprint-gated adapter is implemented without replacing Core. Full-pack runtime validation remains separate from standalone Harrower tests; see COMPATIBILITY_AUDIT.md.
