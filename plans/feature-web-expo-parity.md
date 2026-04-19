# Feature + UI Parity Plan (Website vs Expo)

This plan compares `website/` and `app_expo/` and defines how to reach strong parity.

Current observation: Expo app is ahead in design polish, interaction quality, and information density.


## 1) Scope

Parity dimensions:
1. Feature parity (flows and capabilities)
2. UI parity (layout quality, state handling, visual hierarchy)
3. Interaction parity (navigation, gestures, transitions, feedback)
4. Data parity (same entities and action outcomes)


## 2) Current state summary

### Expo strengths (ahead)
- Native-feeling tab shell and FAB behavior (`app_expo/app/(tabs)/_layout.tsx`)
- Richer home dashboard composition and card treatment (`app_expo/app/(tabs)/index.tsx`)
- More advanced analytics visuals and controls (`app_expo/app/(tabs)/stats.tsx`)
- Better social debt/group cards and net summary treatment (`app_expo/app/(tabs)/social.tsx`)
- Stronger wallet carousel and card presentation (`app_expo/app/(tabs)/wallets.tsx`)

### Website strengths
- Broader view surface and desktop layout potential (`website/src/App.tsx` + view components)
- Existing Data Management view with import/export UX (`website/src/components/views/DataManagement.tsx`)
- Mature auth route coverage in browser flows (`website/src/components/views/AuthViews.tsx`)


## 3) Parity gaps (high priority)

## 3.1 Navigation architecture gap
- Web currently uses manual route/view glue in a large `App.tsx`.
- Expo uses clearer route groups + modular screens.

Plan:
1. Refactor web into route-first shell with smaller per-view containers.
2. Standardize nav state model across web/mobile (tabs + modal intent mapping).

## 3.2 Home dashboard quality gap
- Expo home has stronger balance card composition and recent transaction interactions.
- Web home is good, but visual hierarchy and component consistency can be tightened.

Plan:
1. Align card hierarchy and spacing tokens.
2. Port high-value UX patterns (hide balance toggle clarity, concise trend chips, consistent action affordances).

## 3.3 Stats/analytics gap
- Expo stats has richer modes (line/bar/heatmap, more filter controls, stronger sectional layout).
- Web stats currently has simpler, tabbed blocks and different chart behavior.

Plan:
1. Unify analytics information architecture.
2. Add equivalent chart mode switching on web.
3. Normalize period/type filters and labels across both clients.

## 3.4 Social split/owe gap
- Expo social uses cleaner debt/group card system with stronger state cues.
- Web social has mixed tabs and includes contacts/search mechanics, but visual coherence is behind.

Plan:
1. Keep web-specific contacts/search strength.
2. Upgrade visual system and list/card consistency to Expo quality.
3. Standardize debt state badges and settlement interactions.

## 3.5 Wallet experience gap
- Expo has card carousel, active wallet detail, and transaction section flow.
- Web wallet view is simpler card grid with less contextual detail.

Plan:
1. Add "active wallet" detail panel on web.
2. Show per-wallet trends and recent transactions inline.
3. Align card metadata and action affordances.

## 3.6 Profile/settings gap
- Expo profile has better segmented settings presentation with dialogs.
- Web profile is functional but less refined in interaction depth.

Plan:
1. Add dialog-based setting changes on web where appropriate.
2. Align settings taxonomy (General, Notifications, Data, AI & Privacy).


## 4) Parity matrix (snapshot)

- Home: `Expo > Web`
- Stats: `Expo > Web`
- Wallets: `Expo > Web`
- Social/Debts/Groups: `Expo > Web` (web has extra search path)
- Profile: `Expo > Web`
- Data management: `Web > Expo`
- Auth flows: `Web ~= Expo` (different implementation style)


## 5) Design-system parity strategy (without shared UI components)

Because UI components are not shared, parity should come from shared design contracts:

1. Shared design tokens spec (spacing, radii, typography scale, semantic colors).
2. Shared UX patterns doc (card anatomy, empty/loading/error states, list density).
3. Shared copy dictionary for core labels.
4. Platform adapters implement same contract in different component systems.


## 6) Feature parity backlog

### P0 (must-have)
1. Align tab destinations and page naming across web/mobile.
2. Bring web Home to same information hierarchy as Expo.
3. Bring web Stats to include mode switching + stronger chart views.
4. Bring web Wallets to include active wallet details + recent tx panel.
5. Preserve web Data Management and add equivalent entry in Expo.

### P1
1. Align social debt/group settlement affordances.
2. Align profile/settings IA and dialogs.
3. Standardize empty/loading skeletons.

### P2
1. Micro-interactions parity (transitions, haptics-equivalent feedback cues).
2. Performance parity benchmarking and polish.


## 7) Implementation phases

### Phase 1: Baseline audit and contracts
1. Freeze parity checklist by screen and feature.
2. Define design tokens + state patterns shared doc.
3. Create acceptance screenshots for both clients.

### Phase 2: Web uplift (match Expo quality)
1. Refactor web shell routing and view composition.
2. Upgrade Home/Stats/Wallets visual and interaction parity.
3. Add missing detail panels and chart controls.

### Phase 3: Expo catch-up on web strengths
1. Ensure Data Management parity in Expo flow.
2. Keep or improve social contacts/search affordances where needed.

### Phase 4: Continuous parity guardrail
1. Add parity checklist to PR template.
2. Add monthly parity review snapshot.


## 8) Measurement and acceptance

Parity is considered successful when:
1. No P0 screen has a functional mismatch.
2. Both clients expose equivalent data actions and outcomes.
3. Visual hierarchy and state behavior are consistent to product standards.
4. Internal review labels parity status as `High` for Home/Stats/Wallets/Social/Profile.


## 9) Immediate next actions

1. Implement a web redesign pass for Home/Stats/Wallets using Expo as quality benchmark.
2. Write a per-screen parity checklist document with before/after screenshots.
3. Integrate this parity plan into roadmap tracking in root `README.md`.
