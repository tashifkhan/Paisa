// Runs SQLite in a web worker for androidx.sqlite's WebWorkerSQLiteDriver.
// The message protocol is documented on WebWorkerSQLiteDriver.
//
// Databases live in the origin private file system through the
// "opfs-sahpool" VFS. Unlike the plain "opfs" VFS it needs no
// SharedArrayBuffer, so the page doesn't need cross-origin isolation
// headers, which would break the Google sign-in popup later.
import sqlite3InitModule from '@sqlite.org/sqlite-wasm';

const databases = new Map();
const statements = new Map();
let nextDatabaseId = 1;
let nextStatementId = 1;

const ready = (async () => {
  const sqlite3 = await sqlite3InitModule();
  let pool = null;
  try {
    pool = await sqlite3.installOpfsSAHPoolVfs({ name: 'paisa' });
  } catch (e) {
    // Private windows and very old browsers have no OPFS. The data then
    // lasts only as long as the tab.
    console.warn('Paisa: OPFS is unavailable, using an in-memory database', e);
  }
  return { sqlite3, pool };
})();

function open({ sqlite3, pool }, fileName) {
  const db = pool ? new pool.OpfsSAHPoolDb(`/${fileName}`) : new sqlite3.oo1.DB(':memory:', 'c');
  const databaseId = nextDatabaseId++;
  databases.set(databaseId, db);
  return { databaseId, persistent: pool !== null };
}

function prepare({ databaseId, sql }) {
  const db = databases.get(databaseId);
  if (!db) throw new Error(`No open database ${databaseId}`);
  const stmt = db.prepare(sql);
  const statementId = nextStatementId++;
  statements.set(statementId, stmt);
  return {
    statementId,
    parameterCount: stmt.parameterCount,
    // getColumnNames() throws for statements with no result columns,
    // such as CREATE or INSERT.
    columnNames: stmt.columnCount > 0 ? stmt.getColumnNames() : [],
  };
}

function step({ sqlite3 }, { statementId, bindings }) {
  const stmt = statements.get(statementId);
  if (!stmt) throw new Error(`No prepared statement ${statementId}`);
  stmt.reset(true);
  if (bindings && bindings.length > 0) {
    // The driver leaves holes for parameters it never bound; those are NULL.
    stmt.bind(Array.from(bindings, (v) => (v === undefined ? null : v)));
  }
  const rows = [];
  const columnTypes = [];
  while (stmt.step()) {
    if (rows.length === 0) {
      for (let i = 0; i < stmt.columnCount; i++) {
        columnTypes.push(sqlite3.capi.sqlite3_column_type(stmt.pointer, i));
      }
    }
    rows.push(stmt.get([]));
  }
  return { rows, columnTypes };
}

function close({ statementId, databaseId }) {
  if (statementId != null) {
    statements.get(statementId)?.finalize();
    statements.delete(statementId);
  }
  if (databaseId != null) {
    databases.get(databaseId)?.close();
    databases.delete(databaseId);
  }
}

self.onmessage = async (event) => {
  const { id, data } = event.data;
  try {
    const env = await ready;
    let result;
    switch (data.cmd) {
      case 'open':
        result = open(env, data.fileName);
        break;
      case 'prepare':
        result = prepare(data);
        break;
      case 'step':
        result = step(env, data);
        break;
      case 'close':
        close(data);
        return; // one-way
      default:
        throw new Error(`Unknown command ${data.cmd}`);
    }
    postMessage({ id, data: result });
  } catch (e) {
    postMessage({ id, error: e?.message ?? String(e) });
  }
};
