import math

import numpy as np

from engine_support import cv2, fail


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

    _, box, confidence, tick_quality = max(rescored, key=lambda item: item[0])
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
