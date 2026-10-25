"""Compile the approved generated material atlas into three native 32px game textures."""

import argparse
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent / "steel_hopper" / "source-atlas.png"
DEST = ROOT / "src/main/resources/assets/ic2/textures/block/transport"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    atlas = Image.open(SOURCE).convert("RGBA")
    assert atlas.width == atlas.height * 3, "Expected three edge-to-edge square tiles"
    DEST.mkdir(parents=True, exist_ok=True)
    for index, surface in enumerate(("outside", "top", "inside")):
        tile = atlas.crop((index * atlas.height, 0, (index + 1) * atlas.height, atlas.height))
        tile = tile.resize((32, 32), Image.Resampling.NEAREST)
        assert tile.getchannel("A").getextrema() == (255, 255)
        encoded = BytesIO()
        tile.save(encoded, format="PNG", optimize=True)
        path = DEST / f"steel_hopper_{surface}.png"
        if args.check:
            assert path.read_bytes() == encoded.getvalue(), f"Outdated texture: {path}"
        else:
            path.write_bytes(encoded.getvalue())
        print(f"{surface}: 32x32, opaque, {'checked' if args.check else 'written'}")
    item = Image.open(SOURCE.with_name("source-item.png")).convert("RGBA")
    item = item.resize((32, 32), Image.Resampling.NEAREST)
    item.putalpha(item.getchannel("A").point(lambda value: 255 if value >= 128 else 0))
    assert item.getchannel("A").getextrema() == (0, 255), "The inventory sprite must retain transparency"
    encoded = BytesIO()
    item.save(encoded, format="PNG", optimize=True)
    path = ROOT / "src/main/resources/assets/ic2/textures/item/steel_hopper.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    if args.check:
        assert path.read_bytes() == encoded.getvalue(), f"Outdated item sprite: {path}"
    else:
        path.write_bytes(encoded.getvalue())
    print(f"item: 32x32, binary alpha, {'checked' if args.check else 'written'}")


if __name__ == "__main__":
    main()
