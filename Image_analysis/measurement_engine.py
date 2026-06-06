#!/usr/bin/env python3
import json
import math
import os
import re
import sys
from itertools import combinations
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageOps

try:
    import cv2
except ImportError:
    cv2 = None


MAX_ANALYSIS_SIDE = 1800
DEFAULT_TICK_DISTANCE_CM = 1.0
RESAMPLE_FILTER = getattr(Image, "Resampling", Image).LANCZOS


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


def configure_utf8_stdio():
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")


def read_request():
    return json.loads(sys.stdin.buffer.read().decode("utf-8"))


def require_opencv():
    if cv2 is None:
        fail("OpenCV is required. Install Image_analysis/requirements.txt")


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
    annotated = annotate_image(
        working.copy(),
        ruler,
        selection,
        damage,
        confidence,
        calibration["method"],
    )

    scale_to_original = 1.0 / working_factor
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
        "scaleCmPerPixel": round(calibration["cm_per_pixel"], 6),
        "confidence": confidence,
        "calibrationMethod": calibration["method"],
    }

    return {
        "result": result,
        "annotated_image": annotated,
        "scale_cm_per_pixel": calibration["cm_per_pixel"],
        "anchor_y": damage["anchor_point"][1],
        "anchor_value_cm": damage["center_height_cm"],
        "ruler": ruler,
        "calibration": calibration,
    }


def resize_for_analysis(image):
    width, height = image.size
    largest = max(width, height)
    if largest <= MAX_ANALYSIS_SIDE:
        return image, 1.0

    factor = MAX_ANALYSIS_SIDE / largest
    return image.resize((round(width * factor), round(height * factor)), RESAMPLE_FILTER), factor


def scale_selection(selection, factor, image_size):
    values = [selection.get(key) for key in ("x1", "y1", "x2", "y2")]
    if any(not finite_number(value) for value in values):
        fail("Damage selection coordinates must be finite numbers")

    width, height = image_size
    x1, x2 = sorted((float(values[0]) * factor, float(values[2]) * factor))
    y1, y2 = sorted((float(values[1]) * factor, float(values[3]) * factor))
    if x1 < 0 or y1 < 0 or x2 >= width or y2 >= height:
        fail("Damage selection lies outside the image")

    return {"x1": x1, "y1": y1, "x2": x2, "y2": y2}


def scale_calibration(calibration, factor, image_size):
    if calibration is None:
        return {"referencePoints": [], "rulerRegion": None}

    points = []
    for point in calibration.get("referencePoints", []):
        if any(not finite_number(point.get(key)) for key in ("x", "y", "valueCm")):
            fail("Ruler reference points must contain finite x, y and valueCm values")
        points.append(
            {
                "x": float(point["x"]) * factor,
                "y": float(point["y"]) * factor,
                "valueCm": float(point["valueCm"]),
            }
        )

    region = calibration.get("rulerRegion")
    scaled_region = scale_selection(region, factor, image_size) if region else None
    return {"referencePoints": points, "rulerRegion": scaled_region}


def detect_and_rectify_ruler(bgr, ruler_region):
    height, width = bgr.shape[:2]
    offset_x = 0
    offset_y = 0
    search = bgr
    if ruler_region:
        x1 = max(0, int(ruler_region["x1"]))
        y1 = max(0, int(ruler_region["y1"]))
        x2 = min(width, int(math.ceil(ruler_region["x2"])) + 1)
        y2 = min(height, int(math.ceil(ruler_region["y2"])) + 1)
        search = bgr[y1:y2, x1:x2]
        offset_x, offset_y = x1, y1

    polygon, confidence = find_ruler_polygon(search)
    if polygon is None:
        fail("Could not detect an elongated ruler. Provide rulerRegion to constrain detection")

    polygon[:, 0] += offset_x
    polygon[:, 1] += offset_y
    ordered = order_quad(polygon.astype(np.float32))
    transform, rectified, rect_width, rect_height = rectify_quad(bgr, ordered)

    return {
        "polygon": ordered,
        "transform": transform,
        "inverse_transform": np.linalg.inv(transform),
        "rectified": rectified,
        "width": rect_width,
        "height": rect_height,
        "confidence": confidence,
    }


