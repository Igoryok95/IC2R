# Readable fluid-pipe glass — 21.1.78

For subsequent frame, highlight and item-rendering corrections, see
[21.1.79](fluid-pipe-visuals-21.1.79.md).

## Cause and change

The glass generator assigned a full 0..16 texture rectangle to every greedy-meshed
face, regardless of its dimensions. A long window stretched the square glass sprite;
a one-pixel patch at an elbow compressed the same sprite into a tiny framed insert.
That insert was glass, rather than an extra structural metal piece.

Glass uses Minecraft's position-derived UV mapping, so coplanar elbow patches share
the same texture coordinates. The glazing uses the vanilla white stained-glass
surface with a subtle pale-blue
vertex tint and reduced alpha. It has one outward surface per wall and uses the
translucent render pass, avoiding stacked tint within the same thin wall. Small
three-pixel diagonal highlights come from the original vanilla clear-glass artwork.
They keep a fixed size at 32px density instead of stretching with each window.

The metal frame, disjoint shell geometry, texture assets, fluids, recipes, transfer
logic and saved IDs are unchanged. No raster assets or player-facing strings are added.

## Validation

- The build passes 27 JUnit tests. The new check parses all 256 connection models
  with Minecraft's actual model parser and verifies native UV coordinates and
  matching orientation on every glass patch. It also checks translucent rendering,
  non-opaque tint and fixed-size vanilla highlights.
- The existing coplanar-face checks still cover every size and connection state.
- An isolated development client displays all eight bronze/steel variants with
  actual water/lava and verifies synchronized fluid levels and emptying. Close
  views show the elbow both filled and empty.

![Empty glazed elbow, captured in Minecraft](images/glass-pipes-21.1.78/empty.png)

![The same glazed elbow with water, captured in Minecraft](images/glass-pipes-21.1.78/water.png)

The screenshots are captured in the isolated Minecraft development client. This
release operation does not install files in the production client, exchange ZIP or
server. See the [validation receipt](validation/21.1.78.json).
