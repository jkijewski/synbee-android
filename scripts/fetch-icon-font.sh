#!/bin/bash
# Downloads the Material Symbols font (Apache-2.0) at a pinned commit and checks its hash. Run once before building.
set -euo pipefail
COMMIT=10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7
SHA256=f7837ba8573d9a7a74931618ced8307bc81f96d49af296bf6c8052c772ed5282
OUT="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/res/font/material_symbols.ttf"
URL="https://raw.githubusercontent.com/google/material-design-icons/$COMMIT/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf"
if [ -f "$OUT" ] && echo "$SHA256  $OUT" | sha256sum -c --quiet -; then exit 0; fi
# The folder holds only this downloaded file, which git ignores, so a fresh checkout doesn't have it.
mkdir -p "$(dirname "$OUT")"
curl -sfL -o "$OUT.tmp" "$URL"
echo "$SHA256  $OUT.tmp" | sha256sum -c --quiet -
mv "$OUT.tmp" "$OUT"
echo "Fetched $OUT"
