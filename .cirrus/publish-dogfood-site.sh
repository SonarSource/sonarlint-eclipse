#!/usr/bin/env bash

set -euo pipefail

: "${CIRRUS_BUILD_ID?}" "${PROJECT_VERSION?}" "${ARTIFACTORY_ACCESS_TOKEN?}" "${ARTIFACTORY_URL?}"
: "${S3_BUCKET:=downloads-cdn-eu-central-1-prod}"
: "${BINARIES_URL:=https://binaries.sonarsource.com}"
ROOT_BUCKET_KEY="SonarLint-for-Eclipse/dogfood"
VERSION_BUCKET_KEY="$ROOT_BUCKET_KEY/$PROJECT_VERSION"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
pushd "${SCRIPT_DIR}/" >/dev/null
dogfood_site_dir=$(mktemp -d -p "$PWD" -t tmp.XXXXXXXX)
trap 'rm -rf "$dogfood_site_dir" "$dogfood_site_dir".zip' EXIT

curl --fail --silent --show-error --location \
  "https://api.cirrus-ci.com/v1/artifact/build/$CIRRUS_BUILD_ID/build/site/org.sonarlint.eclipse.site/target/org.sonarlint.eclipse.site-$PROJECT_VERSION.zip" \
  -o "$dogfood_site_dir".zip ||
  {
    # local usage or any use case with no Cirrus artifact
    type jfrog 2>/dev/null || jfrog() { jf "$@"; }
    echo "Failed to download org.sonarlint.eclipse.site-$PROJECT_VERSION.zip from Cirrus CI build $CIRRUS_BUILD_ID; fallback on Artifactory"
    jfrog config use repox 2>/dev/null || jfrog config add repox --artifactory-url "$ARTIFACTORY_URL" --access-token "$ARTIFACTORY_ACCESS_TOKEN"
    jfrog rt curl "sonarsource-public-builds/org/sonarsource/sonarlint/eclipse/org.sonarlint.eclipse.site/$PROJECT_VERSION/org.sonarlint.eclipse.site-$PROJECT_VERSION.zip" -o "$dogfood_site_dir".zip
  }
mkdir -p "$dogfood_site_dir/$PROJECT_VERSION"
unzip -q "$dogfood_site_dir".zip -d "$dogfood_site_dir/$PROJECT_VERSION"

NOW=$(date -u +"%s%3N")
export NOW BINARIES_URL VERSION_BUCKET_KEY
# shellcheck disable=SC2016
envsubst '$NOW,$BINARIES_URL,$VERSION_BUCKET_KEY' <"site-resources/compositeContent.xml" >"$dogfood_site_dir/compositeContent.xml"
# shellcheck disable=SC2016
envsubst '$NOW,$BINARIES_URL,$VERSION_BUCKET_KEY' <"site-resources/compositeArtifacts.xml" >"$dogfood_site_dir/compositeArtifacts.xml"

echo "Upload from $dogfood_site_dir to s3://$S3_BUCKET/$ROOT_BUCKET_KEY/..."
aws s3 sync "$@" --delete "$dogfood_site_dir" "s3://$S3_BUCKET/$ROOT_BUCKET_KEY/"
echo "Upload done"

DISTRIBUTION_ID=$(aws cloudfront list-distributions --query "DistributionList.Items[*].{id:Id,origin:Origins.Items[0].DomainName}[?starts_with(origin,'$S3_BUCKET')].id" --output text)
aws cloudfront create-invalidation --distribution-id "$DISTRIBUTION_ID" --paths "/$ROOT_BUCKET_KEY/*"
echo "Dogfood site published to $BINARIES_URL/?prefix=$ROOT_BUCKET_KEY/"
popd >/dev/null
