#!/usr/bin/env python3
import json
import sys
import tempfile
from pathlib import Path
from urllib.request import Request, urlopen
from PIL import Image

from comparison_rendering import save_aligned_comparison
from engine_support import configure_utf8_stdio, fail, finite_positive, require_opencv
from measurement_processing import process_image


DEFAULT_TICK_DISTANCE_CM = 1.0


def main():
    configure_utf8_stdio()
    request = read_request()
    require_opencv()

    tick_distance_cm = request.get("knownTickDistanceCm") or DEFAULT_TICK_DISTANCE_CM
    if not finite_positive(tick_distance_cm):
        fail("knownTickDistanceCm must be a positive finite number")

    with tempfile.TemporaryDirectory(prefix="idap-measure-") as temp_dir_name:
        temp_dir = Path(temp_dir_name)
        output_dir = temp_dir / "output"

        primary_path = download_image(request["primaryImageUrl"], temp_dir / "primary.img", "primary")
        primary = process_image(
            primary_path,
            request.get("primarySelection"),
            request.get("primaryCalibration"),
            tick_distance_cm,
        )

        comparison = None
        comparison_url = request.get("comparisonImageUrl")
        if comparison_url:
            comparison_path = download_image(comparison_url, temp_dir / "comparison.img", "comparison")
            comparison = process_image(
                comparison_path,
                request.get("comparisonSelection"),
                request.get("comparisonCalibration"),
                tick_distance_cm,
            )

        comparison_image_path = save_aligned_comparison(
            output_dir,
            request["outputImageKey"].replace("/", "-").replace("\\", "-"),
            primary,
            comparison,
        )
        put_image(request["outputImageUrl"], Path(comparison_image_path))

    result = primary["result"]
    result["comparisonImagePath"] = request["outputImageKey"]
    if comparison is not None:
        result["comparisonResult"] = comparison["result"]
    print(json.dumps(result, ensure_ascii=False), flush=True)


def read_request():
    return json.loads(sys.stdin.buffer.read().decode("utf-8"))


def download_image(url, destination, label):
    try:
        with urlopen(url, timeout=30) as response:
            data = response.read()
            destination.write_bytes(data)
        with Image.open(destination) as image:
            print(
                f"[measurement-debug] {label} image bytes={len(data)} "
                f"format={image.format} size={image.width}x{image.height}",
                file=sys.stderr,
            )
        return destination
    except Exception as ex:
        fail(f"Could not read {label} image from MinIO: {ex}")


def put_image(url, path):
    request = Request(
        url,
        data=path.read_bytes(),
        method="PUT",
        headers={"Content-Type": "image/jpeg"},
    )
    try:
        with urlopen(request, timeout=30):
            return
    except Exception as ex:
        fail(f"Could not write measurement image to MinIO: {ex}")


if __name__ == "__main__":
    main()
