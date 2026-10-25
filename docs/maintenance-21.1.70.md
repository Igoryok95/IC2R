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

Final checks: 23 JUnit tests, 479 standalone server GameTests (six optional no-op
checks), separate 3-test Create and Mekanism combinations, 94 Addons integration
tests, and 23 changed menus inspected in the Russian client. Saved audio gain and
an active looping sound pass mute/unmute checks. See the
[validation receipt](validation/21.1.70.json) for exact commands and limits.

## Local standby and compact buffering

Manual torch controls now preserve standby state across saves without producing
redstone that could lock adjacent hoppers. The three native warm-up machines keep
their original power/heat rules. Dynamic GUI titles can reserve bounded space for
corner controls; the Blast Furnace heat label is localized. Experimental's compact
buffer restores shared storage and both transfer-upgrade directions without EU.
See the feature document and validation receipt for current checks.

## Natural professions

The six jobs now have natural village workshops, not only manually placed POIs.
The approved additive weights are 2/2/2/2/1/1 across all five vanilla village styles;
existing vanilla weights and explored chunks are preserved. Actual jigsaw placement
is checked for a native resident and workstation. The brewing-barrel recipe uses a
vanilla barrel plus two rubber, preserving the separate fermentation block.
