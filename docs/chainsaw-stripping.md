# Electric chainsaw stripping

## Cause

The chainsaw inherited `DiggerItem` through `ItemElectricTool`, rather than
`AxeItem`. Its axe-mining tag enabled fast wood harvesting but did not implement
the axe's right-click stripping action. The Advanced Chainsaw in IC2R Addons
inherited the same omission.

## Behavior

- A successful strip uses one normal chainsaw operation: 100 EU.
- The implementation calls NeoForge's `AXE_STRIP` block modification hook. It
  follows native strippable mappings, including compatible modded wood, rather
  than keeping a separate hardcoded list.
- The stripped state retains the properties supplied by the native hook, including
  the log's axis. Normal block-change events and the item-use advancement trigger
  are emitted.
- The IC2 mode key takes priority over stripping, including the Advanced Chainsaw's
  sneak/mode tree-felling shortcut. Both shearing modes can strip wood.
- A main-hand chainsaw lets an offhand shield activate unless the player is
  sneaking, matching vanilla axe behavior. Offhand stripping also works.
- Insufficient charge, an unsupported/already stripped block or a cancelled native
  tool-modification event leaves the block and operation energy unchanged.
- Existing armor-to-tool charging, shearing, tree felling and mining logic remain
  in their shared paths. No new UI strings or key bindings are introduced.

This patch adds wood stripping. It does not advertise copper scraping or wax
removal as chainsaw abilities.

## Validation

The server regression tests use the native `ServerPlayerGameMode.useItemOn` path
in survival mode, rather than calling only the new method directly. They compare
stripped results against a vanilla axe for all available strippable log/wood
states and all three axes, including nether stems and bamboo.

Additional cases cover exact/insufficient charge, rejected repeat clicks, mode
shortcuts, both shearing modes, offhand use, shield intent and cancelled tool
events. Existing chainsaw tests continue to cover wool, sheep, attack energy and
wood harvesting. When IC2R Addons is loaded, the same new cases also run against
its registered Advanced Chainsaw without a hard dependency from the core.

Included in IC2R 21.1.95. Advanced Chainsaw stripping also works through this
shared implementation when an existing compatible Addons version is installed.

Validation passed 34 core unit tests, 532 standalone server GameTests and 627
combined core/Addons GameTests. The stripping comparison covered 63 wood/axis
cases for each saw, 126 when the Advanced Chainsaw was present. Existing shearing
and harvesting tests passed in the same runs.
