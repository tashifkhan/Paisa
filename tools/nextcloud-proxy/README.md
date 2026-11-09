# Nextcloud proxy for Paisa web

Nextcloud doesn't send CORS headers, so a browser page can't talk to it. This
small Cloudflare Worker sits in between: Paisa sends each request here with the
real address in `?target=`, and the worker forwards it and adds
the CORS headers. It stores nothing.

It only forwards to the one server in `NEXTCLOUD_URL`, so nobody can use it as an
open proxy. The Android app doesn't need it.

## Deploy your own

1. Set `NEXTCLOUD_URL` and `ALLOWED_ORIGIN` in `wrangler.toml`.
2. `npx wrangler deploy` from this folder.
3. In Paisa web: More → Backup → Nextcloud, and paste the worker's address
   (like `https://paisa-nextcloud-proxy.you.workers.dev`) into **CORS proxy**.

Your app password passes through your own worker, under your own Cloudflare
account. Use https for both Nextcloud and the worker.

## Run it locally

```sh
NEXTCLOUD_URL=http://127.0.0.1:8090 node tools/nextcloud-proxy/serve.mjs
```

Then use `http://127.0.0.1:8787` as the CORS proxy.

## Or skip the proxy

If you run the Nextcloud server, you can send the CORS headers from it instead,
for example in Apache:

```apache
Header always set Access-Control-Allow-Origin "https://paisa.example.com"
Header always set Access-Control-Allow-Headers "authorization, content-type, depth, ocs-apirequest"
Header always set Access-Control-Allow-Methods "GET, PUT, DELETE, POST, PROPFIND, MKCOL, OPTIONS"
```

Then leave **CORS proxy** empty in Paisa web, and it calls the server directly.
