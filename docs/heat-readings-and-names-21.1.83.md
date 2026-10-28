# Heat readings and familiar names in 21.1.83

## Heat output

The heat-source base previously retained the last draw indefinitely and replaced
the reading for each request. A stopped Blast Furnace could therefore leave an
Electric Heat Generator displaying `10 hU` even though it no longer transferred
heat. The electric generator also retained its active flag when its full buffer
prevented another conversion attempt.

Sources now accumulate actual, non-simulated draws per world tick and publish the
last completed tick. Publishing is independent of source/consumer tick order;
an idle tick clears the reading. This applies to electric, fluid, solid-fuel and
radioisotope heat generators and the Liquid Heat Exchanger. Simulations and
wrong-side requests do not contribute; nonpositive requests transfer nothing.
Stored heat, heat-generation limits, conversion costs and recipes are unchanged.

The Electric Heat Generator resets its conversion state before each server tick.
Its GUI shows current/maximum output in hU/t. Jade receives the same heat reading
in a dedicated localized row. Its existing EU/t figures continue to describe the
electrical network, including replenishment of the generator's energy buffer.

## Russian terminology and JEI

The reference is the bundled Russian localization in IC2 Experimental
`2.8.222-ex112`, with selected familiar Classic terms retained as search aliases.
Existing corrections, units, translation arguments and chat localization remain.

| Ingredient | Display name | Additional Russian JEI search terms |
| --- | --- | --- |
| Mass Fabricator | Генератор материи | Генератор жидкой материи; Производитель материи |
| Compressor | Компрессор | Сжиматель |
| BatBox | Энергохранилище | Бат-бокс; Батбокс |
| Solar Helmet | Шлем с солнечной батареей | Солнечный шлем |
| Static Boots | Электростатические ботинки | Статические ботинки |
| Containment Box | Свинцовый контейнер | Защитный контейнер |
| Iodine Tablet | Таблетка йода | Таблетка иода; Йодная таблетка |
| MFSU Upgrade Kit | Набор улучшения МФСУ | Комплект улучшения МФСУ |
| Dynamite-O-Mote | Радиопульт подрыва | Дистанционный подрыватель |

Pump-cover names use the old **Накладной насос** terminology. Fluid-pipe names
use the shorter historical form, including **Тонкая** for the smallest size;
previous long names remain searchable. The generator's subtitles and GUI title
follow its item name, and the BatBox GUI follows its block name.

Aliases are translation keys in `assets/ic2/search_aliases.json`, with both
English and Russian values. JEI resolves them for the selected language. No
localized display text is embedded in Java; JEI remains optional. Classic terms
such as **Комбайн**, **Агроанализатор** and **Торговая станция** are search aliases
without replacing the existing Experimental display names.

## Validation

Native server regressions cover all five heat sources, multiple real draws,
simulation, invalid sides, idle reset and GUI/Jade consistency. A real Blast
Furnace fixture checks disabled, enabled and disabled-again states, stored heat
and the electric generator's active flag.

The opt-in `-PheatAndNamesSmoke -PwithJade` client pass checks every alias through
JEI's actual ingredient filter in Russian and English, synchronized generator GUI
readings, Jade server data through the real overlay and LCD text width up to
`100 / 100 hU/t`. Locale tests check UTF-8, key parity and format arguments.

Completed checks:

- 34 unit tests and a release build against NeoForge 21.1.234 / JEI API 19.27.
- 500 standalone core GameTests, including both new heat regressions.
- Real client pass on NeoForge 21.1.264 / JEI 19.64.0.455 / Jade 15.10.6:
  all 29 aliases across 26 ingredients found in RU and EN; synchronized heat
  readings `0 / 10`, `10 / 10`, then zero again in GUI/Jade; screenshots inspected.
- 587 combined core/Addons GameTests with Addons 1.4.8.

The initial client launch used JEI 19.64 with NeoForge 21.1.234 and was rejected
by JEI's minimum NeoForge 21.1.238 requirement. The successful client pass used
21.1.264. Core's own minimum was not raised; optional JEI integration declares
the 19.10 API minimum required for localized ingredient aliases.
