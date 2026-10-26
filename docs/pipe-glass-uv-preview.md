# Pipe glass UV preview

Status: unreleased, awaiting visual review. Stable `main` and installed clients remain
on 21.1.77 while this change is prepared on `fix/pipe-glass-uv`.

## Cause and change

The glass generator assigned a full 0..16 texture rectangle to every greedy-meshed
face, regardless of its dimensions. A long window stretched the square glass sprite;
a one-pixel patch at an elbow compressed the same sprite into a tiny framed insert.
That insert was glass, rather than an extra structural metal piece.

Glass now uses Minecraft's position-derived UV mapping. Every face samples the
existing vanilla texture at native model scale. Coplanar elbow patches share the
same coordinates, so their boundaries do not restart the glass border or highlights.
The metal frame, disjoint shell geometry, texture assets, fluids, recipes, transfer
logic and saved IDs are unchanged. No new player-facing strings are introduced.

## Validation

- The build passes 27 JUnit tests. The new check parses all 256 connection models
  with Minecraft's actual model parser and verifies native UV coordinates and
  matching orientation on every glass patch.
- The existing coplanar-face checks still cover every size and connection state.
- An isolated development client displays all eight bronze/steel variants with
  actual water/lava and verifies synchronized fluid levels and emptying. Close
  views show the elbow both filled and empty.

![Empty elbow, captured in Minecraft](images/pipe-glass-uv-preview/empty.png)

![The same elbow with water, captured in Minecraft](images/pipe-glass-uv-preview/water.png)

These are development-client screenshots. This preview has not been installed in
the production client, exchange ZIP or server, and no version has been published.
