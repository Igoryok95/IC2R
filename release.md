# 21.1.92

- Preserve the selected jetpack hover mode when landing on a block.
- Keep grounded hovering idle until jump is pressed, without idle fuel or EU use.
- Toggle hover once per jump/mode-key press instead of repeating every ten ticks.
- Apply the fix through the common path used by native, attached and Addons jetpacks.
- Keep existing localized mode messages, thrust strength and airborne energy costs.

Validation: core build, all 34 unit tests and all 605 required tests in the combined
IC2R 21.1.92 / Addons 1.4.12 dedicated GameTest run passed.

See [cause and validation](docs/jetpack-hover-21.1.92.md).
