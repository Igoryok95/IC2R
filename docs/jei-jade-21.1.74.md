# JEI and Jade integration — 21.1.74

## Problems and changes

JEI previously assumed fixed player-inventory offsets for every DynamicContainer.
The Rotary Macerator menu has fewer slots than a native macerator: index 42 was
outside its 42-slot list. JEI retried the invalid transfer handler while drawing
recipes, producing repeated exceptions and severe client stutter.

Transfers now locate processable inputs and the player's actual inventory. Recipe
outputs and service slots are excluded. Blast Furnace and Ore Washing Plant capsule
slots no longer become extra recipe ingredients. Solid-canning recipes use the same
container-first ingredient ordering in the display and transfer target. The slot-grid
upper bound is exclusive. Native smelting transfers the processing ingredient;
JEI's decorative fuel slot is ignored, and an existing Iron Furnace fuel stack is
left in place. Induction transfer selects its less-filled processing input.

All registered JEI category identifiers are available to dynamic recipe buttons,
including Addons' custom Recycler and Molecular Transformer categories.

## Jade coverage

| Blocks | Server-owned data |
| --- | --- |
| Native EU machines and generators | Stored EU / capacity, tier, measured grid input/output, active state when supported |
| BatBox, CESU, MFE, MFSU and all four chargepads | The same EU data plus nominal output |
| Standard processing machines | Processing progress |
| Induction Furnace | Heat and progress |
| Thermal Centrifuge | Heat relative to the work target and progress |
| Blast Furnace | Heat and progress, without requiring an EU component |
| Reactors | Heat / maximum heat and EU output |

Energy, heat and progress use colored gauges with white labels. Values come from the
server; nominal storage output is labeled separately from measured network flow.
Jade's general fluid and inventory providers remain responsible for those contents.
Core providers filter the `ic2` namespace, so they do not duplicate Addons' engine,
solar or machine overlays. The data collector has no Jade API dependency.

## GUI and compatibility

Induction heat uses two decimal places, including `100.00%`; the value fits the
existing text column. Addons 1.4.4 uses the same precision. Runtime balance, recipes,
capacities and processing speeds are unchanged. New labels are localized in English
and Russian. JEI and Jade remain optional and are not bundled.

For the complete addon fix use core **21.1.74** together with **IC2R Addons 1.4.4**.
This source update does not install files into the player's client, exchange ZIP or
production server.

## Validation

- 24 JUnit tests and 491 standalone server GameTests passed, including actual menu
  roles, all eight storage variants, fractional heat and a heat-powered machine.
- A real isolated client exercised 25 menu/category combinations across both mods,
  all eight Advanced Machines, Molecular Transformer and Kit Assembler. It constructed
  962 recipe layouts, checked transfer simulation, performed real transfers and
  opened recipe lists. The original repeated JEI exception was absent.
- Jade gauges and fractional GUI text were inspected in the isolated client.
- Both core and addon GUIs opened in a separate client run without JEI, Jade or BC.

Machine-readable evidence: [21.1.74.json](validation/21.1.74.json).
