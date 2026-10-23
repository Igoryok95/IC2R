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
