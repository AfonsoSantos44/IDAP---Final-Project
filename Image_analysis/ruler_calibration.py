import os
import re
from itertools import combinations

import numpy as np

from engine_support import cv2, fail, transform_point


_RAPID_OCR = None


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
    samples = read_ruler_labels_with_rapidocr(rectified)
    if len(samples) >= 2:
        return deduplicate_ocr_samples(samples)

    samples.extend(read_ruler_labels_with_tesseract(rectified))
    return deduplicate_ocr_samples(samples)


def read_ruler_labels_with_rapidocr(rectified):
    try:
        from rapidocr import RapidOCR
    except ImportError:
        return []

    global _RAPID_OCR
    if _RAPID_OCR is None:
        try:
            _RAPID_OCR = RapidOCR()
        except Exception:
            return []

    # The rectified ruler is a long, thin strip: its width (the physical ruler
    # width) is often only 15-60 px. RapidOCR internally downscales large images,
    # so feeding the whole strip shrinks the digits back to a few pixels and the
    # detector reads nothing. To avoid this we rotate the strip so the digits are
    # upright, upscale it until the digits are large enough, and OCR it in
    # overlapping tiles kept small enough that RapidOCR does not downscale them.
    # OCR box coordinates are mapped back (tile -> upscaled -> rotated) so the
    # rectified-y of each label is preserved.
    target_height = 96.0
    max_dimension = 6000     # nao processar tiras absurdamente grandes (evita OOM/crash)
    max_tiles = 60           # limite de blocos por rotacao
    samples = []
    for rotation in (90, -90):
        rotated = rotate_image(rectified, rotation)
        height, width = rotated.shape[:2]
        if height < 3 or width < 3:
            continue

        scale = max(1.0, target_height / max(1, height))
        if width * scale > max_dimension:
            scale = max(1.0, max_dimension / max(1, width))
        if scale > 1.0:
            enlarged = cv2.resize(
                rotated,
                (max(1, int(round(width * scale))), max(1, int(round(height * scale)))),
                interpolation=cv2.INTER_CUBIC,
            )
        else:
            enlarged = rotated
        enlarged = np.ascontiguousarray(enlarged)

        big_h, big_w = enlarged.shape[:2]
        tile_w = max(int(big_h * 6), 320)
        step = max(1, tile_w - tile_w // 5)
        offset = 0
        tiles_done = 0
        while offset < big_w and tiles_done < max_tiles:
            tile_offset = offset
            offset += step
            tiles_done += 1
            tile = enlarged[:, tile_offset:min(big_w, tile_offset + tile_w)]
            if tile.shape[0] < 8 or tile.shape[1] < 8:
                continue
            try:
                output = _RAPID_OCR(np.ascontiguousarray(tile))
            except Exception:
                continue

            texts = output.txts or ()
            boxes = output.boxes if output.boxes is not None else ()
            scores = output.scores or ()
            for text, box, confidence in zip(texts, boxes, scores):
                confidence = float(confidence)
                if confidence < 0.5:
                    continue
                mapped = np.asarray(box, dtype=np.float64)
                mapped[:, 0] += tile_offset   # tile -> imagem ampliada
                mapped /= scale               # ampliada -> rodada (escala original)
                samples.extend(
                    rapidocr_box_samples(
                        text=str(text),
                        box=mapped,
                        confidence=confidence,
                        original_shape=rectified.shape[:2],
                        rotation=rotation,
                    )
                )

    return samples


def rapidocr_box_samples(text, box, confidence, original_shape, rotation):
    values = parse_ruler_values(text)
    if not values:
        return []

    left = float(np.min(box[:, 0]))
    right = float(np.max(box[:, 0]))
    top = float(np.min(box[:, 1]))
    bottom = float(np.max(box[:, 1]))
    horizontal = (right - left) >= (bottom - top)
    samples = []

    for index, value in enumerate(values):
        fraction = (index + 0.5) / len(values)
        x = left + ((right - left) * fraction) if horizontal else (left + right) / 2.0
        y = (top + bottom) / 2.0 if horizontal else top + ((bottom - top) * fraction)
        original_y = rotated_to_original_y(x, y, original_shape, rotation)
        samples.append((original_y, float(value), confidence))

    return samples


def parse_ruler_values(text):
    digits = re.sub(r"\D", "", text)
    if not digits:
        return []
    if len(digits) <= 3:
        value = int(digits)
        return [value] if 10 <= value <= 200 else []

    candidates = []
    for width in (2, 3):
        if len(digits) % width != 0:
            continue
        values = [int(digits[index:index + width]) for index in range(0, len(digits), width)]
        if len(values) < 2 or any(value < 10 or value > 200 for value in values):
            continue
        differences = np.diff(values)
        if np.all(differences == 1) or np.all(differences == -1):
            candidates.append(values)

    return max(candidates, key=len) if candidates else []


def read_ruler_labels_with_tesseract(rectified):
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

    return samples


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

    best = None
    for first, second in combinations(samples, 2):
        dy = second[0] - first[0]
        dv = second[1] - first[1]
        if abs(dy) < 2 or abs(dv) < 0.01:
            continue
        slope = dv / dy
        if slope >= 0:
            continue
        intercept = first[1] - slope * first[0]
        residual_limit = max(1.25, tick_distance_cm * 1.5)
        inliers = [
            sample
            for sample in samples
            if abs((slope * sample[0] + intercept) - sample[1]) <= residual_limit
        ]
        weighted_score = sum(sample[2] for sample in inliers)
        if len(inliers) >= 2:
            y_span = max(sample[0] for sample in inliers) - min(sample[0] for sample in inliers)
            value_span = max(sample[1] for sample in inliers) - min(sample[1] for sample in inliers)
        else:
            y_span = 0.0
            value_span = 0.0
        tick_score = tick_slope_score(slope, pixels_per_tick, tick_distance_cm)
        candidate = (
            len(inliers),
            weighted_score,
            tick_score,
            min(1.0, y_span / 300.0) + min(1.0, value_span / 10.0),
            slope,
            intercept,
            inliers,
        )
        if best is None or candidate[:4] > best[:4]:
            best = candidate

    if best is None or best[0] < 2:
        fail("Ruler labels/reference points are inconsistent")

    ys = np.array([sample[0] for sample in best[6]], dtype=np.float64)
    values = np.array([sample[1] for sample in best[6]], dtype=np.float64)
    weights = np.array([sample[2] for sample in best[6]], dtype=np.float64)
    slope, intercept = np.polyfit(ys, values, 1, w=weights)
    predicted = slope * ys + intercept
    mean_error = float(np.average(np.abs(predicted - values), weights=weights))
    confidence = min(1.0, (len(best[6]) / max(2.0, len(samples))) * (1.0 / (1.0 + mean_error)))
    return slope, intercept, max(0.4, confidence)


def tick_slope_score(slope, pixels_per_tick, tick_distance_cm):
    if not pixels_per_tick or pixels_per_tick <= 0 or abs(slope) < 1e-9:
        return 0.0

    pixels_per_value = tick_distance_cm / abs(slope)
    ratio = pixels_per_value / pixels_per_tick
    expected_ratios = [1.0 / divisor for divisor in range(1, 11)] + list(range(1, 21))
    relative_error = min(abs(ratio - expected) / expected for expected in expected_ratios)
    return max(0.0, 1.0 - relative_error)
