# Vanilla distance and sneaking visibility

## Spec

REQ-018: WHERE VANILLA wall mode is selected WHILE an owner is not sneaking THE SYSTEM SHALL restrict every row to viewers at most 64 blocks from the owner.

REQ-019: WHERE VANILLA wall mode is selected WHILE an owner is sneaking THE SYSTEM SHALL restrict every row to viewers less than 32 blocks from the owner and with clear server line of sight.

REQ-020: WHEN distance, sneaking or line of sight changes THE SYSTEM SHALL reconcile current tracked viewers without bypassing privacy, visibility events, player preferences, online sessions or owner tracking.

REQ-021: WHERE VANILLA wall mode is selected THE SYSTEM SHALL use the existing dimmed through-wall text presentation up to 64 blocks for non-sneaking owners and preserve the configured sneak opacity.

REQ-022: WHILE VANILLA wall mode is not selected THE SYSTEM SHALL preserve existing visibility behavior and configuration defaults.

## Prove

Current canonical fork main is 0a8cda4d6e287a2f4993548ef5b71cdb5a27a804. Static inspection shows the current adapter only gates HIDE mode by range/line of sight. OBSCURED text disables see-through while sneaking but has no 32-block gate. Production configuration read during the preceding operation has viewDistance 160 (display range 1), OBSCURED with maxDistance 48, and both own-tag switches false.

Official local Minecraft 26.3 client bytecode inspected in operations/unlimitednametags-f1-20261021 shows EntityRenderer's ordinary name distance 64 and LivingEntityRenderer rejecting discrete/sneaking names at squared distance >=1024. This source evidence is not a live client reproduction. No tests were requested, added or run for this change; no historical red/green result is claimed.

## Engine and architecture

Add an opt-in VANILLA enum mode and a pure common range predicate. The Paper adapter gates every show path, and a one-tick viewer-scheduled reconciliation removes/restores all row types as players move. Current row/session membership is checked again in delayed callbacks. Sneak range reconciliation reads the authoritative state on the viewer scheduler rather than doing new line-of-sight reads on the packet-input thread. Non-sneaking text keeps the existing OBSCURED presentation, with the range fixed at 64 in this mode. Other modes and all saved preferences remain unchanged. The existing shift-system presentation bypass does not override vanilla sneaking rules in VANILLA mode.

This is distance/sneaking parity, not F1 parity or a replacement rendering architecture. Server line of sight approximates client depth occlusion; transparent blocks, latency, rapid crouch transitions and client culling need native acceptance. Client rendering or tracking can shorten visibility. A server tick cannot retroactively hide client-predicted frames. Item/block rows follow the same server visibility gate, but keep their existing depth rendering.

## Adoption and rollback

Reconciliation restores only viewer/row pairs previously suppressed by this policy. It does not retry cancelled show events every tick or reveal arbitrary manually hidden rows. Suppression state is cleared on reload, viewer removal and row retirement. This is transient state, not saved player data.

After review, build and isolated TEST acceptance, select `visibility.throughWallMode: VANILLA` and keep `behavior.viewDistance: 160`. VANILLA ignores the configured through-wall maxDistance and uses 64. Existing wall opacity and sneak opacity remain in use. Both own-tag switches remain false on production. No production configuration/JAR/reload/restart is part of this source change. Roll back by restoring the previous mode; no player data migration is needed.

## Verification and remaining work

No repository-local SPEAR EARS/state helpers exist in the inspected base. This requirement/task/evidence record is used; no helper pass is claimed. Static source review and whitespace checks are recorded with the PR. Compilation, automated regressions, hosted checks, merge, server staging and native client acceptance remain separate gates. No test/build execution is authorized by this request's scope under the active session instructions.

Native acceptance should cover 31.9/32/32.1 and 63.9/64/64.1 blocks; move both participants; crouch/stand behind solid and transparent blocks; repeat refresh/reload, reconnect, tracking removal, vanish/invisibility, disabled worlds, hidden preferences and cancelled show events. Verify every text/item/block row, EntityCulling on/off, and unchanged other modes. Check both own-tag controls remain off.
