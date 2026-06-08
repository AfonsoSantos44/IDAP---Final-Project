from PIL import Image

from engine_support import fail, finite_number


MAX_ANALYSIS_SIDE = 1800
RESAMPLE_FILTER = getattr(Image, "Resampling", Image).LANCZOS


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


def to_original(value, scale):
    return round(float(value) * scale, 2)
