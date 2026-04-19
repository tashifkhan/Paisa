# Local-First Plan (No Account Required) for Paisa

This plan defines how Paisa should work fully offline and account-free by default, with optional cloud features.

Core product direction:
- User can start app and use all personal finance features without creating an account.
- Cloud sync is optional and only enabled if user signs in.
- Bill split and owe tracking must also work locally first.
- For collaborative groups, user can optionally search online users by email and share invite links.


## 1) Product rules (non-negotiable)

1. First launch must never block on auth.
2. Local datastore is the source of truth at startup.
3. Sign-in is an enhancement (sync/collab), not a requirement.
4. Every split/debt feature must have an offline-only local mode.
5. Online-only actions (email search, remote invites, shared group) should be clearly labeled.


## 2) User modes

### Mode A: Local-only (default)
- No account.
- All transactions, wallets, categories, debts, and local groups stored on device.
- No background cloud sync.

### Mode B: Connected (optional)
- User signs in.
- Cloud sync enabled.
- Can use online collaboration features:
  - email search for members,
  - shared groups,
  - invite links.

### Mode C: Hybrid
- User signed in, but keeps some data local-only (optional future enhancement via per-group or per-wallet sync flags).


## 3) Data architecture changes

## 3.1 Local DB becomes primary runtime layer
Use local DB in `app_expo` as canonical runtime source (SQLite/Drizzle/Watermelon/Realm; choose one).

Minimum local entities:
- `transactions_local`
- `wallets_local`
- `categories_local`
- `debts_local`
- `groups_local`
- `group_members_local`
- `group_expenses_local`
- `sync_queue`
- `sync_state`

Common metadata fields for local-first sync:
- `id` (local UUID)
- `remote_id` (nullable)
- `is_local_only` (bool)
- `sync_status` (`pending|synced|failed|conflict`)
- `updated_at_local`
- `deleted_at_local` (soft delete)
- `device_id`

## 3.2 Server is optional replication target
Backend remains source for shared/cloud data, but app never depends on server availability for core usage.


## 4) Auth and onboarding flow redesign

### First launch
1. Show quick welcome.
2. Ask preferred currency/language only.
3. Enter app immediately.
4. Show "Connect account for sync/collaboration" as dismissible banner.

### Settings updates
Add section: `Data & Sync`
- `Mode`: Local only / Connected
- `Sign in to enable cloud sync`
- `Backup & restore` (local export/import)
- `Sync now` (only if connected)


## 5) Split and owe logic: local-first design

## 5.1 Local split groups (offline)
User can create local groups without online identities.

Member model in local groups:
- `member_type`: `self | local_contact | online_user`
- `display_name` (required)
- `email` (optional)
- `remote_user_id` (nullable)

This supports:
- "You + Alice + Bob" split calculations offline,
- debt simplification offline,
- settle-up tracking offline.

## 5.2 Owe/debt engine local-first
All debt calculations run locally for both personal debts and group balances.

Algorithms stored in shared logic package should be pure and deterministic.

## 5.3 Promotion from local group -> online group
When connected, user can "Upgrade to shared group".

Migration behavior:
1. Create remote group.
2. Upload local group metadata + expenses + balances.
3. Map local members:
   - if linked to online users, attach directly,
   - otherwise keep as pending invite placeholders.


## 6) Online member discovery and invites

## 6.1 Email search fallback behavior
In group member add flow (connected mode):

1. User enters email.
2. App checks remote user search endpoint.
3. If found: add as online member.
4. If not found: show options:
   - `Create as local contact` (still split now)
   - `Send invite link` (share link)

## 6.2 Invite link design
Add backend endpoint for invite links:
- `POST /groups/{group_id}/invite-links`
- `GET /invite/{token}`
- `POST /invite/{token}/accept`

Invite link payload should include:
- group id,
- role,
- expiry,
- inviter id,
- optional target email.

App behavior:
- Share via native share sheet.
- Receiver opens deep link to app/web join flow.
- If receiver has no account, allow signup then auto-join.


## 7) Sync model

## 7.1 Outbox queue
All writes hit local DB first, then enqueue sync tasks when connected.

`sync_queue` item:
- `entity_type`
- `entity_id`
- `operation` (`create|update|delete`)
- `payload_snapshot`
- `attempt_count`
- `last_error`

## 7.2 Sync triggers
- app foreground,
- manual sync action,
- periodic background task (when possible),
- network reconnect.

## 7.3 Conflict strategy (v1)
- last-write-wins using `updated_at` + server version check.
- keep conflict log for user visibility.

## 7.4 Deletion strategy
- soft delete locally first,
- propagate tombstone on next sync,
- purge tombstones after retention period.


## 8) API changes required in `server/`

1. Add robust sync endpoints for all entities (idempotent upsert).
2. Add user search endpoint by email (already partly present, extend as needed).
3. Add invite link endpoints.
4. Support unresolved/pending members in group model.
5. Add endpoint to convert local member placeholder to real user after join.


## 9) UX changes in app

1. Remove auth gate from app root navigation.
2. Add optional sign-in CTA in profile/settings only.
3. Add local/connected badges in group screens.
4. In Add Member flow, include:
   - search by email,
   - create local contact,
   - share invite link.
5. Add pending invite state in group member list.


## 10) Security and privacy

1. Local mode should not send finance payloads to server.
2. Invite links should be signed, expiring, and revocable.
3. Email search should be rate-limited to prevent enumeration abuse.
4. Device-local data should be encrypted-at-rest where feasible.


## 11) Migration strategy for existing users

1. Existing authenticated users remain connected mode.
2. On update, create local mirror from current synced data.
3. If user logs out, keep local data by default and disable sync.
4. Provide explicit "Delete local data" action separately.


## 12) Implementation phases

### Phase 1: Local-first foundation
1. Add local DB schema + repository layer.
2. Route all CRUD through local repositories.
3. Remove mandatory auth routing from startup.

### Phase 2: Sync + connected mode
1. Build outbox sync queue.
2. Implement idempotent sync endpoints.
3. Add settings toggle and sync status UI.

### Phase 3: Local-first group split/owe
1. Local group/member/debt models.
2. Offline split and simplify-debt logic.
3. Local->online group promotion flow.

### Phase 4: Email search + invite links
1. Add member search-by-email flow.
2. Add fallback local contact creation.
3. Add invite link generation, sharing, and accept flow.

### Phase 5: Hardening
1. Conflict handling improvements.
2. Observability + sync diagnostics.
3. Recovery tools (replay queue, export debug bundle).


## 13) Acceptance criteria

1. New user can record transactions and manage wallets without account.
2. Split/owe works fully offline with local contacts.
3. Sign-in can be done later with no data loss.
4. Connected users can search email and add online members.
5. If email not found, user can still split locally or share invite link.
6. Invite link recipients can join group after signup.
7. App remains usable when server is down.