def find_ruler_polygon(bgr):
    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    yellow = cv2.inRange(hsv, np.array([15, 65, 65]), np.array([45, 255, 255]))
    saturated = cv2.inRange(hsv, np.array([0, 75, 60]), np.array([179, 255, 255]))

    candidates = []
    for priority, mask in ((3.0, yellow), (0.15, saturated)):
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (7, 21))
        cleaned = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
        cleaned = cv2.morphologyEx(cleaned, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
        candidates.extend(projection_ruler_candidates(cleaned, priority))
        contours, _ = cv2.findContours(cleaned, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        candidates.extend(score_ruler_contours(contours, bgr.shape[:2], priority))

    if not candidates:
        return None, 0.0

    rescored = []
    for score, box, confidence in candidates:
        try:
            _, rectified, _, _ = rectify_quad(bgr, order_quad(box.astype(np.float32)))
            tick_quality = tick_pattern_quality(detect_tick_rows(rectified))
        except (ValueError, np.linalg.LinAlgError, cv2.error):
            tick_quality = 0.0
        rescored.append((score * (0.25 + tick_quality * 2.75), box, confidence, tick_quality))

    score, box, confidence, tick_quality = max(rescored, key=lambda item: item[0])
    confidence = min(0.99, confidence * (0.65 + tick_quality * 0.35))
    return box, confidence


def projection_ruler_candidates(mask, priority):
    height, width = mask.shape
    candidates = []
    column_groups = grouped_high_coverage((mask > 0).sum(axis=0), height * 0.18)
    row_groups = grouped_high_coverage((mask > 0).sum(axis=1), width * 0.18)

    for start, end in column_groups:
        if end - start < 4:
            continue
        active_rows = np.where((mask[:, start:end + 1] > 0).sum(axis=1) > max(2, (end - start) * 0.08))[0]
        if len(active_rows) < height * 0.25:
            continue
        top, bottom = int(active_rows.min()), int(active_rows.max())
        aspect = (bottom - top + 1) / max(1.0, end - start + 1)
        if aspect >= 4.0:
            box = quad_from_mask_region(mask, start, top, end, bottom)
            candidates.append((priority * aspect * 0.8, box, min(0.92, 0.62 + aspect / 40.0)))

    for start, end in row_groups:
        if end - start < 4:
            continue
        active_columns = np.where((mask[start:end + 1, :] > 0).sum(axis=0) > max(2, (end - start) * 0.08))[0]
        if len(active_columns) < width * 0.25:
            continue
        left, right = int(active_columns.min()), int(active_columns.max())
        aspect = (right - left + 1) / max(1.0, end - start + 1)
        if aspect >= 4.0:
            box = quad_from_mask_region(mask, left, start, right, end)
            candidates.append((priority * aspect * 0.8, box, min(0.92, 0.62 + aspect / 40.0)))

    return candidates


def quad_from_mask_region(mask, left, top, right, bottom):
    region = mask[top:bottom + 1, left:right + 1]
    points = cv2.findNonZero(region)
    if points is None or len(points) < 4:
        return np.array([[left, top], [right, top], [right, bottom], [left, bottom]], dtype=np.float32)

    points = points.reshape(-1, 2).astype(np.float32)
    points[:, 0] += left
    points[:, 1] += top
    return quad_from_points(points)


def quad_from_points(points):
    hull = cv2.convexHull(np.asarray(points, dtype=np.float32)).reshape(-1, 2)
    perimeter = cv2.arcLength(hull.reshape(-1, 1, 2), True)
    for epsilon_factor in np.linspace(0.01, 0.08, 15):
        approximation = cv2.approxPolyDP(
            hull.reshape(-1, 1, 2),
            epsilon_factor * perimeter,
            True,
        ).reshape(-1, 2)
        if len(approximation) == 4:
            return approximation.astype(np.float32)

    return cv2.boxPoints(cv2.minAreaRect(hull.astype(np.float32)))


def grouped_high_coverage(values, threshold):
    indexes = np.where(values >= threshold)[0]
    if len(indexes) == 0:
        return []
    groups = []
    start = previous = int(indexes[0])
    for index in indexes[1:]:
        index = int(index)
        if index > previous + 3:
            groups.append((start, previous))
            start = index
        previous = index
    groups.append((start, previous))
    return groups


def score_ruler_contours(contours, shape, priority):
    image_area = float(shape[0] * shape[1])
    scored = []
    for contour in contours:
        area = cv2.contourArea(contour)
        if area < image_area * 0.001 or area > image_area * 0.22:
            continue
        rect = cv2.minAreaRect(contour)
        side_a, side_b = rect[1]
        short_side = max(1.0, min(side_a, side_b))
        long_side = max(side_a, side_b)
        aspect = long_side / short_side
        if aspect < 4.0:
            continue
        fill = min(1.0, area / max(1.0, side_a * side_b))
        score = priority * aspect * math.sqrt(area / image_area) * max(0.2, fill)
        confidence = min(0.98, 0.45 + min(0.35, aspect / 30.0) + (fill * 0.18))
        scored.append((score, quad_from_points(contour.reshape(-1, 2)), confidence))
    return scored


def tick_pattern_quality(rows):
    if len(rows) < 5:
        return 0.0
    diffs = np.diff(rows)
    diffs = diffs[diffs >= 3]
    if len(diffs) < 4:
        return 0.0
    median = float(np.median(diffs))
    regularity = float(np.mean(np.abs(diffs - median) <= max(2.0, median * 0.35)))
    count_score = min(1.0, len(rows) / 24.0)
    return max(0.0, min(1.0, regularity * 0.65 + count_score * 0.35))


def order_quad(points):
    result = np.zeros((4, 2), dtype=np.float32)
    sums = points.sum(axis=1)
    differences = np.diff(points, axis=1).reshape(-1)
    result[0] = points[np.argmin(sums)]
    result[2] = points[np.argmax(sums)]
    result[1] = points[np.argmin(differences)]
    result[3] = points[np.argmax(differences)]
    return result


def rectify_quad(image, source):
    top_left, top_right, bottom_right, bottom_left = source
    width = max(
        2,
        int(round(max(np.linalg.norm(top_right - top_left), np.linalg.norm(bottom_right - bottom_left)))),
    )
    height = max(
        2,
        int(round(max(np.linalg.norm(bottom_left - top_left), np.linalg.norm(bottom_right - top_right)))),
    )
    destination = np.array(
        [[0, 0], [width - 1, 0], [width - 1, height - 1], [0, height - 1]],
        dtype=np.float32,
    )
    transform = cv2.getPerspectiveTransform(source, destination)

    if width > height:
        rotation = np.array([[0, -1, height - 1], [1, 0, 0], [0, 0, 1]], dtype=np.float64)
        transform = rotation @ transform
        width, height = height, width

    rectified = cv2.warpPerspective(image, transform, (width, height))
    return transform, rectified, width, height


def detect_tick_rows(rectified):
    gray = cv2.cvtColor(rectified, cv2.COLOR_BGR2GRAY)
    binary = cv2.adaptiveThreshold(
        gray,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY_INV,
        31,
        9,
    )
    width = binary.shape[1]
    band = max(3, int(width * 0.42))
    row_scores = np.maximum(
        (binary[:, :band] > 0).sum(axis=1),
        (binary[:, width - band:] > 0).sum(axis=1),
    )
    smoothed = np.convolve(row_scores, np.ones(3) / 3.0, mode="same")
    threshold = max(width * 0.08, float(np.percentile(smoothed, 72)))
    return group_positions(np.where(smoothed >= threshold)[0])


def group_positions(values):
    if len(values) == 0:
        return []
    groups = []
    current = [int(values[0])]
    for value in values[1:]:
        value = int(value)
        if value > current[-1] + 2:
            groups.append(float(np.mean(current)))
            current = []
        current.append(value)
    groups.append(float(np.mean(current)))
    return groups


def estimate_tick_spacing(rows):
    if len(rows) < 3:
        return None
    diffs = np.diff(rows)
    diffs = diffs[diffs >= 3]
    if len(diffs) == 0:
        return None
    median = float(np.median(diffs))
    regular = diffs[np.abs(diffs - median) <= max(2.0, median * 0.35)]
    return float(np.median(regular)) if len(regular) else median


def calibrate_ruler(ruler, reference_points, pixels_per_tick, tick_distance_cm):
    if len(reference_points) >= 2:
        samples = []
        for point in reference_points:
            rectified_point = transform_point(ruler["transform"], point["x"], point["y"])
            samples.append((rectified_point[1], point["valueCm"], 1.0))
        slope, intercept, fit_confidence = robust_linear_fit(samples, None, tick_distance_cm)
        return calibration_result(slope, intercept, "analyst-reference-points", 0.95 * fit_confidence)

    ocr_samples = read_ruler_labels(ruler["rectified"])
    if len(ocr_samples) >= 2:
        slope, intercept, fit_confidence = robust_linear_fit(
            ocr_samples,
            pixels_per_tick,
            tick_distance_cm,
        )
        return calibration_result(slope, intercept, "ocr-major-labels", 0.82 * fit_confidence)

    fail(
        "Ruler OCR could not establish an absolute scale. "
        "Provide at least two ruler reference points with x, y and valueCm",
    )


def calibration_result(slope, intercept, method, confidence):
    if abs(slope) < 1e-9:
        fail("Ruler calibration points do not define a usable scale")
    return {
        "slope": float(slope),
        "intercept": float(intercept),
        "cm_per_pixel": abs(float(slope)),
        "method": method,
        "confidence": max(0.05, min(0.99, float(confidence))),
    }


def read_ruler_labels(rectified):
    try:
        import pytesseract
        from pytesseract import Output
    except ImportError:
        return []

    tesseract_cmd = os.environ.get("TESSERACT_CMD")
    if tesseract_cmd:
        pytesseract.pytesseract.tesseract_cmd = tesseract_cmd

    samples = []
    base = cv2.cvtColor(rectified, cv2.COLOR_BGR2GRAY)
    for rotation in (0, 90, -90, 180):
        rotated = rotate_image(base, rotation)
        enlarged = cv2.resize(rotated, None, fx=2.0, fy=2.0, interpolation=cv2.INTER_CUBIC)
        prepared = cv2.threshold(enlarged, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
        try:
            data = pytesseract.image_to_data(
                prepared,
                output_type=Output.DICT,
                config="--psm 11 -c tessedit_char_whitelist=0123456789.",
            )
        except Exception:
            continue

        for index, text in enumerate(data["text"]):
            match = re.fullmatch(r"\d{1,3}(?:\.\d+)?", text.strip())
            if not match:
                continue
            confidence = float(data["conf"][index])
            if confidence < 30:
                continue
            center_x = (float(data["left"][index]) + float(data["width"][index]) / 2.0) / 2.0
            center_y = (float(data["top"][index]) + float(data["height"][index]) / 2.0) / 2.0
            original_y = rotated_to_original_y(center_x, center_y, base.shape, rotation)
            samples.append((original_y, float(match.group()), min(1.0, confidence / 100.0)))

    return deduplicate_ocr_samples(samples)


def rotate_image(image, rotation):
    if rotation == 90:
        return cv2.rotate(image, cv2.ROTATE_90_CLOCKWISE)
    if rotation == -90:
        return cv2.rotate(image, cv2.ROTATE_90_COUNTERCLOCKWISE)
    if rotation == 180:
        return cv2.rotate(image, cv2.ROTATE_180)
    return image


def rotated_to_original_y(x, y, original_shape, rotation):
    height, _ = original_shape
    if rotation == 90:
        return height - 1 - x
    if rotation == -90:
        return x
    if rotation == 180:
        return height - 1 - y
    return y


def deduplicate_ocr_samples(samples):
    samples.sort(key=lambda item: item[2], reverse=True)
    result = []
    for sample in samples:
        if any(abs(sample[0] - existing[0]) < 8 and abs(sample[1] - existing[1]) < 0.5 for existing in result):
            continue
        result.append(sample)
    return result


def robust_linear_fit(samples, pixels_per_tick, tick_distance_cm):
    if len(samples) < 2:
        fail("At least two distinct ruler values are required for calibration")

    expected_slope = None
    if pixels_per_tick and pixels_per_tick > 0:
        expected_slope = tick_distance_cm / pixels_per_tick

    best = None
    for first, second in combinations(samples, 2):
        dy = second[0] - first[0]
        dv = second[1] - first[1]
        if abs(dy) < 2 or abs(dv) < 0.01:
            continue
        slope = dv / dy
        if expected_slope and not (expected_slope * 0.25 <= abs(slope) <= expected_slope * 4.0):
            continue
        intercept = first[1] - slope * first[0]
        residual_limit = max(1.25, tick_distance_cm * 1.5)
        inliers = [
            sample
            for sample in samples
            if abs((slope * sample[0] + intercept) - sample[1]) <= residual_limit
        ]
        weighted_score = sum(sample[2] for sample in inliers)
        candidate = (weighted_score, len(inliers), slope, intercept, inliers)
        if best is None or candidate[:2] > best[:2]:
            best = candidate

    if best is None or best[1] < 2:
        fail("Ruler labels/reference points are inconsistent")

    ys = np.array([sample[0] for sample in best[4]], dtype=np.float64)
    values = np.array([sample[1] for sample in best[4]], dtype=np.float64)
    weights = np.array([sample[2] for sample in best[4]], dtype=np.float64)
    slope, intercept = np.polyfit(ys, values, 1, w=weights)
    predicted = slope * ys + intercept
    mean_error = float(np.average(np.abs(predicted - values), weights=weights))
    confidence = min(1.0, (len(best[4]) / max(2.0, len(samples))) * (1.0 / (1.0 + mean_error)))
    return slope, intercept, max(0.4, confidence)


def measure_selection(selection, ruler, calibration):
    center_x = (selection["x1"] + selection["x2"]) / 2.0
    center_y = (selection["y1"] + selection["y2"]) / 2.0
    top_rect = transform_point(ruler["transform"], center_x, selection["y1"])
    bottom_rect = transform_point(ruler["transform"], center_x, selection["y2"])
    center_rect = transform_point(ruler["transform"], center_x, center_y)

    values = [
        ruler_value(calibration, top_rect[1]),
        ruler_value(calibration, bottom_rect[1]),
    ]
    center_height = ruler_value(calibration, center_rect[1])
    anchor_point = inverse_ruler_point(ruler, calibration, center_height)

    return {
        "center_height_cm": center_height,
        "min_height_cm": min(values),
        "max_height_cm": max(values),
        "anchor_point": anchor_point,
    }


def ruler_value(calibration, rectified_y):
    return calibration["slope"] * rectified_y + calibration["intercept"]


def inverse_ruler_point(ruler, calibration, value_cm):
    rectified_y = (value_cm - calibration["intercept"]) / calibration["slope"]
    return transform_point(
        ruler["inverse_transform"],
        ruler["width"] / 2.0,
        rectified_y,
    )


def reference_segment(ruler, calibration, center_value_cm, distance_cm):
    first = inverse_ruler_point(ruler, calibration, center_value_cm - distance_cm / 2.0)
    second = inverse_ruler_point(ruler, calibration, center_value_cm + distance_cm / 2.0)
    return first, second


def transform_point(matrix, x, y):
    point = np.array([[[float(x), float(y)]]], dtype=np.float32)
    transformed = cv2.perspectiveTransform(point, np.asarray(matrix, dtype=np.float64))[0][0]
    return float(transformed[0]), float(transformed[1])


def annotate_image(image, ruler, selection, damage, confidence, method):
    draw = ImageDraw.Draw(image)
    polygon = [tuple(map(float, point)) for point in ruler["polygon"]]
    draw.line(polygon + [polygon[0]], fill=(0, 180, 0), width=4)
    draw.rectangle(
        (selection["x1"], selection["y1"], selection["x2"], selection["y2"]),
        outline=(255, 0, 0),
        width=4,
    )
    anchor_x, anchor_y = damage["anchor_point"]
    draw.line((0, anchor_y, image.width, anchor_y), fill=(0, 110, 255), width=3)
    draw.ellipse((anchor_x - 6, anchor_y - 6, anchor_x + 6, anchor_y + 6), fill=(0, 180, 0))
    draw.text(
        (10, 10),
        (
            f"height={damage['center_height_cm']:.2f}cm "
            f"range={damage['min_height_cm']:.2f}-{damage['max_height_cm']:.2f}cm "
            f"confidence={confidence:.2f} calibration={method}"
        ),
        fill=(255, 0, 0),
    )
    return image


def save_aligned_comparison(output_dir, base_name, primary, comparison):
    primary_image = primary["annotated_image"]
    output_path = output_dir / f"{base_name}.jpg"
    if comparison is None:
        primary_image.save(output_path, quality=92)
        return str(output_path)

    comparison_image = comparison["annotated_image"]
    target_cm_per_pixel = max(primary["scale_cm_per_pixel"], comparison["scale_cm_per_pixel"])
    primary_image, primary_factor = normalize_image_scale(
        primary_image,
        primary["scale_cm_per_pixel"],
        target_cm_per_pixel,
    )
    comparison_image, comparison_factor = normalize_image_scale(
        comparison_image,
        comparison["scale_cm_per_pixel"],
        target_cm_per_pixel,
    )

    alignment_value = primary["anchor_value_cm"]
    primary_anchor = primary["anchor_y"] * primary_factor
    comparison_anchor_point = inverse_ruler_point(
        comparison["ruler"],
        comparison["calibration"],
        alignment_value,
    )
    comparison_anchor = comparison_anchor_point[1] * comparison_factor

    top_margin = 32
    aligned_y = max(primary_anchor, comparison_anchor) + top_margin
    primary_offset = int(round(aligned_y - primary_anchor))
    comparison_offset = int(round(aligned_y - comparison_anchor))
    canvas_height = max(
        primary_offset + primary_image.height,
        comparison_offset + comparison_image.height,
    )
    canvas = Image.new("RGB", (primary_image.width + comparison_image.width, canvas_height), "white")
    canvas.paste(primary_image, (0, primary_offset))
    canvas.paste(comparison_image, (primary_image.width, comparison_offset))

    draw = ImageDraw.Draw(canvas)
    draw.line((0, aligned_y, canvas.width, aligned_y), fill=(0, 110, 255), width=3)
    draw.text((10, max(2, aligned_y - 24)), f"aligned ruler value: {alignment_value:.2f} cm", fill=(0, 80, 200))
    canvas.save(output_path, quality=92)
    return str(output_path)


def normalize_image_scale(image, source_cm_per_pixel, target_cm_per_pixel):
    resize_factor = source_cm_per_pixel / target_cm_per_pixel
    if abs(resize_factor - 1.0) < 0.005:
        return image, 1.0
    width = max(1, round(image.width * resize_factor))
    height = max(1, round(image.height * resize_factor))
    return image.resize((width, height), RESAMPLE_FILTER), resize_factor


def to_original(value, scale):
    return round(float(value) * scale, 2)


def finite_number(value):
    return isinstance(value, (int, float)) and math.isfinite(float(value))


def finite_positive(value):
    return finite_number(value) and float(value) > 0


def fail(message):
    print(message, file=sys.stderr)
    sys.exit(2)


if __name__ == "__main__":
    main()
