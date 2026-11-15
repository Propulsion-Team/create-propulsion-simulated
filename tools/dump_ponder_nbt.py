#!/usr/bin/env python3
"""
Dump a Create Ponder schematic .nbt into readable JSON.

Usage:
  python tools/dump_ponder_nbt.py \
    --input src/main/resources/assets/createpropulsion/ponder/solid_burner.nbt \
    --output solid_burner_blocks.json
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any

import nbtlib


def _get_key(d: dict[str, Any], *names: str) -> Any:
    for name in names:
        if name in d:
            return d[name]
    return None


def _to_xyz(value: Any) -> list[int]:
    return [int(value[0]), int(value[1]), int(value[2])]


def _decode_palette_entry(entry: Any) -> tuple[str, dict[str, str]]:
    if isinstance(entry, dict):
        name = str(_get_key(entry, "Name", "name", "NameString", "nameString") or "minecraft:air")
        props = _get_key(entry, "Properties", "properties") or {}
        return name, {str(k): str(v) for k, v in dict(props).items()}

    if isinstance(entry, str):
        return entry, {}

    return "minecraft:air", {}


def dump_schematic(path: Path, include_air: bool) -> dict[str, Any]:
    nbt_file = nbtlib.load(path)
    root = dict(nbt_file)

    size_tag = _get_key(root, "size", "Size")
    if size_tag is None:
        raise ValueError("Could not find schematic size (expected key 'size' or 'Size').")
    size = _to_xyz(size_tag)

    palette_tag = _get_key(root, "palette", "Palette")
    if palette_tag is None:
        raise ValueError("Could not find palette (expected key 'palette' or 'Palette').")
    palette = list(palette_tag)

    blocks_tag = _get_key(root, "blocks", "Blocks")
    if blocks_tag is None:
        raise ValueError("Could not find block list (expected key 'blocks' or 'Blocks').")
    blocks_raw = list(blocks_tag)

    center = [(size[0] - 1) / 2.0, (size[1] - 1) / 2.0, (size[2] - 1) / 2.0]

    blocks_out: list[dict[str, Any]] = []
    block_counter: Counter[str] = Counter()

    for i, block in enumerate(blocks_raw):
        block_dict = dict(block)
        pos_tag = _get_key(block_dict, "pos", "Pos")
        state_index = int(_get_key(block_dict, "state", "State", "palette", "Palette", "state_index", "StateIndex"))

        if pos_tag is None:
            continue
        if state_index < 0 or state_index >= len(palette):
            continue

        x, y, z = _to_xyz(pos_tag)
        name, properties = _decode_palette_entry(palette[state_index])

        if not include_air and name == "minecraft:air":
            continue

        block_counter[name] += 1
        blocks_out.append(
            {
                "index": i,
                "name": name,
                "properties": properties,
                "state_index": state_index,
                "pos": {"x": x, "y": y, "z": z},
                "offset_from_origin": {"x": x, "y": y, "z": z},
                "offset_from_center": {
                    "x": round(x - center[0], 3),
                    "y": round(y - center[1], 3),
                    "z": round(z - center[2], 3),
                },
            }
        )

    blocks_out.sort(key=lambda b: (b["pos"]["y"], b["pos"]["z"], b["pos"]["x"], b["name"]))

    return {
        "file": str(path.as_posix()),
        "size": {"x": size[0], "y": size[1], "z": size[2]},
        "origin_note": "(0,0,0) is minX/minY/minZ corner in schematic-local coordinates",
        "total_blocks_in_nbt": len(blocks_raw),
        "total_blocks_emitted": len(blocks_out),
        "palette_size": len(palette),
        "counts_by_block": dict(sorted(block_counter.items(), key=lambda kv: (-kv[1], kv[0]))),
        "blocks": blocks_out,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Dump Ponder schematic NBT blocks + offsets to JSON.")
    parser.add_argument(
        "--input",
        default="src/main/resources/assets/createpropulsion/ponder/solid_burner.nbt",
        help="Path to schematic .nbt file",
    )
    parser.add_argument(
        "--output",
        default="",
        help="Output JSON file path (defaults to stdout)",
    )
    parser.add_argument(
        "--include-air",
        action="store_true",
        help="Include minecraft:air entries in output",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        raise SystemExit(f"Input file not found: {input_path}")

    data = dump_schematic(input_path, include_air=args.include_air)
    serialized = json.dumps(data, indent=2)

    if args.output:
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(serialized, encoding="utf-8")
        print(f"Wrote {output_path} ({data['total_blocks_emitted']} blocks emitted)")
    else:
        print(serialized)


if __name__ == "__main__":
    main()
