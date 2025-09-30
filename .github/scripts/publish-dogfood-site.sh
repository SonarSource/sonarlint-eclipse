#!/usr/bin/env bash

set -euo pipefail

: "${PROJECT_VERSION?}" "${ARTIFACTORY_ACCESS_TOKEN?}" "${ARTIFACTORY_URL?}"
: "${S3_BUCKET:=downloads-cdn-eu-central-1-prod}"
: "${BINARIES_URL:=https://binaries.sonarsource.com}"
ROOT_BUCKET_KEY="SonarLint-for-Eclipse/dogfood"
VERSION_BUCKET_KEY="$ROOT_BUCKET_KEY/$PROJECT_VERSION"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
pushd "${SCRIPT_DIR}/" >/dev/null
dogfood_site_dir=$(mktemp -d -p "$PWD" -t tmp.XXXXXXXX)
trap 'rm -rf "$dogfood_site_dir" "$dogfood_site_dir".zip' EXIT

# Download site ZIP - from workspace artifact if available
if [ -f "$GITHUB_WORKSPACE/site-artifact/org.sonarlint.eclipse.site-$PROJECT_VERSION.zip" ]; then
  echo "Using site artifact from GitHub Actions workspace"
  cp "$GITHUB_WORKSPACE/site-artifact/org.sonarlint.eclipse.site-$PROJECT_VERSION.zip" "$dogfood_site_dir.zip"
else
  echo "Downloading from Artifactory"
  command -v jfrog >/dev/null || jfrog() { jf "$@"; }
  jfrog config use repox 2>/dev/null || jfrog config add repox --artifactory-url "$ARTIFACTORY_URL" --access-token "$ARTIFACTORY_ACCESS_TOKEN"
  jfrog rt curl "sonarsource-public-builds/org/sonarsource/sonarlint/eclipse/org.sonarlint.eclipse.site/$PROJECT_VERSION/org.sonarlint.eclipse.site-$PROJECT_VERSION.zip" -o "$dogfood_site_dir.zip"
fi

mkdir -p "$dogfood_site_dir/$PROJECT_VERSION"
unzip -q "$dogfood_site_dir.zip" -d "$dogfood_site_dir/$PROJECT_VERSION"

NOW=$(date -u +"%s%3N")
export NOW BINARIES_URL VERSION_BUCKET_KEY
# shellcheck disable=SC2016
envsubst '$NOW,$BINARIES_URL,$VERSION_BUCKET_KEY' <"site-resources/compositeContent.xml" >"$dogfood_site_dir/compositeContent.xml"
# shellcheck disable=SC2016
envsubst '$NOW,$BINARIES_URL,$VERSION_BUCKET_KEY' <"site-resources/compositeArtifacts.xml" >"$dogfood_site_dir/compositeArtifacts.xml"

echo "Upload from $dogfood_site_dir to s3://$S3_BUCKET/$ROOT_BUCKET_KEY/..."
aws s3 sync "$@" --delete "$dogfood_site_dir" "s3://$S3_BUCKET/$ROOT_BUCKET_KEY/"
echo "Upload done"

DISTRIBUTION_ID=$(aws cloudfront list-distributions --query "DistributionList.Items[*].{id:Id,origin:Origins.Items[].{DomainName:DomainName}[?starts_with(DomainName,'$S3_BUCKET')]}[?not_null(origin)].id" --output text)
echo "Create CloudFront invalidation for distribution $DISTRIBUTION_ID"
aws cloudfront create-invalidation --distribution-id "$DISTRIBUTION_ID" --paths "/$ROOT_BUCKET_KEY/*"
echo "Dogfood site published to $BINARIES_URL/?prefix=$ROOT_BUCKET_KEY/"
popd >/dev/null