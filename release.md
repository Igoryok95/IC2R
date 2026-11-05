# 21.1.94

- Make the Matter Generator's external fluid connections output-only on all six faces.
- Prevent pipes from returning matter to generators instead of delivering it to a Replicator.
- Keep existing stored matter, capacity, generation costs, ejectors, pump extraction and cell filling.
- Add a regression test with eight real generators, fluid-ejector upgrades, saved pipe remnants
  and a raised Replicator, running through normal server ticks.

See [cause, compatibility and validation](docs/matter-generator-backflow-21.1.94.md).
