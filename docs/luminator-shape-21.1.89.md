# Luminator targeting bounds in 21.1.89

The old directional AABB calculation started each perpendicular axis at
0.46875 instead of zero. The normal axis could also extend beyond the block.
As a result, the selection outline covered only a shifted part of the visible
lamp, and targeting its remaining surface could select the supporting block.

Bounds now cover the complete mounting face, with the original model's 1/16
block depth against the support. All six orientations use the same convention
as the rotated block models. The outline is identical when lit and unlit.
The existing empty physical collision shape is preserved. The contact bounds
used for the lamp's existing monster interaction also follow the repaired shape.

Regression coverage checks all six orientations in both active states, including
block-local bounds, full face dimensions, empty physical collision and rays into
all four visible corners. The client pass compares actual baked-model vertices
against the targeting shape and captures ceiling-mounted selection in the game.

Completed validation:

- The release build and all 34 unit tests passed.
- All 507 required native GameTests passed. Shape checks include all twelve
  facing/active combinations and 48 rays into the four corners.
- The real client confirmed that all twelve baked-model bounds match the world
  outline, then captured a selected ceiling lamp with its complete outline.
- A powered ceiling lamp produces level 15 in the actual server light engine;
  the adjacent air block receives level 14. Removing the control signal returns
  its emission to zero.

The original Experimental 2.8.222 luminator's `getLightValue()` also returns 15
when active and zero otherwise. Vanilla light output has not been reduced or
increased in this fix. Shader bloom, surface emission and colored lighting are
separate from that vanilla light level; their appearance depends on shader rules.
