# Deferred Classic restoration

The user requested that the tested foundation be saved on a separate development
branch and the remaining work be continued later. This document records that scope.
It is not a release announcement. Do not publish or install the incomplete 21.1.70
build as the finished Classic restoration.

## Saved foundation

The `classic-restoration-21.1.70` branch contains silver / Nether aluminium ores,
materials and their initial processing, the Rare Earth Extractor, Centrifugal Rare
Earth Extractor and Alloy Smelter, Solar Helmet and Static Boots, expanded batteries
and storage, ten cables, the IV Transformer, and native Electrolyzer item charging.
The core remains independent of IC2R Addons.

Storage menus retain their original energy gauge. Its tooltip shows exact grouped
EU; output fits without SI abbreviation. Rotor speed and Advanced Machines warm-up
use percentages. Dynamic XML tooltips resolve their live values and localization.

The companion Addons branch, `classic-compatibility-1.4.1`, adapts large monitoring
readings and equipment, and restores all eight solar panels with distinct recipes
and original 32x32 faces. See its `docs/classic-compatibility-1.4.1.md` for details.

## Remaining features

- [ ] All six villager professions: electrical engineer, nuclear engineer, crop
      farmer, demolition specialist, brewing master and Greg. Preserve original
      regular / zombie profession skins, workstations, profession names and trades.
- [ ] Complete brewing in the game: barrel registration, original model / textures,
      ingredients, water and containers, opening / tapping, serving, item data,
      placement / breaking persistence, drink effects, diagnostics, recipe-book
      automation and original comparator / fluid behavior. The tested
      `ClassicBrewState` currently provides internal composition and aging only.
- [ ] Rocket Miner: original missions, refueling, resource collection, inventory,
      progression, persistence and GUI.
- [ ] Additional generators and their original environmental / fuel requirements.
- [ ] Specialized machines and their required material-processing chains.
- [ ] Additional infrastructure, including the original tanks, adjustable
      transformer, crop libraries and other agreed late-game devices.
- [ ] All five Colossal machines: Furnace, Macerator, Extractor, Compressor and
      Recycler, including the original multiblock formation, inputs, per-operation
      progress, water / steam handling, recipes and state saving.
- [ ] Standalone Colossal recipes and conditional recipes using the existing
      Advanced Machines in IC2R Addons, with no Java dependency from the core.
- [ ] Complete worn-armor, profession-skin, texture, recipe, GUI and localization
      validation for all remaining content.

## Dependencies that still block survival progression

Registered content and working behavior do not imply that the entire late-game
crafting chain is already obtainable. Finish these original dependencies before a
release:

- Polished Gold requires the original Refinery and alcohol-processing route.
- Plasma, Plasma Core and their downstream battery / storage / cable recipes
  require the original Plasmafier and its liquid-UU adaptation.
- Colossal casings require their original component recipe and the completed
  multiblock progression. They are already referenced by the restored Photonic
  Solar Panel recipe.
- Reconcile every newly registered component against its original producer and
  consumer, including by-products, container returns and alternative recipes.
- Recheck XP handling for original processing machines where the reference awards
  it; the initial Alloy Smelter port does not yet model a recipe XP field.

The restored UESC / PESD, higher storage, cables and late solar recipes must be
audited through these dependencies. Do not replace missing original producers with
unapproved shortcut crafting recipes.

## Decisions already approved

- Use original Classic behavior and artwork; no generated replacement textures.
  Retain original-resolution villager skins. Existing 32x32 solar faces are restored
  unchanged; an optional 16-to-32 upscale was allowed where needed.
- Adapt former solid UU consumption to native liquid UU at the default equivalent
  of **7 mB per original solid UU item**. Preserve the original material chain and
  add its dependencies. Retain crafting routes alongside new ore generation.
- Extend the existing IC2R Electrolyzer instead of adding a duplicate Classic one.
- Scale battery / storage capacities relative to native IC2R's 10-million-EU
  Lapotron and 40-million-EU MFSU. Current values are documented in the foundation.
- Keep IC2R usable without Addons; integration recipes must be conditional.
- Light Absorbing / Photonic panels retain their original generation, with four /
  sixteen **32,768-EU packets per tick** instead of unsupported tier-7/8 voltage.
- Energy Control integrations remain IC2R-only; the existing optional BuildCraft
  engine and wrench bridge stays in Addons.
- Ask the user before changing other original mechanics or balance.

## Release and deployment gates

- [ ] Finish the original producers and all agreed features above.
- [ ] Update English / Russian text, README, changelog, mod descriptions, metadata
      and the complete recipe / asset provenance documentation.
- [ ] Build and test IC2R alone, Addons with IC2R, and the optional BC / Jade
      combinations. Verify actual English / Russian client GUIs and models.
- [ ] Publish only after the completed scope is validated. The current request is
      to push separate development branches, preserving `main` in both repositories.
- [ ] Then update the client, the configured exchange ZIP and live server. Save
      and gracefully stop the server first, back up, replace verified artifacts,
      restart, and verify fresh startup, versions and an actual status connection.

The detailed implementation checklist remains in
[the 21.1.70 restoration notes](classic-restoration-21.1.70.md).
