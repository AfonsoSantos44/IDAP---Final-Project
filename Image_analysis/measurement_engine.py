#!/usr/bin/env python3
import json
import sys
from pathlib import Path

from comparison_rendering import save_aligned_comparison
from engine_support import configure_utf8_stdio, fail, finite_positive, require_opencv
from measurement_processing import process_image


DEFAULT_TICK_DISTANCE_CM = 1.0


def main():
    configure_utf8_stdio()
    request = read_request()
    require_opencv()

    output_dir = Path(request["outputDirectory"])
    output_dir.mkdir(parents=True, exist_ok=True)

    tick_distance_cm = request.get("knownTickDistanceCm") or DEFAULT_TICK_DISTANCE_CM
    if not finite_positive(tick_distance_cm):
        fail("knownTickDistanceCm must be a positive finite number")

    primary = process_image(
        Path(request["primaryImagePath"]),
        request.get("primarySelection"),
        request.get("primaryCalibration"),
        tick_distance_cm,
    )

    comparison = None
    comparison_path = request.get("comparisonImagePath")
    if comparison_path:
        comparison = process_image(
            Path(comparison_path),
            request.get("comparisonSelection"),
            request.get("comparisonCalibration"),
            tick_distance_cm,
        )

    comparison_image_path = save_aligned_comparison(
        output_dir,
        request["outputBaseName"],
        primary,
        comparison,
    )

    result = primary["result"]
    result["comparisonImagePath"] = comparison_image_path
    print(json.dumps(result, ensure_ascii=False), flush=True)


def read_request():
    return json.loads(sys.stdin.buffer.read().decode("utf-8"))


if __name__ == "__main__":
    main()
