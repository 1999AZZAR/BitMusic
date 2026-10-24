#!/bin/sh
# The fork's own release loop: build the deb, stamp it with the next frea number, install it.
#
# The number comes from what dpkg says is installed: $basefreaN becomes frea(N+1), anything
# older or absent starts at frea1. Run from anywhere; it cd's to the repository root.
set -eu
cd "$(dirname "$0")/.."

base=$(sed -n 's/.*?: "\([0-9][0-9.]*\)"/\1/p' desktopApp/build.gradle.kts | head -1)
if [ -z "$base" ]; then
    echo "could not read the app version from desktopApp/build.gradle.kts" >&2
    exit 1
fi

installed=$(dpkg-query -W -f '${Version}' bitmusic 2>/dev/null || true)
case "$installed" in
    "$base"frea*) n=${installed##*frea}; next="$base"frea$((n + 1)) ;;
    *) next="$base"frea1 ;;
esac
echo "building $next (installed: ${installed:-nothing})"

./gradlew :desktopApp:packageDeb

in="desktopApp/build/compose/binaries/main/deb/bitmusic_${base}_amd64.deb"
out="/tmp/bitmusic-${next}.deb"
sh desktopApp/packaging/linux-stamp-deb.sh "$in" "$out" "$next"
sudo dpkg -i "$out"
dpkg-query -W -f 'installed: ${Package} ${Version}\n' bitmusic
