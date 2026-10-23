# IC2R 21.1.66 fixed build

This fork starts at upstream release `21.1.66` (commit `417fddb1fd23b926accc02b380cd658121f5c9e1`)
for Minecraft 1.21.1 and NeoForge. It contains two changes:

- Machine processing input slots reject empty stacks before calling `StackUtil.copyWithSize`.
  This prevents `IllegalArgumentException: empty stack: 0 minecraft:air` when inventory extensions
  inspect a source slot after shift-clicking its contents into the player inventory. The recipe
  checks for non-empty inputs and the output-slot restrictions are preserved.
- `assets/ic2/lang/ru_ru.json` supplies Russian translations for all 1291 keys in `en_us.json`,
  including machines, items, interfaces, tooltips, crops, advancements, subtitles and death messages.

## Translation provenance

928 translations were matched against the English and Russian locale files shipped in the original
[IndustrialCraft 2 Experimental 2.8.222-ex112](https://maven.ic2.player.to/net/industrial-craft/industrialcraft-2/2.8.222-ex112/).
The legacy Russian locale identifies itself as the translation dated 2019-04-30. The remaining 363
keys were translated or adapted for this port; inherited English strings and terminology were also
reviewed. Credits for the original translations remain with their respective contributors and the
IC2 Dev Team.

Reference JAR SHA-256: `f96b1886b6d060b308fb811029d5c5cc76deb17b835aa368dcb6071f2de290d4`.

## Build and validation

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
