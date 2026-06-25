from PIL import Image, ImageOps
import numpy as np

from comparison_rendering import annotate_image
from engine_support import cv2, fail
from image_inputs import resize_for_analysis, scale_calibration, scale_selection, to_original
from measurement_geometry import measure_selection, reference_segment
from ruler_calibration import calibrate_ruler
from ruler_detection import detect_and_rectify_ruler, detect_tick_rows, estimate_tick_spacing


def process_image(path, selection_input, calibration_input, tick_distance_cm):
    if not path.is_file():
        fail(f"Image file does not exist: {path}")
    if selection_input is None:
        fail("An analyst damage selection is required for every measured image")

    original = ImageOps.exif_transpose(Image.open(path)).convert("RGB")
    working, working_factor = resize_for_analysis(original)
    rgb = np.asarray(working, dtype=np.uint8)
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)

    selection = scale_selection(selection_input, working_factor, working.size)
    calibration_input = scale_calibration(calibration_input, working_factor, working.size)

    ruler = detect_and_rectify_ruler(bgr, calibration_input.get("rulerRegion"))
    tick_rows = detect_tick_rows(ruler["rectified"])
    pixels_per_tick = estimate_tick_spacing(tick_rows)
    calibration = calibrate_ruler(
        ruler,
        calibration_input.get("referencePoints", []),
        pixels_per_tick,
        tick_distance_cm,
    )

    damage = measure_selection(selection, ruler, calibration)
    confidence = round(
        min(0.99, (ruler["confidence"] * 0.35) + (calibration["confidence"] * 0.65)),
        3,
    )

    ref_points = reference_segment(
        ruler,
        calibration,
        damage["center_height_cm"],
        tick_distance_cm,
    )

    # Render annotations on the full-resolution original photo, not the
    # downscaled analysis copy. Drawing on `working` and then resizing again
    # later (during comparison alignment) compounded into blurry overlays and
    # a delivered image well below the original photo's resolution.
    scale_to_original = 1.0 / working_factor
    annotated = annotate_image(
        original.copy(),
        {"polygon": scale_points(ruler["polygon"], scale_to_original)},
        scale_rect(selection, scale_to_original),
        scale_damage(damage, scale_to_original),
        confidence,
        calibration["method"],
    )

    # cm-per-pixel must match the space the coordinates below are reported
    # in (original-image pixels), not the downscaled working-image space
    # calibration was actually fit in.
    cm_per_pixel_original = calibration["cm_per_pixel"] * working_factor

    result = {
        "refObjLengthCm": tick_distance_cm,
        "refObjX1": to_original(ref_points[0][0], scale_to_original),
        "refObjY1": to_original(ref_points[0][1], scale_to_original),
        "refObjX2": to_original(ref_points[1][0], scale_to_original),
        "refObjY2": to_original(ref_points[1][1], scale_to_original),
        "damageAreaX1": to_original(selection["x1"], scale_to_original),
        "damageAreaY1": to_original(selection["y1"], scale_to_original),
        "damageAreaX2": to_original(selection["x2"], scale_to_original),
        "damageAreaY2": to_original(selection["y2"], scale_to_original),
        "calculatedHeightCm": round(damage["center_height_cm"], 2),
        "damageMinHeightCm": round(damage["min_height_cm"], 2),
        "damageMaxHeightCm": round(damage["max_height_cm"], 2),
        "scaleCmPerPixel": round(cm_per_pixel_original, 6),
        "confidence": confidence,
        "calibrationMethod": calibration["method"],
    }

    return {
        "result": result,
        "annotated_image": annotated,
        "scale_cm_per_pixel": cm_per_pixel_original,
        "scale_to_original": scale_to_original,
        "anchor_y": to_original(damage["anchor_point"][1], scale_to_original),
        "anchor_value_cm": damage["center_height_cm"],
        "ruler": ruler,
        "calibration": calibration,
    }


def scale_points(points, scale):
    return [(to_original(x, scale), to_original(y, scale)) for x, y in points]


def scale_rect(rect, scale):
    return {key: to_original(value, scale) for key, value in rect.items()}


def scale_damage(damage, scale):
    scaled = dict(damage)
    scaled["anchor_point"] = (
        to_original(damage["anchor_point"][0], scale),
        to_original(damage["anchor_point"][1], scale),
    )
    return scaled