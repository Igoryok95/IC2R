# 21.1.68 (unreleased)

Minecraft **1.21.1** · NeoForge **21.1.234+** · Java **21**

Publishing is on hold. The latest published release remains 21.1.67.

## Fixed

- Preserve complete translatable chat components on server and client. Russian clients now
  receive localized chainsaw mode messages instead of server-rendered English sentences.
- Keep nested mode names / styles and numeric parameters intact. Sprayers, armor HUD,
  miners, EU storage, chargepads, wind meters and directional upgrades use the same path.
- Replace literal-English ownership / safe, painter, EU-reader and mining-laser messages
  with locale keys. Localize debug mode names and player-facing report labels / values.
- Localize Coke Kiln construction hints, invalid-fluid warnings and the in-game mod-list
  description. English / Russian coverage is 1335 matching keys.

## Validation

Eight new GameTests exercise real mode changes / machine notifications and check the
translation trees that reach the player. Three JUnit checks verify argument / style
preservation and serialized messages rendered first in English, then in Russian.
The full final local run passed **442 GameTests** and **14 JUnit tests**.
One earlier wind-turbine fixture failure and the successful rerun are recorded in
FIXED_BUILD.md. No wind behavior was changed.

This is a text / message-delivery update. No machines are added; crafting, generation,
processing balance and saved-item identities remain unchanged.

---

# 21.1.67

Minecraft **1.21.1** · NeoForge **21.1.234+** · Java **21**

This is the Igoryok95 fork release, based on upstream IC2R 21.1.66.

## Fixed

- Reject empty and zero-count stacks in machine processing input slots before copying them.
  This fixes crashes when Smooth Swapping or another inventory extension inspects a source
  slot after its contents have been shift-clicked into the player inventory.
- Keep the original validation of non-empty machine inputs and restrictions on output slots.

## Added

- Russian translations for all 1291 English locale keys.
- Nine regression game tests for machine input inspection and slot behavior.
- Localization tests for key coverage, format arguments and UTF-8 text.

## Version and installation

- The default branch and standard Gradle build now use **21.1.67**.
- The mod advertises `ic2` version `21.1.67`; the JAR is `ic2-neoforge-21.1.67.jar`.
- Replace the previous IC2R JAR; keep only one IC2R JAR in `mods`.
- Install on the client and server and keep their versions aligned.
- Smooth Swapping can remain enabled. Recipes, world generation, balance and packet formats
  are unchanged by this fork's patch.

## Validation

Run `./gradlew build runGameTestServer` with Java 21. The suite contains 434 game tests and
11 JUnit tests, including the empty-input and Russian-localization checks.
Local release validation passed all 434 game tests and all 11 JUnit tests. The packaged JAR's
version, Russian locale coverage and ZIP integrity were also verified.
