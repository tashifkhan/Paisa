// The web app routes on the client (/settings/backup, /transactions/42), so the
// dev server answers every unknown path with index.html, like the live site.
if (config.devServer) {
  config.devServer.historyApiFallback = true;
}
