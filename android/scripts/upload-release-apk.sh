#!/bin/zsh
set -euo pipefail
# Builds the signed Android release APK on this machine and attaches it to an
# existing GitHub release. Nothing runs in CI, so the keystore and its passwords
# never leave this checkout.
#
#   android/scripts/upload-release-apk.sh v1.4.1
#   android/scripts/upload-release-apk.sh v1.4.1 --dry-run   # everything but the upload
#
# Requires:
#   - android/keystore.properties and the .jks it points at (both gitignored)
#   - the tag checked out on HEAD with a clean working tree
#   - the GitHub release already created with its notes
#   - gh authenticated against this repo (repo scope)

die() { print -u2 -- "$1"; exit 1 }

DRY_RUN=0
TAG=""
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    -*) die "Unknown option: $arg" ;;
    *)
      [[ -z "$TAG" ]] || die "Give exactly one tag, e.g. v1.4.1."
      TAG="$arg"
      ;;
  esac
done
[[ -n "$TAG" ]] || die "Usage: upload-release-apk.sh <tag> [--dry-run]   e.g. v1.4.1"
[[ "$TAG" =~ ^v[0-9]+\.[0-9]+ ]] || die "Tag '$TAG' does not look like v1.2.3."
VERSION="${TAG#v}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ANDROID="$ROOT/android"
APK_OUT="$ANDROID/app/build/outputs/apk/release"
STAGE="$APK_OUT/upload"
ASSET="Z-Sync-$VERSION.apk"
# The Z-Sync upload key, from the certificate in android/zencompanion-release.jks.
# The uploaded APK must carry it or it is not a legitimate Z-Sync build.
EXPECTED_SIGNER="a939fff118b414c2defe2840655cce5b8c125cfc162b272972b84596efbb6d1d"

REPO="$(git -C "$ROOT" remote get-url origin | sed -E 's#.*github\.com[:/]##; s#\.git$##')"
[[ -n "$REPO" ]] || die "Cannot work out the GitHub repo from origin."

# --- preflight -------------------------------------------------------------

[[ -f "$ANDROID/keystore.properties" ]] || die \
  "Missing android/keystore.properties. Copy keystore.properties.example and point storeFile at the release keystore."

BUILD_VERSION="$(sed -n 's/^ *versionName *= *"\(.*\)"/\1/p' "$ANDROID/app/build.gradle.kts" | head -1)"
[[ "$BUILD_VERSION" == "$VERSION" ]] || die \
  "android/app/build.gradle.kts has versionName $BUILD_VERSION but the tag is $TAG. Bump the version first."

git -C "$ROOT" rev-parse -q --verify "refs/tags/$TAG" >/dev/null || die "Tag $TAG does not exist locally."
TAG_SHA="$(git -C "$ROOT" rev-parse "$TAG^{commit}")"
HEAD_SHA="$(git -C "$ROOT" rev-parse HEAD)"
[[ "$TAG_SHA" == "$HEAD_SHA" ]] || die \
  "$TAG is not checked out (tag is $TAG_SHA, HEAD is $HEAD_SHA)."

REMOTE_TAGS="$(git -C "$ROOT" ls-remote --tags origin "refs/tags/$TAG" "refs/tags/$TAG^{}" 2>/dev/null)" ||
  die "Cannot reach origin to check that the tag is pushed."
REMOTE_SHA="$(print -r -- "$REMOTE_TAGS" | tail -1 | cut -f1)"
[[ -n "$REMOTE_SHA" ]] || die "Tag $TAG is not on origin yet. Push it first: git push origin $TAG"
[[ "$REMOTE_SHA" == "$TAG_SHA" ]] || die \
  "origin/$TAG is $REMOTE_SHA but the local tag is $TAG_SHA. Re-fetch or re-tag before uploading."

# Last, because it is the one check that also fails on purpose while developing.
[[ -z "$(git -C "$ROOT" status --porcelain)" ]] || die \
  "Working tree is dirty, so a build here would not match $TAG. Commit or stash first."

command -v gh >/dev/null || die "gh (GitHub CLI) is not on PATH."
gh auth status >/dev/null 2>&1 || die "gh is not authenticated. Run: gh auth login"
gh release view "$TAG" -R "$REPO" >/dev/null 2>&1 || die \
  "No GitHub release for $TAG. Create it with its notes first, then run this again."

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK" && -f "$ANDROID/local.properties" ]]; then
  SDK="$(sed -n 's/^sdk\.dir=//p' "$ANDROID/local.properties")"
fi
[[ -n "$SDK" && -d "$SDK" ]] || die \
  "Android SDK not found. Set ANDROID_HOME or sdk.dir in android/local.properties."
BUILD_TOOLS="$(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort -V | tail -1)"
[[ -n "$BUILD_TOOLS" ]] || die "No build-tools installed under $SDK/build-tools."

# --- build -----------------------------------------------------------------

# Gradle resolves the SDK the same way, but be explicit: the tag may be checked
# out in a worktree that has no local.properties.
export ANDROID_HOME="$SDK"

print -- "Building $VERSION from $TAG …"
(cd "$ANDROID" && ./gradlew :app:assembleRelease)

APK="$APK_OUT/app-release.apk"
[[ -f "$APK" ]] || APK="$(find "$APK_OUT" -maxdepth 1 -name '*.apk' | head -1)"
[[ -n "$APK" && -f "$APK" ]] || die "No APK in $APK_OUT after the build."

# The build directory is reused between runs, so confirm the artifact really is
# the version we are publishing instead of trusting a leftover file.
APK_VERSION="$("$BUILD_TOOLS/aapt2" dump badging "$APK" |
  sed -n "s/^package:.*versionName='\([^']*\)'.*/\1/p" | head -1)"
[[ "$APK_VERSION" == "$VERSION" ]] || die \
  "APK says versionName '$APK_VERSION', expected $VERSION. Stale build output?"

# --- verify ----------------------------------------------------------------

if ! CERTS="$("$BUILD_TOOLS/apksigner" verify --print-certs "$APK" 2>&1)"; then
  print -u2 -- "$CERTS"
  die "$APK is not a valid signed APK."
fi
print -- "$CERTS" | grep -qi "$EXPECTED_SIGNER" || die \
  "$APK is not signed with the Z-Sync release key. Refusing to upload it."

rm -rf "$STAGE"
mkdir -p "$STAGE"
cp "$APK" "$STAGE/$ASSET"

print -- ""
print -- "Signature : $EXPECTED_SIGNER"
print -- "SHA-256   : $(shasum -a 256 "$APK" | cut -d' ' -f1)"
print -- ""

# --- upload ----------------------------------------------------------------

if (( DRY_RUN )); then
  print -- "Dry run, nothing uploaded. The real command would be:"
  print -- "  gh release upload $TAG -R $REPO $STAGE/$ASSET --clobber"
  exit 0
fi

print -- "Uploading to $TAG …"
gh release upload "$TAG" -R "$REPO" "$STAGE/$ASSET" --clobber

print -- ""
print -- "Assets on $TAG:"
gh release view "$TAG" -R "$REPO" --json assets --jq '.assets[] | "  \(.name)  \(.size) bytes"'
