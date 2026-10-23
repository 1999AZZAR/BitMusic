#!/bin/sh
# Stamps a jpackage-built BitChord .deb with the fork's version line and injects
# it into the bundled launcher's config, since jpackage normalizes package
# versions to MAJOR.MINOR.BUILD and Compose validates them per format.
# Usage: linux-stamp-deb.sh <input.deb> <output.deb> <version>
set -eu

in="$1"
out="$2"
version="$3"
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

dpkg-deb -R "$in" "$root"
sed -i "s/^Version: .*/Version: $version/" "$root/DEBIAN/control"
# [Application] holds jvm-args in jpackage's cfg grammar.
sed -i "/^\[Application\]/a app.options=-Dbitchord.version=$version" \
    "$root/opt/bitchord/lib/app/BitChord.cfg"
dpkg-deb -b "$root" "$out"
rm -rf "$root"
