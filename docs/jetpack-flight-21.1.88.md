# Jetpack flight in 21.1.88

The shared jetpack code reset fall distance after powered thrust, but did not
clear Minecraft's separate floating counter. Sustained hovering could therefore
cause a vanilla survival server with `allow-flight=false` to disconnect a player.

Successful server-owned thrust now clears `clientIsFloating` and
`aboveGroundTickCount` through a small server-network accessor. The normal packet
handler evaluates subsequent movement as before. No creative-flight ability,
permission or server configuration changes are made. An empty or inactive
jetpack does not clear the watchdog. This shared path covers electric/biogas
jetpacks, attached jetpacks and compatible Addons nano/advanced jetpacks.

The native regression test exercises sustained powered hovering, then verifies
that an empty pack leaves the watchdog untouched and that the player abilities
and server flight setting remain unchanged. Existing jetpack movement, energy,
sound and attachment tests remain applicable.

An audit of the native armor and electric-tool hints confirms that Nano/Quantum
helmets, jetpack hover, Quantum movement, chainsaw, mining laser and wrenches
read current key mappings. Addons 1.4.10 uses the same live-binding approach.

Validation: core build and all 34 unit tests passed. The paired IC2R 21.1.88 /
Addons 1.4.10 dedicated GameTest run passed all 594 required tests, including the
floating-counter regression and enabled/disabled Nano/advanced jetpack paths.
The actual client verified the native helmet hint after rebinding both IC2 keys
in English and Russian. Server configuration and player flight abilities were
checked to remain unchanged by the jetpack fix.
