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
    "$root/opt/bitmusic/lib/app/BitMusic.cfg"

# Package standard desktop icon theme files into /usr/share/icons and /usr/share/pixmaps
mkdir -p "$root/usr/share/pixmaps"
mkdir -p "$root/usr/share/icons/hicolor/scalable/apps"
mkdir -p "$root/usr/share/icons/hicolor/512x512/apps"
mkdir -p "$root/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$root/usr/share/icons/hicolor/128x128/apps"
mkdir -p "$root/usr/share/icons/hicolor/48x48/apps"

if [ -f /usr/share/icons/hicolor/scalable/apps/bitmusic.svg ]; then
    cp /usr/share/icons/hicolor/scalable/apps/bitmusic.svg "$root/usr/share/icons/hicolor/scalable/apps/bitmusic.svg"
    cp /usr/share/icons/hicolor/scalable/apps/bitmusic.svg "$root/usr/share/icons/hicolor/scalable/apps/BitMusic.svg"
fi
for s in 48 128 256 512; do
    if [ -f "/usr/share/icons/hicolor/${s}x${s}/apps/bitmusic.png" ]; then
        cp "/usr/share/icons/hicolor/${s}x${s}/apps/bitmusic.png" "$root/usr/share/icons/hicolor/${s}x${s}/apps/bitmusic.png"
        cp "/usr/share/icons/hicolor/${s}x${s}/apps/bitmusic.png" "$root/usr/share/icons/hicolor/${s}x${s}/apps/BitMusic.png"
    fi
done
if [ -f /usr/share/pixmaps/bitmusic.png ]; then
    cp /usr/share/pixmaps/bitmusic.png "$root/usr/share/pixmaps/bitmusic.png"
    cp /usr/share/pixmaps/bitmusic.png "$root/usr/share/pixmaps/BitMusic.png"
fi

# jpackage ships Categories=Audio; a menu needs the whole multimedia family.
for desktop in "$root"/opt/bitmusic/lib/*.desktop; do
    sed -i "s/^Categories=.*/Categories=AudioVideo;Audio;Player;Multimedia;/" "$desktop"
    sed -i "s|^Icon=.*|Icon=bitmusic|" "$desktop"
    if ! grep -q "StartupWMClass" "$desktop"; then
        echo "StartupWMClass=BitMusic" >> "$desktop"
    fi
done
dpkg-deb -b "$root" "$out"
rm -rf "$root"
rm -rf "$root"
