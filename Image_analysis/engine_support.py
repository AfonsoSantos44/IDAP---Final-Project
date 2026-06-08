import math
import sys

import numpy as np

try:
    import cv2
except ImportError:
    cv2 = None


def configure_utf8_stdio():
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")


def require_opencv():
    if cv2 is None:
        fail("OpenCV is required. Install Image_analysis/requirements.txt")


def transform_point(matrix, x, y):
    point = np.array([[[float(x), float(y)]]], dtype=np.float32)
    transformed = cv2.perspectiveTransform(point, np.asarray(matrix, dtype=np.float64))[0][0]
    return float(transformed[0]), float(transformed[1])


def finite_number(value):
    return isinstance(value, (int, float)) and math.isfinite(float(value))


def finite_positive(value):
    return finite_number(value) and float(value) > 0


def fail(message):
    print(message, file=sys.stderr)
    raise SystemExit(2)
