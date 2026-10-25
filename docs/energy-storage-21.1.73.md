# Energy storage and charging fixes in 21.1.73

## Confirmed causes

- Both native Energy delegates accepted an entire injected amount and returned zero
  rejected EU, even when only part of the packet fitted. The grid also left a sink's
  remaining demand unchanged after a partial transfer, so later sources or packets
  could keep sending against the same old demand.
- The redstone discharge slot consumed a whole fuel item when its EU value exceeded
  the requested amount. Adding that whole portion could exceed the buffer capacity.
- ElectricItemManager edited CustomData through a shared mutable NBT reference before
  replacing the component. ItemStack copies used by menu change detection could see
  that mutation too, hiding the final charge update until the player moved the item.
  The BatPack charging slot itself can supply the exact final 100 EU.

## Storage audit and corrections

| Storage | Capacity, EU | Correction |
| --- | ---: | --- |
| BatBox / BatBox Chargepad | 40,000 | Bounded network intake, API setters and saved values |
| CESU / CESU Chargepad | 300,000 | Same shared correction |
| MFE / MFE Chargepad | 4,000,000 | Same shared correction |
| MFSU / MFSU Chargepad | 40,000,000 | Same shared correction |

The shared sink-only delegate used by machines also returns unaccepted EU. BasicSink,
BasicSinkSource and the public prefab stored-energy/capacity setters follow the same
bounds. Signed addEnergy withdrawals remain supported for teleporters, including
large double-backed stores. Ordinary API calls report the amount actually transferred.

Overfilled storage blocks are normalized to their capacity when loading; this repairs
the stored amount rather than masking it in the tooltip. The excess above capacity
is discarded once when repairing such a save. Negative values are normalized to zero.
Capacity reductions also bound the actual stored amount. Normal incoming EU that does
not fit remains at its source. Redstone fuel waits until its complete portion fits,
so it is neither consumed early nor partially discarded.

Raw component NBT is not globally clipped before machine upgrade capacities are
restored. The explicit forceAddEnergy path remains available for Matter Fabricator
scrap amplification, which legitimately accumulates processing credit beyond one
operation's threshold. This fix does not change that mechanic.

The Energy-O-Mat already bounds accepted energy by paid credit and remaining buffer
space. The AE2/FE adapter already reports actual acceptance. Addons solar panels,
Advanced Machines, native EU counters and the optional electric engine inherit the
shared Energy correction without requiring new item IDs or capacity changes.

## Electric items and synchronization

Charging and discharging now modify copied NBT before replacing CustomData.
Copied stacks and the menu's previous snapshots retain their original charge;
simulated transfers do not create or mutate custom data. The real charging rate
and BatPack capacity remain 100 EU per transfer and 60,000 EU respectively.

## Validation

Five new server regression tests cover all eight storage variants, fractional excess
packet rejection, repeated full-buffer rejection, legacy overflow repair, signed
withdrawals, redstone fuel boundaries, sink-only machine components, API prefabs,
capacity reductions, parallel sources and EU conservation. A real container listener
receives the final BatPack value without moving the item; copied-stack isolation and
simulation safety are checked for charging and discharging.

Results are recorded in [the validation receipt](validation/21.1.73.json). No new
player-facing strings are introduced; existing English and Russian localization applies.
