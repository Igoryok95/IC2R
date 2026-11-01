# Experimental machine artwork and Scanner layout in 21.1.84

## Cause and changes

IC2R inherited 16x16 machine artwork from its prior port, while Addons retained
the original 32x32 Advanced Machines Modern set. Those sets differed in material
detail, chassis design and pixel density.

468 PNGs are restored directly from the default modern assets in
`industrialcraft-2-2.8.222-ex112.jar` (IC2 Experimental 1.12.2). Original artwork
belongs to the IC2 team and its original artists. Files are copied byte for byte;
there is no resizing, generation, recoloring or interpolation.

| Group | Replaced PNGs |
| --- | ---: |
| Processing, fluid, mining, utility and UU machines | 249 |
| Electrical, heat and kinetic generators, including reactors | 129 |
| Energy stores, chargepads and transformers | 68 |
| Personal safes and trading machines | 15 |
| Nuke | 3 |
| Machine and advanced machine casings | 2 |
| Bronze tank | 2 |
| Total | 468 |

Existing asset paths and model UV coordinates remain. The Macerator's two block
models stop substituting the generic 16x16 side/bottom and use their own restored
original faces, including the active front and top. No GUI backgrounds, item
sprites, recipes or behavior are replaced by this texture import. Existing
custom fluid pipes and new storage-box designs are preserved.

The Iron Furnace, Luminator and mining-pipe sprites were 16x16 in the original
Experimental pack too; they retain those original dimensions. The Compact Item
Buffer already had its original 32x32 texture and needs no replacement.

The [complete plain-text list](texture-replacements-21.1.84.txt) names every changed
PNG. The [provenance manifest](experimental-machine-textures-21.1.84.json) maps each
destination to its exact source path inside the reference archive.

## Scanner headings

The common title was centered over the entire screen, while the result caption
started at a fixed x=123. Its Russian text reached outside the frame. A dedicated
title-rendering hook preserves the default for other GUIs. The Scanner centers
its title at x=57 over scanning and its result caption at x=134 over the result
screen. Both keep the existing y=6 baseline and existing localization keys.
No slot or control moves.

## Validation

The original PNGs are compared directly with the reference archive after copying.
The opt-in `-PexperimentalTexturesSmoke` client pass renders native and Addons
machines together, checks the real baked quads at all four rotations in both
active states, and captures the Scanner in English and Russian at GUI scales
1 through 4. It checks the actual font widths against each header's bounds.

Completed validation:

- 34 unit tests and the release build passed against the default NeoForge
  21.1.234 baseline (`build/21.1.84-build.log`).
- One real client pass on NeoForge 21.1.264 / JEI 19.64.0.455 with Addons 1.4.8
  checked 768 baked 32x32 quads: 16 displayed machines, two active states, four
  orientations and all six faces (`build/21.1.84-client.log`).
- All eight Scanner views (English/Russian, GUI scales 1-4) passed actual font
  bounds checks. The captured views at scales 2 and 4 were visually inspected.
- All 468 PNGs inside the built `ic2-neoforge-21.1.84.jar` match the original
  Experimental archive byte for byte and have dimensions 32x32.
- The plain-text replacement list contains exactly 468 asset paths.

The core source and resources are updated. This change does not install anything
into the user's client, exchange ZIP or server and does not change Addons.
