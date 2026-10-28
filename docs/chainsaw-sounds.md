# Chainsaw shutdown audio

## Shared cause

Addons `AdvancedChainsaw` extends IC2R `ItemElectricToolChainsaw`. It inherits the
same idle, stop and cutting sound events; it does not replace the sound lifecycle.

The shared `ItemElectricTool` requested `stopSound.playOnce()` and immediately
called `clearSound()`. Clearing removed that same controller and cancelled its
queued playback before the audio engine could play the shutdown sound.

The item-wide `wasEquipped` flag also depended on the order in which spare stacks
ticked. A second chainsaw in the inventory could suppress detection of a discharged
held saw. A tool removed from the inventory receives no further inventory ticks,
so those ticks alone cannot reliably handle dropping or transferring it away.

## Correction

- Detach the shutdown controller before clearing idle/start sounds. Play its final
  sound once, then unregister it through its completion callback.
- Track changes of the local player's held electric tool at the end of the client
  tick. This handles dropped and removed stacks, while a changed player/world
  releases the previous controller without starting a shutdown sound.
- Detect charge loss and initialization from the active sound handles, rather than
  the item-wide equipment flag. Recharging a held saw restores idle playback.
- Keep client audio cleanup off the integrated server thread when dropping a tool.

The correction belongs to IC2R and also covers the existing Addons chainsaw. Its
textures, sound files, cutting events, energy costs and tool modes are unchanged.

## Validation

The opt-in `chainsawSoundSmoke` development run uses an isolated world and the real
Minecraft sound engine. It exercises both registered chainsaws, each with a spare
copy in the inventory:

| Action | Expected audio |
| --- | --- |
| Change the selected hotbar slot | Idle stops; one shutdown; idle returns on re-equip |
| Drop the selected tool through the native client action | Idle stops; one shutdown |
| Remove the selected stack from the server inventory | Idle stops; one shutdown |
| Discharge and recharge the held tool | One shutdown; idle returns after charging |
| Switch between identical stacks | One continuous idle source, no false shutdown |

Assertions inspect both playback requests and actual decoded audio-source events,
not just the controller's playing flag. They also check for duplicate shutdowns,
surviving idle channels and retained completed shutdown controllers.

Run with Addons present:

```powershell
.\gradlew.bat runClient '-Pneo_version=21.1.264' -PchainsawSoundSmoke '-PaddonsJar=C:\GIT\IC2R-Addons\build\libs\ic2r-addons-neoforge-1.21.1-1.4.6.jar' --no-configuration-cache --console=plain
```

Successful completion emits `IC2R_CHAINSAW_SOUND_CLIENT_PASS`.

The paired pass completed with both saws (eight unequip/depletion cases), no duplicate
idle loops or shutdown events, and no retained completed shutdown controller.
The build and all 29 unit tests passed on NeoForge 21.1.264.
