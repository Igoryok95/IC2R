# Machine sound lifecycle — 21.1.80

## Cause and correction

A pending sound request could still run after its owner stopped. Minecraft also
loads audio buffers asynchronously: a source stopped before decoding completed
could start afterwards. A single stop command did not cancel that late playback.

Sound requests now carry a playback generation. Superseded requests are discarded,
and positioned sounds remain explicitly cancelled through the native tickable
sound lifecycle until Minecraft releases their channels. Completion listeners belong
to their sound controller, survive replacement of a playback instance and run once.
Stopping clears them; a late callback cannot restart an inactive or removed machine.
Repeated inactive ticks do not enqueue redundant stop work.

The same controller serves IC2R machines and inherited Addons machine sounds.
Entity-tracking sounds use the same cancellation and completion rules. Global and
owner cleanup stop registered controllers, including collected owners.

The audit also found two operation-state problems:

- Standard machines validated a cached recipe using only its input. They now recheck
  the complete recipe, including available output space and fluid containers/tanks.
- An Induction Furnace could resume partial work after power returned without
  restoring its active flag. Actual processing now restores that flag and sound.

## Validation

The opt-in `machineSoundSmoke` development run exercises the real client SoundEngine,
not only a GUI flag. It covers cancellation before queue flush and during initial
buffer loading, input removal just after startup, successful processing, blocked
output, power loss, restoration and block removal. It checks that no operating
sources remain registered after interruption/removal.

The paired pass includes the canner, macerator, compressor, extractor, electric
furnace, induction furnace and all eight Addons Advanced Machines. Machines that
define no operating-loop event are checked for their existing silent behavior.
A standalone IC2R pass covers the six native machines without Addons.

See [the machine-readable validation record](validation/21.1.80.json).
