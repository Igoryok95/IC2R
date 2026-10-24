# Classic restoration for IC2R 21.1.70

This is the implementation checklist for the requested Classic content restoration.
Unchecked entries are not implemented or released. The installed release remains
21.1.69 until the complete update has passed validation.

## Agreed behavior

- Restore all six Classic villager professions and complete barrel brewing.
- Restore the basic Solar Helmet and Static Boots.
- Restore the expanded electrical progression, Rocket Miner, additional generators,
  specialized machines, infrastructure and multiblock machines, including tanks and
  all five Colossal machines.
- Preserve obtainable material-processing recipes alongside new ore generation.
- Add the missing silver and Nether aluminium ore generation without replacing the
  existing IC2R tin, lead, uranium or rubber-tree generation.
- Adapt UU consumers to IC2R's liquid UU; retain the original material dependencies.
- Use original textures and skins. Do not generate replacement artwork.
- Keep all player-facing names, messages, tooltips and screens translatable in
  English and Russian.
- Obtain agreement before making changes to original mechanics or balance.
- Extend the existing IC2R fluid Electrolyzer with Classic item electrochemistry;
  do not add a duplicate Electrolyzer block. This adaptation was explicitly agreed.
- Scale new battery capacities against IC2R's 10-million-EU Lapotron Crystal and
  stationary storage capacities against its 40-million-EU MFSU. For example,
  Glowtronic Crystal holds 75 million EU rather than Classic's 7.5 million EU.
- The unamplified UU production costs are 7 million EU per Classic item and
  1 million EU per IC2R millibucket. Use 7 mB per former UU item at default balance.
- IC2R must remain fully usable without IC2R Addons. Integration recipes are
  conditional; standalone Colossal recipes use the corresponding basic machines
  and original intermediate component costs. Validate both mod combinations.

## Implementation and validation

- [x] Original asset provenance and dimensions recorded for the currently imported assets.
- [x] Silver and Nether aluminium ores, drops, tags and processing recipes.
- [ ] Rare-earth and aluminium processing; magnets, alloys and circuit dependencies.
- [ ] Solar Helmet and Static Boots, including worn textures and charging behavior.
- [ ] All six professions, original skins, workstations and trades.
- [ ] Complete barrel recipes, fermentation, aging, serving, effects and persistence.
- [ ] Expanded storage, transformers, cables and electrical components.
- [ ] Rocket Miner and its original mission, refueling and collection mechanics.
- [ ] Additional generators and their environmental / fuel requirements.
- [ ] Specialized machines and required intermediate processing.
- [ ] Additional infrastructure, including fluid tanks and required crop storage.
- [ ] All five Colossal machines and their original structure requirements.
- [ ] Original recipe dependencies on advanced machines supplied by IC2R Addons
      resolved without duplicating those machines or creating a circular dependency.
- [ ] Liquid-UU costs checked against the original energy and recipe requirements.
- [ ] English / Russian localization, format arguments and UTF-8 checks pass.
- [ ] Server GameTests, unit tests and complete build pass.
- [ ] Real client checks cover models, worn armor, villager trades and every new GUI,
      including progress bars, inventory positions, interaction and persistence.
- [ ] README, release notes, mod descriptions and validation receipts updated.
- [ ] Commit and push the tested source; publish only the completed release.
- [ ] Update client and reconcile every mod/config entry in the transfer ZIP.
- [ ] Save and gracefully stop the live server, back up, update, start and verify
      the new version, startup completion and an actual status connection.

## Reference

Primary behavior reference: IC2 Classic 1.19.2, version 2.1.3.4, original JAR
SHA-256 `f0a99733ef33491eca418831d681a79902250d341650f652d06f8b88d3627f83`.
The local reference files are under the ignored `build/villager-reference/` folder.
Original author copyrights and asset rights remain with their respective owners;
the fork's license does not relicense imported assets.

## Implemented foundation (not a release)

The current development branch contains the ore/material foundation, basic charging
armor, Rare Earth Extractor, Centrifugal Rare Earth Extractor and Alloy Smelter,
native Electrolyzer item electrochemistry, Glowtronic / UESC / PESD batteries,
ESU / ISU / PESU storage, ten cables and the IV Transformer. The remaining checklist
above still blocks publication and installation of 21.1.70.

New batteries store 75 / 175 / 500 million EU. Stationary storage holds 300 million /
2 billion / 4 billion EU. Exact storage readings use the extended long-valued API;
the legacy integer API remains binary compatible. Teleporter availability and cost
distribution avoid signed-integer overflow and preserve surplus energy.

The existing Electrolyzer keeps its water-to-gas process. Its added item slots charge
water cells and magnets for 20,000 EU above 70% of the machine's own EU buffer.
At tier 2, charged water cells recover 16,000 EU below 30%; unfinished charging and
recovery buffers persist. The machine reuses IC2R's existing charged-water-cell ID.

The top red bar on rare-earth machines is the original fractional-material accumulator.
Rotor speed is shown separately as text and a percentage in the original position.
Native upgrade slots are functional. JEI lists fractional processing outputs and item
electrochemistry without requiring Addons.

Storage menus retain the original charge gauge. Hovering it shows exact charge and
capacity with grouped digits, for example `1 250 000 000 / 4 000 000 000 EU`, in one
tooltip line. No abbreviated energy line is added above the player inventory.
Output is also shown without SI abbreviation (`32 768 EU/t` for PESU). Short
localized menu titles preserve the full item names. Real client checks cover slot
bounds, title widths and exact readings from 0 through 4 billion EU in both languages.

Imported images are copied unchanged, including original dimensions. Reproduce the
asset/model/material data with:

```powershell
python tools/restore_classic_assets.py <path-to-original-IC2Classic-1.19.2-2.1.3.4.jar>
```

The tool checks the input SHA-256 before writing assets and updates the provenance
manifest. Generated model geometry reuses native IC2R cable shapes.
