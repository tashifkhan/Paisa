// A CORS proxy so Paisa in the browser can reach your Nextcloud.
//
// Nextcloud sends no CORS headers, so a web page can't call it directly. Paisa
// sends each request here with the real address in `?target=`; this forwards
// it and adds the CORS headers. It stores nothing and logs nothing.
//
// Settings (wrangler.toml [vars] or the Cloudflare dashboard):
//   NEXTCLOUD_URL   your server, e.g. https://cloud.example.com. Requests to
//                   anywhere else are refused, so this can't be used as an
//                   open proxy.
//   ALLOWED_ORIGIN  where Paisa web runs, e.g. https://paisa.example.com.
//                   "*" allows any site, which is fine for testing only.

const METHODS = ['GET', 'PUT', 'DELETE', 'POST', 'PROPFIND', 'MKCOL'];
const FORWARD = ['authorization', 'content-type', 'depth', 'ocs-apirequest', 'user-agent'];

export async function handle(request, env) {
  const origin = env.ALLOWED_ORIGIN || '*';
  const cors = {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': ['OPTIONS', ...METHODS].join(', '),
    'Access-Control-Allow-Headers': FORWARD.join(', '),
    'Access-Control-Max-Age': '86400',
    Vary: 'Origin',
  };
  if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors });

  const refuse = (status, message) =>
    new Response(message, { status, headers: { ...cors, 'Content-Type': 'text/plain' } });

  if (!env.NEXTCLOUD_URL) return refuse(500, 'Set NEXTCLOUD_URL on the proxy');
  if (!METHODS.includes(request.method)) return refuse(405, 'Method not allowed');

  // The target is in the URL, not a header, so browsers never serve one
  // file's cached reply for another.
  const target = new URL(request.url).searchParams.get('target');
  let url;
  try {
    url = new URL(target);
  } catch {
    return refuse(400, 'Missing or invalid ?target=');
  }
  const base = new URL(env.NEXTCLOUD_URL);
  const basePath = base.pathname.replace(/\/$/, '');
  if (url.origin !== base.origin || !(url.pathname === basePath || url.pathname.startsWith(basePath + '/'))) {
    return refuse(403, 'This proxy only forwards to its own Nextcloud');
  }

  const headers = new Headers();
  for (const name of FORWARD) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }
  const hasBody = !['GET', 'DELETE', 'MKCOL'].includes(request.method);
  const upstream = await fetch(url.toString(), {
    method: request.method,
    headers,
    body: hasBody ? await request.arrayBuffer() : undefined,
    // The sign-in poll must never follow a redirect with its token.
    redirect: 'manual',
  });

  // No content-length: fetch hands back a decompressed body, so the server's
  // length (of the compressed bytes) would cut it short.
  const out = new Headers(cors);
  // Sync files must always be fresh.
  out.set('Cache-Control', 'no-store');
  for (const name of ['content-type', 'last-modified', 'etag']) {
    const value = upstream.headers.get(name);
    if (value) out.set(name, value);
  }
  return new Response(upstream.body, { status: upstream.status, headers: out });
}

export default { fetch: handle };
