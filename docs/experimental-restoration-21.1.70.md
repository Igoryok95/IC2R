# Experimental restoration for IC2R 21.1.70

Development branch based on released 21.1.69. The broad Classic restoration is
cancelled. This document describes implemented source, not a published release or
an installed client, ZIP or multiplayer-server update.

## Included

- Preserve released furnace XP/collection, client-language messages, empty-input,
  crop-save and GUI texture fixes. Keep storage/teleporter arithmetic and live
  tooltip fixes. Native EU capacities and world generation remain unchanged.
- Use the Russian name **Компрессор** consistently with Addons.
- Restore Experimental beer and rum brewing, original ingredient ratios, aging,
  tapped/closed state, serving, drink effects and saved barrel contents. A barrel
  replaces **vanilla scaffolding**; no industrial scaffold item is introduced.
  Add water, wheat/hops for beer or sugar cane for rum, then use a treetap and an
  empty mug. Tapping pauses aging; removing the tap loses one litre, as originally.
- Restore the basic Solar Helmet and Static Boots. They charge worn electric
  chest equipment using Experimental solar/walking rules and provide no armor.
- Restore four sizes each of bronze/steel fluid pipes, LV/MV pump covers, side
  toggling with wrenches and cover removal with a crowbar. Pumps pull 32/128 mB/t.
  Capacities/rates follow Experimental; both native machines and NeoForge fluid
  capabilities work. Pipes do not generate EU or introduce another power system.
- Restore normal/sticky dynamite, placement/redstone activation and linked remote
  detonators. Links retain dimensions, avoid force-loading chunks and cannot be
  triggered twice. The point blast uses Experimental resistance/damage rules.
- Restore the 12-slot radioactive-material Containment Box, iodine tablets,
  MFSU Upgrade Kit and reusable 1,000 mB fluid cell. MFE/chargepad upgrades preserve
  energy, facing and inventory. Cells retain partial contents through native and
  NeoForge capabilities and work with dispensers/world fluids.
- Add all six professions using original Classic villager/zombie artwork,
  adapting trades and workstations to Experimental items. Villagers acquire jobs
  from the workstations below; this update does not add village houses or worldgen.
- Complete matching English/Russian names, tooltips, messages, recipes and models.

## Villager comparison

The Classic column describes specialized trade families, not an exact price copy.
IC2R trade pools cover levels 1-5 and use native items. High-tier goods may require
both emeralds and progression components. Crop seed bags include scanned stats.

| Profession | Classic specialization | IC2R implementation | Workstation |
| --- | --- | --- | --- |
| Electrical Engineer | Circuits, casings, advanced circuits, compressed solar and complex circuits | Circuits, casings, cable purchases, advanced circuits, native basic solar panel; no complex circuit | Iron Furnace |
| Nuclear Engineer | Cooling/vent/exchanger components, dense copper, enriched fuel, planner | Native coolant cells, vents/exchangers and upgrades, dense copper, containment box, iodine; no Classic fuel/planner | Nuclear Reactor |
| Crop Farmer | Seeds, fertilizer, Grin and overgrowth supplies | Native scanned crop seeds, fertilizer, Grin, hydration and Weed-EX | Crop-Matron |
| Demolition Man | Buys TNT/ITNT/nukes; sells scrap metal/gunpowder | Buys TNT/ITNT; sells gunpowder, dynamite, sticky dynamite and remote; no nuke purchase or scrap-metal item | Industrial TNT |
| Brewing Master | Buys beer and aged whisky | Sells barrels, mugs and hops; buys coffee ingredients/drinks, beer and mature rum; no whisky | Booze Barrel |
| Greg | Foam, EU meter, sprayer, tools/jetpacks, solid UU and special devices | Native foam, meter, sprayer, electric jetpack, diamond drill, chainsaw and electric wrench; no Classic devices/solid UU | Industrial Workbench |

IC2R Addons extends the Electrical Engineer with monitoring circuits, sharp plates,
superconducting covers and the first advanced solar panel; the Nuclear Engineer
with thermometers/monitors/panels; Greg with advanced tools and the Advanced Lappack.
Those entries are registered by Addons, never referenced from standalone IC2R.
The other three professions retain native trades.

## Optional Create processing

No Create classes are linked from IC2R. Without Create, its recipes, compatibility
pack and three zinc intermediates remain disabled. With Create installed:

- Raw zinc/ore produces **two existing Create crushed zinc**, and a raw block 18.
- Create crushed copper, iron, gold, lead, tin, silver, uranium and zinc use native
  washing/centrifuging routes. Ordinary metals also grind to native dust; uranium
  follows the existing nuclear route rather than inventing uranium dust.
