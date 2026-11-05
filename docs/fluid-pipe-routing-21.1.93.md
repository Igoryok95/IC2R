# Fluid-pipe routing in 21.1.93

## Cause

Passive integer averaging could stop on a descending staircase of fluid amounts.
For example, a pipe holding 1 mB beside an empty pipe calculated a zero transfer.
Long lines retained part of their contents even when a machine at the end could
accept them. This was especially visible with the Matter Generator's 1 mB batches.

## Changes

Pipes now find accepting fluid handlers through their loaded, open connections.
Fluid moves to adjacent pipes closer to a compatible receiver. Even a 1 mB batch
can traverse an empty line, and existing saved remnants drain without rebuilding
the pipes or adding more fluid. Flow still passes through individual pipe buffers.

Equal routes rotate their output priority so small batches can reach both branches.
Unused output shares are redistributed when another destination still has room.
Cycles cannot attract circulating fluid because a routed transfer decreases the
remaining distance. Contents in dead ends can flow back toward a reachable outlet.

Full receivers retain the fluid in pipe storage. Passive equalization remains
available where no receiver is reachable; opening a connection or making space in
a receiver allows the buffered fluid to continue. Different fluids and their data
components remain separated.

The original pipe capacities, pump extraction rates and per-profile output rates
are retained. The outgoing budget is shared across a pipe's destinations, including
the next pipe. Closed wrench connections and the one-way input faces of pump
covers are respected.

Topology is cached per connected component. Each fluid's reverse receiver search
is shared for the current world tick. Capability caches refresh machine ports;
placement, removal, side changes, covers and chunk lifecycle invalidate topology.
Traversal and connection rendering inspect loaded neighboring chunks only.

## Compatibility

Existing IDs, tank NBT, contents, capacities, covers, textures, Jade readings and
fluid rendering are retained. Routing data is transient and rebuilt after load.
The core has no BuildCraft, Addons, Jade or JEI requirement for this behavior.
No player-facing text was added.

## Validation

The native test suite adds cases for all eight profiles, a 40-block 1 mB line into
a real Replicator, uneven saved buffers, fair branches, a ring, a dead end, full
receivers, closed/reopened ports, shared output limits and a water/matter junction
feeding compatible machines. The long-line case also checks loaded chunk counts.

Validation completed on November 5, 2026:

- `spotlessApply build`: successful; all 34 unit tests passed.
- Standalone core on NeoForge 21.1.234: all 521 required GameTests passed.
- Core 21.1.93 with IC2R Addons 1.4.12 on NeoForge 21.1.264: all 613 required
  GameTests passed, including the routing cases.
- An isolated Minecraft client passed the existing eight-profile fluid-rendering
  smoke test, including real water/lava, emptying and synchronized measured flow.
  Full and empty elbow screenshots were also inspected.

The client, dedicated test servers and test worlds were separate from the user's
installed client and production server. No production mod or world was updated.
