// Resolve the SQLite worker straight from web/sqlite-worker, so edits land on
// the next build. As an npm "file:" dependency, Yarn copied it once and kept
// serving the stale copy.
const path = require('path');
const repoRoot = path.resolve(__dirname, '../../../..');
config.resolve = config.resolve || {};
config.resolve.alias = Object.assign({}, config.resolve.alias, {
  'paisa-sqlite-worker': path.join(repoRoot, 'web/sqlite-worker'),
});
// The worker imports @sqlite.org/sqlite-wasm. Find it in the Kotlin/Wasm
// node_modules, since web/sqlite-worker has none of its own.
config.resolve.modules = (config.resolve.modules || ['node_modules']).concat([
  path.resolve(__dirname, '../../node_modules'),
]);
