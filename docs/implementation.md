# Nametag lifecycle and rendering design

## Entry flash follow-up, 2026-10-20

The user reports a brief own-tag flash when entering swimming and flying poses on both TEST and production. Server-side eligibility previously depended solely on already-updated pose/flags, despite entry events being able to precede those updates. Seven new behavior assertions fail on unchanged d62ee79: five transition ordering cases and two earlier-hiding cases. This proves a server eligibility gap and missing approved policy, not the causal ordering of the recorded client frame.

Entry now marks a session/token guard before destroying owner rows. All owner show paths share the existing movement eligibility gate, so a refresh during pre-update flags cannot restore them. A five-owner-tick callback removes only its own token, then rechecks session, current pose/flags and normal visibility. Failed starts recover without a permanent block; superseded callbacks cannot release newer entries. Pose, swim, glide, creative-flight and Elytra input use the same latch. Client entity-action IDs must match the sender, and cancelled packets are ignored.

The user approved earlier hiding while in water or wearing Elytra. The new default-true visibility.hideOwnNametagWhileInWaterOrWearingElytra option extends the existing movement master switch. The owner eligibility adapter applies this policy to every spawn path. A one-tick entity-scheduled watch hides visible owner rows after water/equipment changes and reconciles on exit; it does not repeatedly destroy already-hidden rows. Watches start for existing players and joins, cancel on replacement/quit/close, and reject replaced/invisible/dead/spectator sessions. An accepted move into a WATER block also marks the entry before isInWater updates; waterlogged contacts use the authoritative water flag/watch.

Only the owner's copy is suppressed. No pose, hitbox, permissions, preferences or other-viewer row state changes. No persistence migration is needed. Local wrapper builds use the declared Paper 26.2/Java 25 toolchain and separately verify the installed PacketEvents 2.14.0 API. There is no network submodule pin for this standalone plugin. Production and shared TEST remain unchanged in this implementation task.

The policy removes the need to wait for horizontal pose when water/equipment is already known. Client prediction before the first water/equipment update still cannot be retroactively undone by a server plugin; acceptance must include immediate equip-and-glide and sprinting directly into water under realistic latency. No guarantee across arbitrary client mods or network conditions is inferred from unit tests.

## Spec and prove

Requirements are in requirements.md. Reports establish stale rows at an old player location, inconsistent OBSCURED rendering, and failure to recover after invisibility/spectator transitions. Screenshots and reported relog recovery establish symptoms, not a complete causal packet trace. The baseline already has viewer cleanup/session regression tests; they are existing evidence, not historical red/green evidence for this work.

## Engine and architecture

- Common code registers an allocated row through a default runtime hook, preserving other platform adapters. The Paper packet adapter retains owner/row associations and expands owner removal packets to include current and retired displays.
- Per-viewer OBSCURED state checks actual metadata before skipping an update and caches only successful writes. Placeholder/style refreshes preserve per-viewer wall state.
- Player recovery waits until potion/gamemode changes take effect on the owner's scheduler, reconciles actual tracking, and rejects callbacks from old connections or currently suppressed owners. The entity-scoped UniversalScheduler overload preserves Paper/Folia scheduling ownership.
- The Paper visibility adapter applies HIDE range/line-of-sight policy to every spawn path. Viewer-owned recording preferences destroy allocated other-player rows without changing own-tag preference or other entities.
- Existing config defaults, permissions, storage, commands, platform ownership, and passenger ordering remain intact. No client culling configuration is changed.

## Refine and delivery

Verification and unresolved acceptance are in verification.md. The Gradle wrapper runs under JDK 23 while compilation/tests use the declared JDK 25 toolchain. External Gradle is not the canonical delivery path. UnlimitedNametags is not listed in the current network .gitmodules, so no submodule pin change is included.

## Ticket 388 follow-up

- `visibility.preferNormalTextWithLineOfSight` is opt-in (false by default). For SEE_THROUGH text, clear-sight viewers use depth-tested text; blocked viewers retain the row's configured wall visibility. Opacity, background, permissions, and other modes retain their existing behavior. Metadata refreshes preserve per-viewer flag ownership; tick, text refresh, sneak and reload paths reconcile it.
- The common text layer owns presentation. The Paper adapter schedules the new line-of-sight work on the viewer's scheduler, rechecking current session, feature settings, and row membership before applying it. Text refresh schedules only its viewer, avoiding a repeated full-viewer traversal.
- The Paper owner scheduler retries authoritative pose metadata one tick after `EntityPoseChangeEvent`. Bukkit SNEAKING maps to protocol CROUCHING; other supported poses are preserved. Only owners with nametag rows participate. This writes only metadata index 6 with ENTITY_POSE through normal packet listeners; it never sets server pose, swimming flags, collision state, or a hitbox.
- Pose retries carry an owner transition token and reject replaced sessions and later transitions. The socket callback also checks the viewer's current connection, tracking veto, connection state, and exact owner-spawn generation. Removal/re-spawn of the same entity ID invalidates the older snapshot.
- This is mitigation/reconciliation, not a demonstrated root-cause reproduction of either ticket symptom. The inspected local Minecraft 26.3 RemotePlayer does not calculate its own pose; Entity pose metadata drives dimensions. The reported client's exact version and mods remain unknown. Server/client acceptance is separate.

No production configuration, files, console, or process is touched by this follow-up. See ticket-388-acceptance.md for the staging-only configuration and remaining client evidence.

## Own-tag movement guard

Requirements REQ-012 through REQ-014 add temporary owner-only suppression for swimming/crawl/prone, horizontal flight/spin poses and swimming/gliding/flying flags. Preference calculation and persistence stay separate. Entry hides the owner's allocated display; recovery runs on the current owner's scheduler and reads the latest state. Normal platform eligibility and both common direct owner-spawn paths enforce the guard; a non-spawned owner metadata template remains for other viewers and item/block initialization. No forced five-tick reveal, global row removal, pose mutation, permission change or preference write is introduced. The user accepted suppression in all camera views. See own-nametag-movement.md for configuration, simulation and acceptance boundaries.
