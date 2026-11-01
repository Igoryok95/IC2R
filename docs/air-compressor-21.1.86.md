# Air Compressor in 21.1.86

In 21.1.87, the redundant status label below the receiver is removed in every
operating state. The GUI remains 212x190 and all other element coordinates are
preserved; production, delivery, inventory, controls and Jade are unchanged.

The ordinary Compressor produces air capsules, making a direct Blast Furnace
installation depend on repeated capsule handling. The separate Air Compressor
produces the same compressed-air fluid in a built-in 16,000 mB receiver. It supplies
adjacent fluid consumers and ordinary fluid pipes automatically, up to 32 mB/t
across its automatic outputs. External pumps can also extract from the receiver;
external filling is rejected. No Addons, BuildCraft or other mod is required.

The selected base rate is 2 EU/t and 1,000 mB per 300 ticks (3.33 mB/t). This keeps
the ordinary Compressor's 600 EU per 1,000 mB cost. Production is continuous;
fractional mB are retained across saves. The final part of a nearly full receiver
costs only its proportional EU; filling never exceeds capacity. Compression stops
without idle EU or working audio when the receiver is full, disabled or unpowered.
Stored air can still leave while compression is disabled. The local switch does
not emit redstone or disable adjacent hoppers.

Overclocker, Transformer and Energy Storage upgrades use the native IC2R upgrade
formulas. The receiver itself stays at 16,000 mB; Energy Storage upgrades affect
the electrical buffer. The base electrical tier is LV. GUI and Jade distinguish
actual EU consumption, air production and air delivery, averaged over 20 ticks.
External extraction is included in delivery. Base production supports three
continuously operating ordinary Blast Furnaces, which each consume 1 mB/t.

## Recipe

```text
P M P
  C
P T P
```

- P: iron plate (four total, shared plate tag)
- M: electric motor
- C: ordinary Compressor
- T: tiny bronze fluid pipe

## Native UI and artwork

The horizontal layout is defined by the normal IC2R dynamic GUI. Its background,
energy bolt, discharge slot, upgrade slots, inventory and information tooltip
are native widgets. A horizontal receiver gauge adds the shared native-slot
border convention. Lit/unlit vanilla torch artwork identifies the local switch.
All player text is localized in English and Russian, including JEI information.

The block reuses the original 32x32 Compressor front, active front, side, top and
bottom PNGs. No original PNG is overwritten or resampled. The dial is assembled
as model geometry sampling the original palette, with a round stepped rim and
separate scale marks. A client renderer moves its needle according to the live
receiver fill; the carried model has a resting needle.

## Validation

Native regression coverage checks base EU/air conservation, full/empty stopping,
partial production across saving, a saved local switch without redstone output,
direct and piped delivery, an extraction-only receiver, native upgrades, final-mB
capacity and actual Jade data. The opt-in client pass captures the real block
and horizontal GUI in RU/EN and checks text bounds and slot layout.

Completed validation: 34 unit tests and all 505 required native GameTests passed
in the standalone core. The real NeoForge 21.1.234 client with JEI and Jade passed
the RU/EN GUI checks, a client-to-server torch-button toggle, saved receiver
display and model captures. The final visual pass also verified a readable red
needle with face lighting and a single Jade receiver bar. No original PNG files
were changed. Deployment into an existing client, ZIP or server is separate.