- Zinc gains purified crushed ore, dust and tiny dust, using existing IC2R artwork.
  The other metals reuse native intermediates; no duplicate Create crushed ores.
- Smelting XP is data driven and collected by Iron, Electric and Induction Furnaces.
  Zinc gives 0.35 per processed item, preserving 0.7 after normal raw-ore doubling.
  Other supported metals retain native recipe XP and fractional persistence.
- A conditional built-in datapack corrects Create 6.0.10's old IC2 ingot/nugget IDs.
  Tin/silver/uranium splashing returns one native ingot instead of nine obsolete
  nuggets. Missing aluminium recipes are disabled; no Classic aluminium chain.
  User datapacks can override recipes with a higher selected priority.

No new ore is generated. Create owns zinc world generation. Unsupported third-party
metals are not silently converted into arbitrary IC2R materials.

## Optional Mekanism processing

Mekanism compatibility is enabled only with Mekanism installed and registers no
items or textures. It uses common material tags so raw items/blocks and unified
variants are recognized without requiring a particular displayed mod name.

- Osmium ore/raw material yields two existing Mekanism dirty dusts; raw blocks
  yield eighteen. Grinding or washing cleans them 1:1 into Mekanism dust. Ingot
  grinding is 1:1 and cannot produce a material multiplication loop.
- Clumps/dirty dust for iron, gold, copper, tin, lead, uranium and osmium work in
  the corresponding native grinding/cleaning stages. Chemical crystals, shards,
  slurry and gas requirements are not bypassed. Cleaning grants no extra metal.
- Tin/lead/uranium already use native common-tag ore/raw routes; tagged raw blocks
  now unpack consistently. Fluorite ore gives six gems, matching Mekanism, and
  gems grind 1:1 to its dust.
- With both mods installed, Create's crushed osmium can be ground, washed,
  centrifuged or smelted in IC2R. Its existing item is reused.
- Osmium dust retains Mekanism's **0.3 XP** per smelt, so two processed items
  preserve the raw material's **0.6 XP**. The same metadata reaches all three
  native furnaces and their existing XP collection/save mechanism.

See [the Mekanism manifest](mekanism-processing-compatibility.json). Compatibility
is checked with the installed Mekanism 10.7.19.85 and Almost Unified 1.4.2; it does
not change world generation or the original mods' chemical recipes.

## Sound and automation fixes

The native Music & Sounds screen includes a localized **IC2R** volume slider.
It saves the existing client-only `audio.volume` setting, scales machine/equipment
sounds and refreshes active loops. Muting and unmuting does not require restarting
the machine. Vanilla master/category levels still apply; other mods retain their
own volume. The narrow slider fills the free cell beside Voice/Speech, inside the native
scrollable options list for smaller windows.

Hoppers prioritize the less-filled independent Induction Furnace input. Empty
channels fill left/right in turn instead of filling one stack before the other.
Full or incompatible slots fall back to the other channel. The same ordering is
exposed through sided item capabilities, without changing manual slot assignments.
Other inspected advanced machines have either one process input or distinct-purpose
inputs; fuel, container, upgrade and output slots keep their original rules.

## Original assets and references

Mechanics/recipes: Experimental `2.8.222-ex112`; modern-port cross-check
`2.9.40-ex119`. Classic `2.1.3.4` supplies only original six-profession artwork and
trade-specialization reference. No Classic progression, PESU/UESC, Colossal machine,
new cable tier, electrochemistry or solid-UU mechanic is included.

The [asset manifest](experimental-assets-21.1.70.json) records original archive
hashes and individual asset provenance. Original 16px icons/block faces are enlarged
to 32px with nearest-neighbor sampling; no new artwork is generated. Original 64px
profession and armor sheets retain their resolution. Import scripts are in `tools/`.
Existing upstream copyright/asset notices continue to apply.

## Addons boundary

Addons keeps four solar tiers through Quantum and retired-panel migration. Its
percentage/monitoring/GUI/server fixes stay. Advanced Lappack remains 40 million EU;
Ultimate Lappack 100 million EU, 50,000 EU transfer, tier 4. Recipes and all other
released equipment balance remain unchanged. No former Classic capacity adaptation
or upper-panel restoration is carried into these branches.

## Validation

See `docs/validation/21.1.70.json` for commands, versions, pass counts and limits.
The standalone suite and separate Create processing checks avoid sending mod-network
packets to vanilla mock logins. Addons checks run with actual optional mod classes;
real client checks use a normal negotiated connection in an isolated test world.
Implementation was batched before validation; only affected failures are rechecked.
Neither repository's `main` is modified.

Build core with Java 21 using `gradlew build`. Before publication of 21.1.70, build
Addons against that local JAR using `-Pic2rJar=C:\GIT\IC2R\build\libs\ic2-neoforge-21.1.70.jar`.
