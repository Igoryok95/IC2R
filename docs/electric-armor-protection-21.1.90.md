# Electric armor protection — 21.1.90

## Cause and correction

All charged Nano and Quantum pieces previously used the same `ic2:charged_armor`
modifier ID. Attribute instances identify modifiers by ID, not by the equipment
slot that supplied them. One piece therefore replaced another's contribution.
Removing a piece or changing its charge could remove the shared modifier even
while other charged pieces remained equipped. This affected real armor points
and damage reduction as well as the HUD.

Each humanoid armor slot now uses its own `ic2:charged_armor/<slot>` ID. The
existing values remain helmet 3, chestplate 8, leggings 6 and boots 3: a fully
charged suit totals 20 points, displayed as ten full vanilla armor icons.
Recharging or using one piece updates only its contribution. Empty pieces retain
their existing charge-dependent protection behavior.

`ItemArmorElectric.getChargedArmorProtection()` exposes the piece's charged armor
points. Nano/Quantum subclasses supply the existing slot values. The base returns
zero, preserving unrelated electric equipment. Compatible Addons chestplates can
participate without IC2R depending on Addons.

EU per absorbed damage, the separate electric absorption formula, capacities,
durability, toughness, abilities and artwork are unchanged. No new GUI or locale
strings are needed.

## Validation

`ElectricArmorGameTests` covers native Nano, Quantum and mixed sets without
Addons installed. It exercises Minecraft's actual equipment transition rather
than summing item tooltips:

- Sequential equip/removal and four distinct protection modifier IDs.
- EU changes while pieces remain equipped, one-piece depletion and recharge at
  the protection threshold, and a mixed vanilla iron chestplate.
- Actual survival-player attacks: health loss matches electric absorption plus
  the full vanilla armor/toughness reduction, and absorption spends EU.
- Empty electric suits provide no charged armor points or free protection.

The paired Addons 1.4.12 tests cover upgraded/solar/mixed equipment and a real
server/client HUD sequence, including jumps, fall-energy use and partial removal.

Completed checks: 34 unit tests, all 510 required standalone GameTests, and a
successful build. The paired run passed all 601 required GameTests plus Addons'
20 unit tests. The actual client passed all sixteen synchronized states; native
HUD screenshots confirm full and partial armor values.
