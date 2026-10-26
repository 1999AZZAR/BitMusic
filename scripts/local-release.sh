#!/bin/sh
# BitMusic local release loop: build the deb, stamp it with the branch-aware version, and install.
#
# Version scheme:
#   master  -> 1.2.0
#   staging -> 1.2.0-beta-0 (increments -1, -2... if already installed)
#   feature -> 1.2.0-beta-<feature>-0
set -eu
cd "$(dirname "$0")/.."

branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "staging")
version_file="$(dirname "$0")/../version.txt"
if [ -f "$version_file" ]; then
    base=$(tr -d '[:space:]' < "$version_file")
else
    base="1.2.0"
fi

case "$branch" in
    master|main)
        prefix="$base"
        default_version="$base"
        ;;
    staging)
        prefix="$base-beta"
        default_version="$base-beta-0"
        ;;
    feature/*)
        feat_name=$(echo "${branch#feature/}" | tr '/' '-')
        prefix="$base-beta-${feat_name}"
        default_version="$base-beta-${feat_name}-0"
        ;;
    fix/*)
        fix_name=$(echo "${branch#fix/}" | tr '/' '-')
        prefix="$base-beta-${fix_name}"
        default_version="$base-beta-${fix_name}-0"
        ;;
    *)
        clean_branch=$(echo "$branch" | tr '/' '-')
        prefix="$base-beta-${clean_branch}"
        default_version="$base-beta-${clean_branch}-0"
        ;;
esac

installed=$(dpkg-query -W -f '${Version}' bitmusic 2>/dev/null || true)

if [ "$branch" = "master" ] || [ "$branch" = "main" ]; then
    next="$base"
else
    case "$installed" in
        "$prefix"-*)
            rev=${installed##*-}
            if echo "$rev" | grep -qE '^[0-9]+$'; then
                next="$prefix-$((rev + 1))"
            else
                next="$default_version"
            fi
            ;;
        "$default_version")
            next="$prefix-1"
            ;;
        *)
            next="$default_version"
            ;;
    esac
fi

echo "Building BitMusic $next for branch '$branch' (currently installed: ${installed:-nothing})"

./gradlew :desktopApp:packageDeb -Pbitchord.version="$next"

in=$(ls -1 desktopApp/build/compose/binaries/main/deb/bitmusic_*_amd64.deb | tail -n 1)
out="/tmp/bitmusic-${next}.deb"
sh desktopApp/packaging/linux-stamp-deb.sh "$in" "$out" "$next"
sudo dpkg --force-downgrade -i "$out"
rm -f "$out"
dpkg-query -W -f 'installed: ${Package} ${Version}\n' bitmusic
