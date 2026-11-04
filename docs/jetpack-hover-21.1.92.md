# Jetpack hover selection in 21.1.92

## Cause

The shared jetpack tick explicitly disabled `hover_mode` when the player touched
the ground. Attempting to enable hover while standing on a block could therefore
produce an enabled message immediately followed by a disabled message.

Separately, holding jump and the mode key retriggered the switch every ten ticks.
That cooldown repeated both the mode change and its chat message while the same
keys remained held.

## Fix

Landing preserves the selected hover mode. Without a jump request, a grounded
pack remains idle, with no thrust, fuel/EU use or engine loop. Leaving the ground
resumes powered hovering, and sneaking still controls descent. Airborne thrust,
energy costs and the survival-server flight watchdog keep their existing logic.

The server now detects the press edge of the jump/mode-key combination. Holding
it does not repeat. Releasing either key rearms the combination for the next
press, including immediately after landing. This input latch belongs to the
player rather than the item save and is cleared when no jetpack is equipped.
Hover changes replace the item component and use the existing localized messages.

This is a core fix. IC2R Addons 1.4.12 already delegates its Advanced Jetpack and
Advanced Nano Chestplate to this path and requires no separate implementation.
Gravi Chestplate creative-flight controls are a different system.

## Validation

Native regression tests cover electric and biogas jetpacks and a jetpack attached
to an ordinary chestplate. When Addons is installed, the same tests also exercise
its Advanced Jetpack and Advanced Nano Chestplate through registry lookup, without
adding a required dependency to the core.

The cases cover landing and ground activation, idle energy use, resumed hover,
sneaking descent, held combinations across landing, immediate release/repress and
localized message counts.

Validation completed:

- The core build and all 34 unit tests passed against NeoForge 21.1.234.
- One combined dedicated run with NeoForge 21.1.264 and IC2R Addons 1.4.12 passed
  all 605 required GameTests, including both new hover regressions across all five
  pack variants. No BuildCraft or Jade dependency is required for the fix.
- Existing flight-watchdog, thrust, fuel/EU, attachment and equipment tests passed
  in the same run. The production server and client were outside this test run.
