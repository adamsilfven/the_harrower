# LC Core compatibility audit — 0.4.0

Input: user's installed `leyline-convergence-core-0.9.3.8.jar`.
SHA-256: ef7b840b31891bc55806917bd464d0e887e64e419cf1dbeff81774ed106ec198

The existing optional integration gate hashes these complete class files. All three match the previously audited 0.9.3.7-balance.1 bytecode exactly:

| Class | SHA-256 |
|---|---|
| EndgameService | 953f12a66de3122bf18642c5fdc74e3203495eb9b6efaeedfd8f5bce4039148c |
| LandRules$Binding | e36eb5bcc9f1820520171d6842c2b2681599720fec443dd6659339822d9df8aa |
| LandService | 9f1a6717afd6b71edc6d3f43bb24e9e40d2b7c15da8ec8e828909dc8cbe5fe76 |

`javap` inspection also confirmed the referenced saved-data, registry, owner/color/dimension/position/leyline getters, Spark completion and refresh method signatures. The existing hash gate and adapter architecture are preserved; the diagnostic version label now includes 0.9.3.8. No installed Core file, registry, quest, skill, configuration or save was edited.

This is evidence of static API/injection compatibility, not a full-pack runtime test. The supplied Core requires Ars Nouveau, Iron's Spells, Occultism, Dynamic Trees, FTB Quests and Pufferfish Skills; those full pack dependencies were not present in the native test profile. Five-Leyline dynamic behavior therefore remains a pack acceptance check. Unknown/nonmatching Core bytecode continues to disable the adapter instead of injecting into an unreviewed build.
