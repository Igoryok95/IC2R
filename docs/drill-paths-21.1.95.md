# Drill paths in 21.1.95

The drills' mining tags provided shovel digging speed without the shovel's
right-click path action. Mining, Diamond and Iridium Drills now expose
`SHOVEL_FLATTEN` and use NeoForge's native block-modification hook.

One right click modifies one block. It costs the drill's normal mining operation:
50 EU for the Mining Drill, 80 EU for the Diamond Drill and 800 EU for the Iridium
Drill. These are existing tool costs, not a new balance setting.

Grass blocks, dirt, podzol, coarse dirt, mycelium and rooted dirt follow vanilla
shovel behavior. Compatible modded block mappings and event cancellation are
respected. The space above must be air, the bottom face cannot flatten, and an
unsupported/already flattened block costs no operation energy. The IC2 mode and
extra keys take priority.

IC2R Addons 1.4.13 reuses the shared `ItemElectricTool.flattenPath` implementation
for Advanced Diamond and Advanced Iridium Drills. Their current efficiency mode
determines the cost; a 3x3 mining mode still makes only one path block per click.
Pathing takes priority on suitable ground. On other blocks, the existing torch
placement fallback remains available. Vajra keeps its existing right-click
behavior. The new addon therefore requires IC2R 21.1.95 or later in the 21.x line.

Server regression coverage uses native survival interactions for all three
standard drills and both advanced drills, soil types, faces, charge thresholds,
mode shortcuts, tool-event cancellation/custom results, selected efficiency
costs, single-block behavior and torch fallback.

The completed checks passed 34 core unit tests, 532 standalone server GameTests,
20 Addons unit tests and 627 combined required server GameTests. The server suites
ran once after the full drill-path implementation; no gameplay world was used.
