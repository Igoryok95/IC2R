# Warm-up display spacing — 21.1.75

The previous check only compared text width with a nominal column width. A full
`100.00%` value could still touch an input slot or the energy-indicator background.

The Induction Furnace now reserves space on the left by moving its input overlay,
input slots, battery and EU indicator together. Its recipe click area follows the
repositioned arrow. Addons aligns all eight caption/value columns and moves the
three crowded input/battery/EU columns slightly right. Progress-gauge backgrounds
and menu slot order remain intact.

Normal font size and two-decimal precision are preserved. Recipes, storage, machine
logic, speed, sound and energy balance are unchanged. No new localization keys are
needed; English and Russian key sets remain complete.

## Validation

- Core build: 24 JUnit tests passed. Addons build: 19 JUnit tests passed, including
  progress-background separation from inventory slots.
- One isolated client pass opened the Induction Furnace and all eight Advanced
  Machines at full warm-up with filled inputs, in Russian and English: 18 views.
- 520 checks compared rendered text bounds with actual neighboring widget bounds,
  including gauge backgrounds. Minimum clearance: **4 GUI pixels**, which is
  8 screen pixels at the tested GUI scale 2.
- All 18 captured screens were visually inspected. Production client, archive and
  server files were not modified by this source update.

See [validation receipt](validation/21.1.75.json).
