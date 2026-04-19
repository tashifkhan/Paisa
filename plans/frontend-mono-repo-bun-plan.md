# Bun Monorepo Plan (Client Consolidation: Shared Logic Only)

This plan consolidates client code into a Bun workspace monorepo while sharing only:
- hooks
- services
- types
- utils

No shared UI components. Web and mobile keep their own design systems.


## 1) Objective

Unify `app_expo/` and `website/` into one client monorepo so both apps reuse business logic, API contracts, and utility code without coupling their UI layers.

Current repo apps:
- Mobile: `app_expo/` (Expo)
- Web: `website/` (Vite React)


## 2) Target structure

```text
.
├── apps
│   ├── mobile            # from app_expo
│   └── web               # from website
├── packages
│   └── core
│       ├── src
│       │   ├── hooks
│       │   ├── services
│       │   ├── utils
│       │   └── types
│       ├── index.ts
│       └── package.json
├── package.json          # root workspace config
├── tsconfig.base.json
└── bun.lock
```


## 3) Workspace setup (Bun)

Create root `package.json`:

```json
{
  "name": "paisa-clients",
  "private": true,
  "packageManager": "bun@1.x",
  "workspaces": ["apps/*", "packages/*"],
  "scripts": {
    "dev:mobile": "bun --filter @repo/mobile dev",
    "dev:web": "bun --filter @repo/web dev",
    "build:web": "bun --filter @repo/web build",
    "lint": "bun --filter '*' lint"
  }
}
```

Then run `bun install` at root to generate a single root `bun.lock`.


## 4) Shared logic package (`@repo/core`)

`packages/core/package.json`:

```json
{
  "name": "@repo/core",
  "version": "0.0.1",
  "private": true,
  "main": "./index.ts",
  "types": "./index.ts",
  "peerDependencies": {
    "react": ">=19"
  }
}
```

`packages/core/index.ts`:

```ts
export * from './src/hooks';
export * from './src/services';
export * from './src/utils';
export * from './src/types';
```

In app package.json files:

```json
"dependencies": {
  "@repo/core": "workspace:*"
}
```


## 5) TypeScript pathing

Create root `tsconfig.base.json`:

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@repo/core": ["./packages/core/index.ts"],
      "@repo/core/*": ["./packages/core/src/*"]
    },
    "module": "esnext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true
  }
}
```

Have `apps/mobile/tsconfig.json` and `apps/web/tsconfig.json` extend it.


## 6) What to move into `packages/core`

Move/refactor only logic modules that are truly platform-agnostic:

1. `types`
   - API DTOs from `app_expo/services/types.ts`
2. `services`
   - API clients that rely on injected transport/config
3. `utils`
   - date/currency/math/format transforms
   - hash/dedupe helpers
4. `hooks`
   - pure React hooks without direct DOM or React Native imports

Do not move:
- components
- native modules
- styling/theming code
- router/screen UI files


## 7) Platform boundary rules

Shared package must not directly import:
- `react-native`, `expo-*`
- `window`, `document`
- app-specific env APIs (`EXPO_PUBLIC_*`, `import.meta.env`)

Use dependency injection for platform specifics.

Example config contract:

```ts
export interface CoreRuntimeConfig {
  apiBaseUrl: string;
  getToken: () => Promise<string | null>;
}
```

Each app creates its own adapter and passes it into core services.


## 8) Environment variable strategy

Do not read environment variables inside `@repo/core`.

Instead:
- Web reads from `import.meta.env` in `apps/web`.
- Mobile reads from Expo config in `apps/mobile`.
- Both pass resolved values into core initializer.

This avoids `NEXT_PUBLIC_` vs `EXPO_PUBLIC_` style mismatch.


## 9) Expo (Metro) monorepo config

Update `apps/mobile/metro.config.js` to watch workspace root and resolve shared package:

```js
const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const projectRoot = __dirname;
const workspaceRoot = path.resolve(projectRoot, '../..');

const config = getDefaultConfig(projectRoot);
config.watchFolders = [workspaceRoot];
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, 'node_modules'),
  path.resolve(workspaceRoot, 'node_modules')
];
config.resolver.disableHierarchicalLookup = true;

module.exports = config;
```


## 10) Web app config (Vite in this repo)

Since `website/` is Vite (not Next.js), configure Vite to preserve symlinks and allow workspace pathing if needed.

`apps/web/vite.config.ts` (concept):

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  resolve: {
    preserveSymlinks: true
  }
});
```

If you later migrate web to Next.js, use `transpilePackages: ["@repo/core"]`.


## 11) TanStack Query sharing strategy

Recommended split:
- `@repo/core/services/*`: pure fetchers and mappers
- `@repo/core/hooks/*`: shared query hooks using TanStack Query
- per-app query client instances remain in each app shell

This keeps cache ownership local while sharing data logic.


## 12) Migration phases

### Phase 1: workspace bootstrap
1. Create `apps/` and `packages/`.
2. Move `app_expo -> apps/mobile`, `website -> apps/web`.
3. Add root workspace package.json and root lockfile.

### Phase 2: core package extraction
1. Create `packages/core`.
2. Move shared `types` first.
3. Move shared `utils`.
4. Move API services with config injection.

### Phase 3: hook extraction
1. Move cross-platform hooks only.
2. Add adapters for storage/network/platform behavior.
3. Validate no platform-specific imports in core.

### Phase 4: hardening
1. Add lint guardrails (`no-restricted-imports`) in core.
2. Add tests for shared logic.
3. Add CI matrix for mobile+web typecheck/lint/build.


## 13) Guardrails and cautions

1. React version alignment across apps is mandatory to avoid invalid hook call errors.
2. Keep `@repo/core` dependency-light; use peer deps where possible.
3. Avoid circular imports between app code and core.
4. Keep auth token storage implementation platform-owned (SecureStore vs localStorage).
5. Keep UI concerns out of shared hooks/services return types.


## 14) Definition of done

1. One Bun workspace at repo root with single `bun.lock`.
2. Both `apps/mobile` and `apps/web` compile and run from workspace scripts.
3. Shared logic consumed from `@repo/core` in both apps.
4. No shared UI components.
5. Environment/platform dependencies are injected at app boundary.
