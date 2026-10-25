# 21.1.70 (development branch)

Restores Experimental beer/rum brewing, basic charging armor, fluid pipes and pump
covers, dynamite and remote control, containment box, iodine, MFSU upgrade kit and
reusable fluid cell. Six original Classic profession skins accompany native
Experimental trades; Addons extends matching trades when installed.

Optional Create compatibility reuses existing crushed ores and includes zinc
processing, with positive recipe XP retained by all three furnaces. Fixes Create's
outdated IC2 recipe identifiers through a conditional built-in compatibility pack.
Also supports optional Mekanism osmium, fluorite and tagged intermediates, including
Create crushed osmium and positive XP. Adds a localized client volume slider for
IC2R audio and balanced hopper insertion into both induction inputs. No Classic
progression, PESU, upper solar tiers or world-generation changes.

Retains released XP, localization, input, crop-save and GUI fixes. Storage gauges
show exact values in live tooltips; teleporter sums and external readings avoid
integer overflow. Russian Compressor naming is unified. See
[feature and validation details](docs/experimental-restoration-21.1.70.md).

This is not yet a published release or an installed client/server update.

---

# 21.1.69

Minecraft **1.21.1** · NeoForge **21.1.234+** · Java **21**

Fixes lost smelting XP after IC2R ore processing and adds XP collection to the Induction Furnace.

## Fixed

- Give the 20 smeltable dust / crushed / purified ore recipes positive XP: iron / copper 0.35,
  gold 0.5, lead / silver / tin / uranium 0.25 per item. A normal doubled ore route retains
  the raw material's total smelting reward. Alloy / non-ore recipe XP is unchanged.
- Accumulate and persist XP from both successful induction-furnace channels. Add the same
  localized Collect XP button as the Iron / Electric Furnaces; keep heat text clear of it.
- Preserve decimal rewards without float accumulation drift. Twenty iron-dust smelts now
  produce exactly 7 collectable XP rather than 6 with an almost-integer remainder.
- Mark all three furnaces dirty after collection; retain fractional XP without duplicating
  already collected rewards after saving / reloading.

- Render GUI overlays with Minecraft-owned textures and real PNG dimensions. Remove the
  redundant raw OpenGL upload and incorrect power-of-two UV padding; restore slot frames.
- Avoid loading a world chunk while serializing crop crossing state. The old path could
  deadlock chunk unloading / server shutdown; serialize the block entity state directly.

Existing positive recipe rewards and unification remain data driven. No mod integration,
world-generation change, energy rebalance or saved-item-ID migration is added. Previously
unrecorded zero-XP smelts cannot be recovered retroactively.

## Validation

450 server GameTests, 20 JUnit tests and actual EN/RU client checks of all three furnace
menus / XP buttons. The final server also saves and shuts down normally. Detailed evidence
is recorded in docs/validation/21.1.69.json.

---

# 21.1.68

Minecraft **1.21.1** · NeoForge **21.1.234+** · Java **21**

This release fixes client-language notifications and completes English / Russian coverage
for player-facing messages.

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
