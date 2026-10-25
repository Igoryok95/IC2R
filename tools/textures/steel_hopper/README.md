# Steel hopper texture sources

The visual concept was approved on 2026-10-25. `approved-concept.png` is the design
reference, not an in-game screenshot. `source-atlas.png` was generated with the
built-in imagegen tool, using the approved concept and the existing IC2R steel
block texture as references. The exact atlas prompt is in [prompt.txt](prompt.txt).
`source-item.png` is the separately generated transparent inventory sprite, based
on the vanilla hopper icon alone, retaining its flat front-facing shape; its prompt is in [item-prompt.txt](item-prompt.txt).
No image-generation CLI or API key was used.

The atlas contains three adjacent square material tiles: outside, top and inside.
The compiler splits these tiles and samples each to native 32x32 pixels with nearest
neighbor sampling. It does not enlarge the vanilla 16x16 textures or generate game
geometry. Both block models inherit the original vanilla hopper geometry. The item
sprite is normalized to the vanilla icon bounds on a 32x32 canvas with binary alpha and inherits vanilla hopper item
transforms. Source images are retained so the game assets can be reproduced.

With Python 3 and Pillow installed:

```powershell
python tools/textures/build_steel_hopper.py
python tools/textures/build_steel_hopper.py --check
```

Outputs:

- `src/main/resources/assets/ic2/textures/block/transport/steel_hopper_outside.png`
- `src/main/resources/assets/ic2/textures/block/transport/steel_hopper_top.png`
- `src/main/resources/assets/ic2/textures/block/transport/steel_hopper_inside.png`
- `src/main/resources/assets/ic2/textures/item/steel_hopper.png`
