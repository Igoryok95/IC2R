# IC2R empty-input fix and Russian localization

This fork starts at upstream release `21.1.66` (commit `417fddb1fd23b926accc02b380cd658121f5c9e1`)
for Minecraft 1.21.1 and NeoForge. It contains two changes:

- Machine processing input slots reject empty stacks before calling `StackUtil.copyWithSize`.
  This prevents `IllegalArgumentException: empty stack: 0 minecraft:air` when inventory extensions
  inspect a source slot after shift-clicking its contents into the player inventory. The recipe
  checks for non-empty inputs and the output-slot restrictions are preserved.
- `assets/ic2/lang/ru_ru.json` supplies Russian translations for all 1291 keys in `en_us.json`,
  including machines, items, interfaces, tooltips, crops, advancements, subtitles and death messages.

## Version 21.1.67

The patch and Russian translations are included on this fork's `main` branch as the standard
**21.1.67** release. Build it with `./gradlew build runGameTestServer`; the output is
`build/libs/ic2-neoforge-21.1.67.jar`, with mod ID `ic2` and advertised version `21.1.67`.
The instructions below describe the earlier `21.1.66_fixed` compatibility build.

## Version 21.1.68: client-language messages

Locale-file coverage did not catch server-side translation of nested chat components. The old
Component overload called `getString()` on the server, then sent the resulting English sentence
as a translation key. Chainsaw `Mode: Normal` / `Mode: No Shearing` reproduced the problem.

Both proxies now deliver the original component tree. Arguments keep nested translations,
styles and numeric types. Existing tool / armor / machine call sites no longer flatten messages
before sending them. Literal-English player warnings and debug chat labels use locale keys.
The common EN / RU coverage is now 1335 keys, including Coke Kiln / fluid warnings and the
localized mod-list description. Built-in technical class names, IDs and developer-console
field / graph reports remain diagnostic data.

The final local validation passed 442 GameTests and 14 JUnit tests. One earlier full run failed
the pre-existing wind-turbine spin persistence test with a random zero-speed fixture; all new
chat tests passed in that run, and the subsequent complete run passed all 442 tests. Wind
simulation and machine logic were not modified by this text update.

The standard 21.1.68 build produces `build/libs/ic2-neoforge-21.1.68.jar` with mod ID `ic2`.
Release artifacts are available from the repository's GitHub Releases page.

## Version 21.1.69: processed-ore and induction-furnace XP

Twenty processed-metal smelting recipes had experience 0.0. Their dust / crushed / purified
forms now grant half the corresponding direct raw-material reward (iron / copper 0.35,
gold 0.5, IC2 metals 0.25). Bronze alloy and non-ore recipes keep their existing behavior.
The recipe manager still supplies the reward; no tag / output fallback overrides datapacks.

The Induction Furnace now persists XP, accumulates each completed channel independently and
handles the same localized Collect XP button as the Iron / Electric Furnaces. Heat labels
move above the button so they do not overlap it or the player inventory. Every collection
marks the machine dirty and retains fractional XP. A shared decimal accumulator normalizes
float recipe rewards before summing, preventing 20 x 0.35 from becoming just under 7.

The new regressions cover real Macerator -> Electric Furnace processing, both induction
channels / blocked outputs, iron-furnace smelting, persistent collection, duplicate clicks
and integer payout after fractional recipes. The optional `-PxpSmoke` developer client uses
only `run/saves/FurnaceXpSmoke`; normal clients never execute the hook.

## Translation provenance

928 translations were matched against the English and Russian locale files shipped in the original
[IndustrialCraft 2 Experimental 2.8.222-ex112](https://maven.ic2.player.to/net/industrial-craft/industrialcraft-2/2.8.222-ex112/).
The legacy Russian locale identifies itself as the translation dated 2019-04-30. The remaining 363
keys were translated or adapted for this port; inherited English strings and terminology were also
reviewed. Credits for the original translations remain with their respective contributors and the
IC2 Dev Team.

Reference JAR SHA-256: `f96b1886b6d060b308fb811029d5c5cc76deb17b835aa368dcb6071f2de290d4`.

## Historical 21.1.66_fixed build and validation

Use Java 21 and the checked-in Gradle wrapper:

```text
./gradlew test runGameTestServer jar -Pmod_version=21.1.66 -PfixedBuild
```

On Windows, use `gradlew.bat` and quote `"-Pmod_version=21.1.66"` in PowerShell.
The resulting mod is `build/libs/ic2-neoforge-21.1.66_fixed.jar`.
Its mod ID and advertised version remain `ic2` and `21.1.66` for compatibility with the original
server and addon dependency checks. The manifest identifies the patched build with
`Implementation-Version: 21.1.66_fixed` and an `IC2R-Patch` attribute.

Nine regression game tests cover empty and zero-count inputs, the post-transfer
`Slot.allowModification` call, non-empty recipe validation and output extraction. With these tests
added to the unmodified release, six tests reproduce the empty-stack exception. With the guard,
all 434 game tests pass. Locale unit tests check key coverage, formatting arguments and UTF-8 text.

Install the fixed JAR in place of the original IC2R JAR. Keep only one IC2R JAR in `mods`.
Smooth Swapping does not need modification or disabling. This fork does not change recipes,
world generation, machine balance, saved data or network packets.

## Shutdown regression discovered during 21.1.69 validation

The first full run passed all 449 then-registered GameTests but stalled during world saving.
A thread dump located `TileEntityCrop.getStoredCrossingBase -> Level.getBlockState` inside
chunk unloading / serialization. Asking the world for that same chunk re-entered the chunk
loader after the chunk had begun unloading. The getter now reads the block entity's cached
BlockState and falls back to its stored crossing flag, without a world lookup. An additional
GameTest serializes a detached crop with a different world state to verify this invariant.
The unfinished developer world is retained locally; final validation uses a fresh test world.

## GUI overlay validation

An initial Russian client smoke exited with Windows access violation 0xC0000005 while opening
the induction screen; no Java crash stack was produced. Source inspection also found a
redundant raw OpenGL allocation in GlTexture: Image then rendered the original resource via
Minecraft's texture manager, but used power-of-two padded dimensions for UV coordinates.
The metadata helper no longer allocates / frees a second GPU texture. Minecraft owns texture
upload / release, and dimensions match the original PNG, including non-power-of-two and 1x1
images. Unit checks exercise both without an OpenGL context. Actual EN/RU screen checks and
server-owned XP collection are repeated after this change; the crash is not treated as a
proven driver diagnosis.

Final 21.1.69 local results: 450 server GameTests, 20 JUnit tests, and successful English /
Russian native-menu XP collection. The final server saved all dimensions and shut down.
