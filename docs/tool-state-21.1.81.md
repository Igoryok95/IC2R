# Tool state fixes — IC2R 21.1.81

## Nano Saber combat state

The tooltip recalculated the active damage modifier correctly, but the equipped
player could keep the inactive combat attributes. The active flag was changed
inside the live `CustomData` tag shared with Minecraft's previous equipment
snapshot. Both copies changed together, so equipment comparison did not detect
the toggle.

The state capability now updates `CUSTOM_DATA` through `CustomData.update`.
Minecraft retains an independent old snapshot and applies the existing dynamic
damage and attack-speed modifiers through its normal equipment lifecycle.
Activation, deactivation and depleted-state network updates use this same path.

Balance is unchanged. A normal player's base plus item attributes are:

| State | Damage before armor, critical hits and enchantments | Attack speed |
| --- | ---: | ---: |
| Inactive, or insufficient charge for a powered hit | 5 | 1 |
| Active with at least 400 EU | 21 | 4 |

A fully charged active hit kills an ordinary unequipped zombie. Switching the
saber off restores the inactive values without re-equipping it. Existing activation
and idle-drain rules, the 400 EU hit cost and special armor interaction are retained.

## Shared drill environment rules

`ItemDrill.getEnvironmentSpeedMultiplier` exposes the original IC2R rules: a factor
of three underwater when the submerged-mining attribute is below one, and another
factor of three without ground contact. These compensate part of Minecraft's
ordinary penalties. The native drills still use the same values.

Addons 1.4.7 reuses this method for its diamond drill, iridium drill and Vajra.
IC2R neither loads nor requires Addons for the helper or native tools to work.

## Validation

Native server regression tests verify independent state snapshots, preservation
of unrelated item attributes and tooltip flags, actual equipped combat attributes
and actual zombie health after charged hits. The held stack is never changed during
activation/deactivation. Further checks exercise 399 EU, exactly 400 EU, discharge,
failed energy use and reactivation after recharge.

The release also contains the [shared chainsaw sound correction](chainsaw-sounds.md),
already checked in the real client audio engine with both native and Addons saws.

The final core build and 29 unit tests passed on NeoForge 21.1.234. The paired
NeoForge 21.1.264 run with Addons 1.4.7 passed all 584 required native server tests.
See [the validation record](validation/21.1.81.json).
