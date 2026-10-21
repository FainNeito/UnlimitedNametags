# Stale nametag cleanup

### REQ-001 - Player removal
WHEN an owner removal packet is sent THE SYSTEM SHALL include that owner's current and retired nametag row IDs in the removal packet.

### REQ-002 - Viewer release
WHEN a nametag row is hidden THE SYSTEM SHALL destroy its viewer display before forgetting its wrapper, including blocked viewers.

### REQ-003 - Session isolation
WHEN a player respawns or changes world THE SYSTEM SHALL invalidate old connection state before mounting new rows.

### REQ-004 - Client independence
THE SYSTEM SHALL send nametag cleanup packets without requiring changes to client EntityCulling settings.

## Acceptance still required
### REQ-005 - Obscured presentation
WHEN placeholders refresh THE SYSTEM SHALL preserve each viewer's obscured opacity and wall visibility until their line of sight changes.

### REQ-006 - Visibility recovery
WHEN invisibility or spectator mode ends THE SYSTEM SHALL reconcile tracking and recover nametags after the new player state takes effect, only for the current online session.

### REQ-007 - Hidden rows
WHILE HIDE mode has no line of sight or exceeds its configured range THE SYSTEM SHALL reject nametag spawning through every show path.

### REQ-008 - Recording preference
WHILE a viewer has disabled other players' nametags THE SYSTEM SHALL remove only those nametag displays and preserve the viewer's separate own-tag preference.

### REQ-009 - Water rendering mitigation
WHERE preferNormalTextWithLineOfSight is enabled WHILE a viewer has clear line of sight THE SYSTEM SHALL render SEE_THROUGH text with normal depth and retain configured wall visibility for blocked viewers, without altering text opacity.

### REQ-010 - Pose reconciliation
WHEN a visible nametag owner changes pose THE SYSTEM SHALL retry the owner's authoritative pose metadata after the event takes effect, only to current sessions that still track that owner.

### REQ-011 - Pose integrity
THE SYSTEM SHALL preserve legitimate swimming, crawling, crouching, sleeping, and flying poses and SHALL NOT change server pose or hitboxes to reconcile a viewer.

### REQ-012 - Own-tag movement suppression
WHILE an owner is swimming, prone/crawling, gliding, spin-attacking, or flying THE SYSTEM SHALL suppress that owner's nametag rows only for that owner when hideOwnNametagWhileSwimmingOrFlying is enabled.

### REQ-013 - Preference-preserving recovery
WHEN the owner leaves a suppressed movement state THE SYSTEM SHALL reconcile own-tag visibility from the current online session and saved preferences without changing other viewers, permissions, or persistent preferences.

### REQ-014 - Owner show-path safety
WHILE movement suppression applies THE SYSTEM SHALL reject owner display spawning through direct spawn, show, refresh, recovery, and delayed elytra paths without a fixed-delay forced reveal.

Vanilla clients do not report first/third-person camera selection. The user chose temporary own-tag suppression in every camera view for these states. The new setting defaults true; disabling it restores previous movement eligibility. Server-observable SWIMMING covers crawling/prone with that pose; client-only pose mods need separate acceptance. Visual previews simulate policy decisions, not Minecraft rendering.

### REQ-015 - Movement entry ordering
WHEN a current owner's swimming, horizontal pose, glide, flight, or Elytra input transition starts THE SYSTEM SHALL retain temporary owner-only suppression across refresh and recovery until the entry settling window completes, even before server pose flags update.

### REQ-016 - Bounded transition recovery
WHEN an entry settling callback completes THE SYSTEM SHALL release only its matching owner-session transition and reconcile current movement, visibility and saved preferences, including failed or aborted starts.

The settling window is five owner-scheduler ticks. It prevents premature server-side respawn; it cannot retroactively hide a client-predicted frame before the server receives input. The user approved earlier suppression while in water or wearing Elytra on 2026-10-20.

### REQ-017 - Anticipate horizontal movement
WHERE hideOwnNametagWhileInWaterOrWearingElytra is enabled WHILE movement hiding is enabled and an owner is in water or wearing an Elytra THE SYSTEM SHALL suppress only that owner's own rows before swimming or gliding starts and reconcile visibility when those conditions end.

Earlier suppression defaults enabled. It covers equipped Elytra even on land and water contact before swimming. It applies in all cameras, preserves other viewers and preferences, and does not require a client mod. Actual client transition acceptance remains required, especially simultaneous equipment/water entry and pose prediction under latency.

Reproduce teleports, world transfers, tracking-distance removal, vanish/invisibility, quit/rejoin, and repeated transfers with EntityCulling enabled and disabled. Observe the player and every row from a second client. See verification.md for current local evidence and remaining client acceptance; compilation alone does not establish either.
