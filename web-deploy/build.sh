#!/usr/bin/env bash
# Builds what billi.taf.sh serves into web-deploy/public:
#   /        the landing page, from web-deploy/site
#   /app/    the web app, from :app:wasmJsBrowserDistribution
# Then deploy with: npx wrangler deploy -c web-deploy/wrangler.jsonc
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew :app:wasmJsBrowserDistribution -q

out=web-deploy/public
rm -rf "$out"
mkdir -p "$out/app"
cp web-deploy/site/index.html web-deploy/site/404.html web-deploy/site/styles.css \
  web-deploy/site/tokens.css web-deploy/site/base.css web-deploy/site/logo.svg \
  web-deploy/site/favicon.svg web-deploy/site/billi-curious.svg "$out/"
cp -R web-deploy/site/screens "$out/"
cp -R app/build/dist/wasmJs/productionExecutable/. "$out/app/"
# Source maps and licence notes stay out of the upload.
mv "$out/app/.assetsignore" "$out/.assetsignore"
echo "Built $out"
