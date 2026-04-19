# Paisa

Paisa is a personal finance app ecosystem (Expo mobile + web + backend) focused on fast expense tracking, split/owe flows, and optional cloud features.

## Goal

- Build a local-first personal finance product that works without forced sign-in.
- Add optional sync/collaboration when users choose to connect accounts.
- Keep mobile and web feature/UI parity tight while allowing platform-specific UX.

## What We Are Building

- `app_expo/`: mobile app (primary UX, currently ahead in design quality)
- `website/`: web app
- `server/`: API backend
- `mcp_server/`: MCP service
- `plans/`: implementation and architecture plans

## Roadmap (Todo)

- [ ] Implement local-first mode (no account required)
- [ ] Add optional cloud sync + outbox conflict handling
- [ ] Close web/expo feature and design parity gaps
- [ ] Add Gemini integration with strict local-only gate
- [ ] Add share-sheet import for PDF/image into Expo
- [ ] Consolidate clients into Bun monorepo structure
- [ ] Consolidate backend + MCP into uv workspace
