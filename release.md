# 21.1.95

- Restore axe-style right-click wood stripping for the Electric Chainsaw and the
  inherited Advanced Chainsaw in IC2R Addons, for 100 EU per successful action.
- Preserve native stripping mappings, log orientation, tool events, mode shortcuts
  and offhand/shield behavior. Rejected actions do not spend operation energy.
- Let Mining, Diamond and Iridium Drills create dirt paths using their normal
  operation cost: 50 / 80 / 800 EU. Follow native shovel clearance and face rules.
- Provide the shared path action used by Advanced Drills in IC2R Addons 1.4.13.

## Also included since the previous public release, 21.1.82

- Deliver buffered fluid and individual 1 mB batches to accepting machines without
  trapping remnants in pipes; keep Matter Generator fluid connections output-only
  so produced matter reaches the Replicator instead of returning to generators.
- Keep jetpack hover mode selected after landing and avoid repeated toggles while
  a key combination is held. Powered jetpacks cooperate with the survival-server
  floating watchdog without enabling creative flight.
- Keep charged Nano/Quantum armor protection independent for each equipment slot
  and preserve the correct full-set armor points through energy changes.
- Use one villager per industrial workstation and migrate existing IC2 job
  reservations while retaining vanilla village entries and workshop weights.
- Add the Air Compressor, its 16,000 mB receiver and direct compressed-air supply
  to Blast Furnaces and fluid pipes; clean up its native GUI status area.
- Restore original Experimental 32x32 machine textures and align Scanner headers.
- Correct Luminator targeting bounds and normal pickaxe/drill mining of fluid pipes.
- Separate actual heat delivery from EU readings and restore familiar Russian
  IC2 names with localized JEI aliases.

## Validation and compatibility

The current source passed 34 core unit tests, 532 standalone GameTests, 20 Addons
unit tests and 627 combined GameTests. GitHub paired validation also passed,
including the optional BuildCraft/Jade build. The release is built with NeoForge
21.1.264, matching the paired Addons runtime.

For Advanced Drill path actions, use IC2R Addons 1.4.13 with IC2R 21.1.95+.

See [chainsaw behavior](docs/chainsaw-stripping.md) and
[drill paths and compatibility](docs/drill-paths-21.1.95.md).
