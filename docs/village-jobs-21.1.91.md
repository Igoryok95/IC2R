# Village workstation reservations — 21.1.91

## Cause and correction

The six restored IC2 professions registered their POI types with `maxTickets=2`
and `validRange=2`. Vanilla professional workstations use `1, 1`. The extra
reservation allowed more than one villager to target the same IC2 workstation,
increasing competition and potential IC2 job acquisition beyond one employee
per generated workshop. Both parameters now match vanilla.

This does not establish that every reported village had a majority of IC2
professionals. Workshop selection and villager job acquisition are separate.
Terrain, access to job blocks, the building mix and simulation time affect the
observed population. A saved region snapshot can lag the live entity state.

The village pool remains additive. All vanilla entries keep their weights; the
IC2 workshop weights remain electric/crop/brewing/demo 2 each, nuclear/Greg 1
each. The sum is still 10 extra weight units per village style, not a percentage
of residents. No spawn replacement, village rebuilding or blanket reassignment
of vanilla residents is introduced. Addons only extends IC2 trade lists.

## Existing saves

Changing the registered capacity alone would leave old saved `free_tickets`
values in POI records. The IC2-only codec migration preserves the number of
occupied reservations while reducing capacity to one:

| Old free tickets (capacity 2) | New free tickets (capacity 1) |
| --- | --- |
| 2, no reservation | 1 |
| 1, one reservation | 0 |
| 0, two reservations | 0 |

Newly saved IC2 records contain `ic2_job_capacity=1`, preventing the migration
from subtracting a vacancy again after a later reload. Only the six restored
IC2 POI types are affected. Vanilla records keep their original format and
reservation counts. Existing residents are not deleted or rewritten; Minecraft's
normal job competition handles residents that had claimed the same block.

## Validation

All 34 unit tests and 512 required standalone GameTests passed with a successful
build. The village regression checks cover:

- All five additive house pools, unchanged vanilla totals, approved IC2 weights
  and idempotent injection.
- Actual POI claims: first employee succeeds, a second cannot reserve the same
  block, release restores exactly one vacancy.
- All six IC2 workstations and representative vanilla professional blocks.
- IC2 professions accept their own POI and reject vanilla job blocks.
- Legacy 0/1/2 free-ticket records for each IC2 role, a save/reload without a
  second reduction, and an untouched vanilla composter record.
- Existing real-jigsaw resident generation and every workstation overlay.

The production server/world was inspected read-only. This source fix and build
do not install a new mod or modify that running world's records.
