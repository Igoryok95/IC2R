# Matter Generator backflow in 21.1.94

## Cause

The Matter Generator registered its tank with both input and output access, even
though its purpose is to produce UU matter. Receiver-directed pipes introduced in
21.1.93 therefore classified nearby generators as accepting endpoints. With fluid
ejectors installed, matter could circulate between the generators and their pipes
while a more distant Replicator remained empty.

The 21.1.93 tests used a real Replicator but injected matter into the pipes or
extracted it through pump covers. They did not cover generators with ejector
upgrades feeding an ordinary connected pipe face.

## Fix and compatibility

The generator's existing tank now has output-only external access on all faces.
It can still generate matter, fill cells internally, eject matter through upgrades
and supply extraction pumps. External fluid handlers cannot fill it. The fluid
filter, 8,000 mB capacity, energy costs, saved contents and NBT identifiers are
unchanged. Pipes can drain existing remnants toward the accepting Replicator
without rebuilding the line.

Other machine inputs and bidirectional storage tanks retain their existing access.
No models, GUI layout or player-facing text changed.

## Validation

Two new native regression cases failed against 21.1.93 before changing runtime code:

- The generator accepted an external simulated fill on its bottom face.
- Eight generators with real fluid ejectors failed to feed a raised Replicator
  through a 32-block small-bronze-pipe line during normal server ticks.

The tests verify all six external faces, generation, ejector behavior,
vertical transport, saved pipe remnants, complete delivery and conservation of the
total matter amount. Existing tests also cover internal cell filling, pump covers,
single-mB transport, branches and mixed fluids.

Validation completed on November 5, 2026:

- `spotlessApply build runGameTestServer` on NeoForge 21.1.234 succeeded: all
  34 unit tests and all 523 required GameTests passed.
- The multi-generator test delivered exactly 1,038 mB to the raised Replicator,
  leaving zero matter in the generators and pipe buffers after 200 normal ticks.
- Core 21.1.94 with IC2R Addons 1.4.12 on NeoForge 21.1.264 passed all 615
  required GameTests.
- Existing internal cell-filling, pump extraction and general routing tests passed.

All runtime tests used isolated dedicated servers. Production client, server and
ZIP were not modified during this fix.
