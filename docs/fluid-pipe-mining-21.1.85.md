# Fluid-pipe mining tools in 21.1.85

Bronze and steel fluid pipes were missing from `minecraft:mineable/pickaxe`.
Consequently pickaxes and electric drills treated them as unsupported blocks and
fell back to hand speed. This was unrelated to the corrected Addons fallback for
levers: a lever should retain hand speed, but metal pipes should support pickaxes.

All four sizes of both metals now belong to the shared vanilla tag. No item
code, hardness, harvest requirement, fluid transfer, pipe geometry, drops or EU
cost changes. Native drills and Addons tools obtain their existing effective
speed through that tag; no hardcoded item/mod dependency is added to core.

A native GameTest checks all eight variants with actual player mining speed:
hand 1, diamond pickaxe 8 and charged native diamond drill 16. An empty native
drill retains fallback 1; reading speed spends no EU, and the normal pipe drop
remains available. The paired Addons client pass checks both Advanced drills in
all three efficiency profiles against all eight pipe variants.

Validation completed with 34 core unit tests and all 588 required paired native
GameTests passing. The paired Addons 1.4.9 client checked all 48 Advanced drill
and pipe-profile combinations in the same RU/EN pass that verified the engine
information icon. Both builds succeeded; deployment is separate.
