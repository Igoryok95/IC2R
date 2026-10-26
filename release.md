# 21.1.74

Minecraft 1.21.1 / NeoForge 21.1.234+ / Java 21.

- Add optional Jade EU gauges, tiers, measured network input/output, storage output,
  machine progress/heat and reactor heat/output, with English and Russian labels.
- Resolve JEI inputs and all 36 player inventory slots from the actual dynamic menu.
  Prevent the per-frame IndexOutOfBoundsException caused by addon menu layouts.
- Keep outputs, upgrades, batteries, blades and fluid service slots out of recipe
  ingredients. Match solid-canner container/material ordering and slot-grid bounds.
- Add native furnace transfer without JEI's decorative fuel requirement and dynamic
  transfer for all three Metal Former modes. Resolve addon recipe-button categories.
- Display Induction Furnace heat with two decimal places, matching Addons 1.4.4.
- Keep JEI, Jade and IC2R Addons optional; preserve machine balance and recipes.

See [integration details and validation](docs/jei-jade-21.1.74.md).

---

# 21.1.73

Minecraft 1.21.1 / NeoForge 21.1.234+ / Java 21.

- Bound network intake by free capacity in native EU components and public sink prefabs;
  return rejected EU and decrement remaining network demand after each transfer.
- Repair old overfilled BatBox/CESU/MFE/MFSU and chargepad saves on load. Bound legacy
  storage setters and capacity reductions while preserving signed teleporter withdrawals.
- Wait before consuming an indivisible redstone fuel portion when it does not fit.
- Update electric-item charge through copied custom data, so menu snapshots remain
  independent and the final charge reaches the client without moving the item.
- Preserve native capacities, item transfer limits, recipes and Matter Fabricator amplification.

See [audit and validation](docs/energy-storage-21.1.73.md).

---

# 21.1.72

Minecraft 1.21.1 / NeoForge 21.1.234+ / Java 21.

- Change the Steel Hopper recipe to five steel plates and one vanilla hopper. The recipe book unlocks with steel plates; ingots no longer match.
- Replace the block-like inventory picture with a flat, front-facing 32x32 hopper icon in vanilla proportions, with a transparent background and steel colors.
- Shorten English and Russian transfer tooltips to the item rate.
- Preserve the transfer rate, yellow rarity, block model, inventory and redstone behavior.

---

# 21.1.71

Minecraft 1.21.1 / NeoForge 21.1.234+ / Java 21.

Adds the Steel Hopper to IC2R itself. It transfers one item every two ticks between
containers, up to 10 items/s, while ordinary hoppers keep their vanilla
timing. Craft one with five steel ingots and a vanilla hopper.

Retains the vanilla model, five-slot GUI, redstone locking, comparator output,
loose-item collection and inventory drops. Mod item pipes use native NeoForge
hopper capabilities. Contents, custom names and transfer cooldown survive reloading.
New 32x32 steel textures follow the approved IC2R design; the Uncommon item name is
yellow. Names and tooltips are localized in English and Russian. No EU or addon is required.

Validation: 24 JUnit tests and 484 standalone GameTests pass. The actual native GUI,
five synchronized slots, EN/RU names and yellow tooltip title were checked in an
isolated client. See [details](docs/steel-hopper-21.1.71.md).

Existing client, ZIP and production-server installations are a separate update.

---

# 21.1.70

Restores Experimental beer/rum brewing, basic charging armor, fluid pipes and pump
covers, dynamite and remote control, containment box, iodine, MFSU upgrade kit and
reusable fluid cell and the original Compact Item Buffer (nine shared slots, four
transfer upgrades, no EU). Local torch buttons keep the Induction Furnace, Thermal
Centrifuge and Blast Furnace warm without emitting redstone to neighboring blocks.
Original EU/hU supply and external redstone behavior are preserved. Six original Classic profession skins accompany native
Experimental trades and functional village workshops; Addons extends matching trades when installed.
Workshops enter the five vanilla village styles additively: electrical, crop, brewing and demolition
weight 2 each, nuclear and Greg weight 1 each. Vanilla weights remain unchanged; only new village
chunks receive workshops. The nuclear workstation is empty and contains no fuel.
The IC2 brewing-barrel recipe now uses a vanilla barrel between two rubber items.

Optional Create compatibility reuses existing crushed ores and includes zinc
processing, with positive recipe XP retained by all three furnaces. Fixes Create's
outdated IC2 recipe identifiers through a conditional built-in compatibility pack.
Also supports optional Mekanism osmium, fluorite and tagged intermediates, including
Create crushed osmium and positive XP. Adds a localized client volume slider for
IC2R audio and balanced hopper insertion into both induction inputs. No Classic
progression, PESU, upper solar tiers or ore-generation changes.

Retains released XP, localization, input, crop-save and GUI fixes. Storage gauges
show exact values in live tooltips; teleporter sums and external readings avoid
integer overflow. Russian Compressor naming is unified. See
[feature and validation details](docs/experimental-restoration-21.1.70.md).

Requires Minecraft 1.21.1, Java 21 and NeoForge 21.1.234+. IC2R Addons is optional.
Existing explored chunks are preserved; village workshops appear in newly generated village chunks.
This release does not install itself into any existing client, ZIP or server.

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
