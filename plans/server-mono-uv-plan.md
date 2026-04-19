# Server + MCP Monorepo Plan (uv Workspace)

This plan combines:
- `server/` (FastAPI backend)
- `mcp_server/` (MCP service)

into a single Python monorepo using `uv`, while keeping services independently runnable.


## 1) Objective

Create one backend workspace so shared Python tooling, dependency management, and CI are centralized, while backend API and MCP remain separate applications.


## 2) Current state in this repo

- `server/` has FastAPI app and its own `pyproject.toml`.
- `mcp_server/` has MCP app and its own `pyproject.toml`.
- Both already use `uv.lock` independently.


## 3) Target structure

```text
.
├── backend
│   ├── pyproject.toml         # workspace root (uv)
│   ├── uv.lock                # single lockfile for backend workspace
│   ├── apps
│   │   ├── api                # moved from server
│   │   │   ├── pyproject.toml
│   │   │   ├── app/
│   │   │   └── main.py
│   │   └── mcp                # moved from mcp_server
│   │       ├── pyproject.toml
│   │       └── src/
│   ├── packages
│   │   └── common             # optional shared python package
│   │       ├── pyproject.toml
│   │       └── src/paisa_common/
│   └── .venv
```


## 4) Workspace root config (`backend/pyproject.toml`)

Use uv workspace members for both apps.

Example shape:

```toml
[project]
name = "paisa-backend-workspace"
version = "0.1.0"
requires-python = ">=3.12"

[tool.uv.workspace]
members = [
  "apps/api",
  "apps/mcp",
  "packages/common"
]
```

Notes:
- `packages/common` is optional at first; include once shared code exists.


## 5) Per-app package identities

Keep separate app packages:

### `apps/api/pyproject.toml`
- name: `paisa-api`
- dependencies: FastAPI, Beanie, Motor, auth/mail libs
- script entrypoint example: `paisa-api = "main:main"` (or uvicorn command in scripts)

### `apps/mcp/pyproject.toml`
- name: `paisa-mcp`
- dependencies: fastmcp, httpx, jwt, dotenv
- existing script can stay: `paisa-mcp = "paisa_mcp.server:main"`


## 6) Shared code package (`packages/common`) - recommended

Create a reusable internal package for overlap between API and MCP:

Potential shared modules:
- auth token validation helpers
- API client wrappers
- DTO/shared models (where appropriate)
- config loading primitives
- logging setup

This reduces duplication and keeps MCP/API behavior consistent.


## 7) Dependency and lock strategy

1. Use one workspace lockfile at `backend/uv.lock`.
2. Run installs/sync from `backend/` root.
3. Keep app-specific dependencies declared in each app's `pyproject.toml`.
4. Put only truly shared tooling in root dependency groups (lint/type/test).


## 8) Environment management

Keep separate env files per app:
- `backend/apps/api/.env`
- `backend/apps/mcp/.env`

Optional shared env template:
- `backend/.env.example` with namespaced variables.

Recommended prefixes:
- `API_*` for FastAPI app
- `MCP_*` for MCP app
- `COMMON_*` for shared settings


## 9) Run commands (workspace style)

From `backend/` root, use per-member execution.

Examples (conceptual):
- run API dev server
- run MCP dev server
- run tests/lint for one member or all members

Add a small `Makefile` or task runner script to simplify team usage:

```make
api:
	uv run --package paisa-api python apps/api/main.py

mcp:
	uv run --package paisa-mcp paisa-mcp

lint:
	uv run ruff check .
```


## 10) Migration steps

### Phase 1: create backend workspace shell
1. Create `backend/` folder.
2. Move `server -> backend/apps/api`.
3. Move `mcp_server -> backend/apps/mcp`.
4. Add root workspace `pyproject.toml`.

### Phase 2: normalize app metadata
1. Rename API project from generic `server` to `paisa-api`.
2. Confirm MCP project name remains `paisa-mcp`.
3. Ensure both have clear entrypoints.

### Phase 3: unify lock + env docs
1. Regenerate single `backend/uv.lock`.
2. Add per-app env examples and onboarding docs.

### Phase 4: extract shared package
1. Create `backend/packages/common`.
2. Move duplicate helpers from API/MCP.
3. Reference `paisa-common` from both apps.


## 11) CI/CD plan

Use matrix or staged jobs:

1. Lint/type checks (workspace-wide)
2. API tests
3. MCP tests
4. Build/package checks for both apps

Deploy separately:
- API deploy pipeline from `backend/apps/api`
- MCP deploy pipeline from `backend/apps/mcp`


## 12) Risks and mitigations

1. **Dependency conflicts** between API and MCP
   - Mitigation: keep strict per-app dependencies; avoid over-sharing.

2. **Import breakage after moving folders**
   - Mitigation: update module paths and run tests immediately after move.

3. **Env variable collisions**
   - Mitigation: namespaced env vars and per-app `.env` files.

4. **Operational coupling fear**
   - Mitigation: keep independent run/deploy scripts and clear ownership.


## 13) Definition of done

1. `backend/` workspace runs both API and MCP from one uv workspace.
2. Single lockfile and shared tooling setup.
3. API and MCP still deploy and run independently.
4. Optional `paisa-common` package used for shared backend logic.
