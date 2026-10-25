# IC2R 21.1.70 maintenance branch

This branch starts from released IC2R 21.1.69. The broad Classic restoration is no
longer part of the planned update. Experimental restoration and the separately
approved six villager professions are tracked in [the scope](experimental-restoration-21.1.70.md). This is development source, not a published
release or an installed client/server update.

## Preserved fixes

- Processed-ore XP and collection from iron, electric and induction furnaces are
  inherited from 21.1.69, including fractional remainders and save/reload safety.
- Existing Russian translations, client-language notifications, empty-input crash
  protection, crop-save deadlock prevention and GUI texture fixes are retained.
- Storage menus keep the original charge bar. Exact grouped charge/capacity values
  appear in its tooltip; tier and output occupy bounded separate rows rather than
  overlapping the gauge or running outside the panel.
- Dynamic XML tooltips evaluate their data expressions live instead of displaying
  unresolved expressions such as `%base.getEnergyTooltip()%`.
- Extended storage readings avoid narrowing large energy values to signed integers.
  The existing integer API remains available. Teleporter totals and cost distribution
  avoid overflow, and a missing energy source cannot cause division by zero.

The Classic material chain, ore generation, machines, large stores, cables and
electrochemistry extensions are excluded. Native storage capacities remain unchanged.
Abandoned work is recoverable from the earlier development branch.

IC2R remains standalone. IC2R Addons is optional; its maintenance branch contains
display and monitoring fixes without restoring the removed high-tier solar panels
or changing equipment balance.

## Completed additions and validation

The Experimental scope, six professions, optional Create/Mekanism processing,
sound slider and balanced induction hopper inputs are documented in
[the feature scope](experimental-restoration-21.1.70.md). The sound slider uses the
free narrow cell beside Voice/Speech. Core remains independent of Addons.

Final checks: 23 JUnit tests, 471 standalone server GameTests (six optional no-op
checks), separate 3-test Create and Mekanism combinations, 87 Addons integration
tests, and 23 changed menus inspected in the Russian client. Saved audio gain and
an active looping sound pass mute/unmute checks. See the
[validation receipt](validation/21.1.70.json) for exact commands and limits.
