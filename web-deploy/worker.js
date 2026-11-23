// billi.taf.sh: the landing page at /, the web app under /app/.
//
// Static files answer first, without running this script. It only sees
// requests that match no file, and handles three of them:
// - /app with no slash goes to /app/
// - any other page under /app/ (a client-side route like /app/settings/backup)
//   gets the app's index.html, and the app opens that screen itself
// - everything else gets the 404 page (asked for as /404: the asset server
//   redirects /404.html there)
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === '/app') {
      return Response.redirect(`${url.origin}/app/${url.search}`, 301);
    }
    // A missing script or image under /app/ stays a 404; only page routes,
    // which have no file extension, get the app.
    const lastSegment = url.pathname.split('/').pop();
    if (url.pathname.startsWith('/app/') && !lastSegment.includes('.')) {
      const shell = await env.ASSETS.fetch(new Request(new URL('/app/', url), request));
      return new Response(shell.body, { status: 200, headers: shell.headers });
    }
    const notFound = await env.ASSETS.fetch(new Request(new URL('/404', url), request));
    return new Response(notFound.body, { status: 404, headers: notFound.headers });
  },
};
