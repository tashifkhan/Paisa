// Runs worker.js locally with Node 18+, for trying Paisa web against a
// Nextcloud on your own machine or network.
//
//   NEXTCLOUD_URL=http://127.0.0.1:8090 node tools/nextcloud-proxy/serve.mjs
//
// Listens on 127.0.0.1:8787 (PORT to change).
import { createServer } from 'node:http';
import { handle } from './worker.js';

const env = {
  NEXTCLOUD_URL: process.env.NEXTCLOUD_URL,
  ALLOWED_ORIGIN: process.env.ALLOWED_ORIGIN || '*',
};
const port = Number(process.env.PORT || 8787);

createServer(async (req, res) => {
  const chunks = [];
  for await (const c of req) chunks.push(c);
  const body = chunks.length ? Buffer.concat(chunks) : undefined;
  const request = new Request(`http://127.0.0.1:${port}${req.url}`, {
    method: req.method,
    headers: req.headers,
    body: ['GET', 'HEAD', 'OPTIONS'].includes(req.method) ? undefined : body,
  });
  const response = await handle(request, env);
  res.writeHead(response.status, Object.fromEntries(response.headers));
  res.end(Buffer.from(await response.arrayBuffer()));
}).listen(port, '127.0.0.1', () => console.log(`Paisa Nextcloud proxy on http://127.0.0.1:${port}`));
