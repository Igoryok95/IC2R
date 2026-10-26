# Reactor chamber readings in Jade — 21.1.77

Jade previously collected IC2R telemetry from the targeted block entity. A reactor
chamber delegates its inventory to the reactor, but is not itself an `IReactor`.
Consequently Jade showed the inventory while omitting reactor heat and EU output.

The telemetry collector now resolves a chamber to the live central reactor in one
of its six adjacent positions. The same server-owned heat, maximum heat, nominal
output and active state are displayed through every connected chamber. The chamber
keeps its own block title and Jade's normal inventory/fluid information.

An isolated chamber, a chamber without a world, or a removed block entity returns
no status. The Jade provider does not add an empty IC2R status tag. The lookup checks
loaded adjacent positions on each request, rather than relying on the chamber's
cached reactor reference; removal and same-tick replacement cannot reuse old readings.
It does not load chunks to find a reactor.

This change affects telemetry only. Reactor behavior, chamber attachment, inventory,
redstone, power output and saved data are unchanged. Existing English/Russian Jade
translations are reused; Jade and Addons remain optional.

## Validation

- Build and **26 JUnit tests** pass, including complete EN/RU locale coverage.
- **495 required GameTests** pass without Jade installed. The two added tests
  check all six chamber directions, changing heat/output/active state, an isolated
  chamber, removal without a neighbor notification and same-tick reactor replacement.
- One isolated client pass with Jade installed verifies data received through
  Jade's real server request, not a synthetic tooltip. The connected chamber shows
  the reactor gauges in Russian and English. The standalone chamber receives no
  `ic2_status` tag and displays no reactor fields. All three captured views were
  visually inspected.
- No production client, ZIP or server files were changed by this source update.

See the [validation receipt](validation/21.1.77.json).
