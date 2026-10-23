# Player own nametag command

## SPEAR spec

REQ-023: WHEN a permitted player runs /nametag without arguments THE SYSTEM SHALL toggle only that player's saved own-tag viewing preference.

REQ-024: WHEN a permitted player runs /nametag on or off THE SYSTEM SHALL set that same saved preference explicitly and provide clear feedback.

REQ-025: WHILE both global own-tag controls are disabled THE SYSTEM SHALL reject enabling with a server-disabled message and preserve the existing preference.

REQ-026: WHEN a sender uses the short command THE SYSTEM SHALL require existing unt.preferences and unt.showownnametag permissions, reject console use, and expose no target-player or administrative operations.

REQ-027: WHEN a player uses the short command THE SYSTEM SHALL preserve movement suppression, other viewers' visibility, all other preferences, existing commands and persistent data keys.

## Prove and engine

Authoritative FainNeito main5cf4877829ac18e3fc3c9c04d046b60362f861e0 was refetched before this isolated branch. The existing `/unt preferences showown true|false` writes the saved preference but offers no no-argument toggle and can report a true preference while global policy prevents viewing. This is static source evidence, not historical red/green test evidence.

Add standalone `/nametag [on|off]` and `/unt own [on|off]` using one shared command builder/handler. No arguments invert the existing saved preference. Enabling is allowed only when showCurrentNameTag or allowPerPlayerShowOwnWhenGlobalDisabled permits it; disabling remains allowed. Existing manager/PDC methods perform persistence and refresh. No new permissions, admin grants, rank updates, data keys or migrations.

## Architecture and acceptance

This is a Paper command adapter change using the current lifecycle/Brigadier registration API and existing application preference operations. The helper only accepts the executing player. Own-tag movement safety and global show eligibility remain authoritative. Existing long preference/admin commands remain compatible. A conflicting standalone command can be handled through Paper's normal namespaced command registration; `/unt own` remains an alternative.

No repository-local EARS/state helper exists; this requirement/task/evidence record is maintained without claiming helper success. No tests are added or manually run for this request. Existing hosted CI runs automatically and its exact-head outcome is recorded separately. Native command discovery, nonstaff permissions, saved preference after reconnect/reload, global-off rejection, movement suppression and cross-viewer independence require in-game acceptance. No merge/deploy/reload/restart or production permission/config change is authorized by this source task. Production's prior global own-tag disable remains unchanged.

## Player controls

- `/nametag` or `/unt own`: toggle your saved own-tag preference.
- `/nametag on` or `/unt own on`: enable, subject to the server's global policy.
- `/nametag off` or `/unt own off`: disable.

Both `unt.preferences` and `unt.showownnametag` already default to true. Operators do not need to grant any admin permission to expose these controls. The command does not enable global own-tag viewing itself.
