# Continuous machine startup audio — 21.1.82

## Cause and correction

The original Induction Furnace recordings contain no silent section at the
startup/working boundary. Startup was a separate one-shot source: its completion
was polled on the client tick, then the loop was queued and decoded for another
audio channel. That handoff could produce a short audible pause.

Machines with both a startup and a working recording now use one cancellable
streaming source. The startup PCM is followed directly by the ordinary looping
stream, including when the boundary falls inside an audio buffer. The channel is
not replaced when startup ends. Electric and Induction Furnaces use this shared
path; machines without startup recordings keep their existing playback.

Original Ogg assets, volume, pitch, machine state, EU use and processing speed are
unchanged. The existing ownership/cancellation lifecycle also applies to the combined
source. A cancelled asynchronous load closes its streams and yields no playable PCM.
Resource-pack recordings with incompatible audio formats fall back to the normal
working loop instead of combining incompatible samples.

## Validation

Four PCM stream tests verify exact byte continuity through a partial boundary,
an exact boundary, an empty startup, and closing both streams once on interruption.

The real client smoke run records the actual streaming-source events. Both
furnaces retain the same live source before and after their startup duration,
without another channel-start event. The pass also checks all six native machines
and eight Addons Advanced Machines for queued/buffering cancellation, startup
interruption, ordinary processing, blocked output, power loss, restoration and
block removal. No operating source may survive an interruption/removal.

Markers: `IC2R_GAPLESS_MACHINE_SOUND_PASS` and `IC2R_MACHINE_SOUND_CLIENT_PASS`.
The final NeoForge 21.1.234 build passed all 33 unit tests. The paired NeoForge
21.1.264 run with Addons 1.4.7 passed all 584 required native server tests.
See [the validation record](validation/21.1.82.json).
