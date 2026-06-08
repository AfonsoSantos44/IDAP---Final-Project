import tempfile
import unittest
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

from comparison_rendering import normalize_image_scale
from measurement_processing import process_image
from ruler_calibration import parse_ruler_values, robust_linear_fit


class MeasurementEngineTests(unittest.TestCase):
    def test_parses_consecutive_ruler_values_from_one_ocr_box(self):
        self.assertEqual(
            [64, 65, 66, 67, 68, 69],
            parse_ruler_values("646566676869"),
        )
        self.assertEqual([78, 79], parse_ruler_values("78 79"))
        self.assertEqual([], parse_ruler_values("810203"))

    def test_ocr_fit_rejects_mirrored_and_noisy_label_families(self):
        samples = [
            (455.5, 80.0, 0.99),
            (511.7, 79.0, 0.99),
            (555.2, 78.0, 0.99),
            (598.6, 77.0, 0.99),
            (642.1, 76.0, 0.99),
            (686.0, 75.0, 0.99),
            (915.0, 70.0, 0.99),
            (40.5, 81.0, 0.99),
            (85.5, 82.0, 0.99),
            (130.5, 83.0, 0.99),
            (361.5, 28.0, 0.60),
        ]

        slope, intercept, _ = robust_linear_fit(samples, 5.0, 1.0)
        measured_value = (slope * 582.5) + intercept

        self.assertLess(slope, 0)
        self.assertAlmostEqual(77.5, measured_value, delta=0.8)

    def test_perspective_ruler_uses_analyst_points_for_damage_range(self):
        with tempfile.TemporaryDirectory() as directory:
            image_path = Path(directory) / "perspective-ruler.png"
            image = np.full((800, 600, 3), 255, dtype=np.uint8)
            ruler_polygon = np.array(
                [[65, 35], [145, 55], [118, 760], [38, 740]],
                dtype=np.int32,
            )
            cv2.fillConvexPoly(image, ruler_polygon, (0, 220, 255))

            for y in range(90, 730, 32):
                left = int(63 - ((y - 35) * 25 / 705))
                right = int(143 - ((y - 55) * 25 / 705))
                cv2.line(image, (left, y), (right - 15, y), (0, 0, 0), 3)

            cv2.imwrite(str(image_path), image)

            processed = process_image(
                image_path,
                {"x1": 250, "y1": 345, "x2": 500, "y2": 435},
                {
                    "referencePoints": [
                        {"x": 92, "y": 160, "valueCm": 80},
                        {"x": 70, "y": 640, "valueCm": 32},
                    ],
                    "rulerRegion": {"x1": 20, "y1": 15, "x2": 165, "y2": 780},
                },
                1.0,
            )

            result = processed["result"]
            self.assertEqual("analyst-reference-points", result["calibrationMethod"])
            self.assertGreater(result["calculatedHeightCm"], 45)
            self.assertLess(result["calculatedHeightCm"], 65)
            self.assertLess(result["damageMinHeightCm"], result["calculatedHeightCm"])
            self.assertGreater(result["damageMaxHeightCm"], result["calculatedHeightCm"])
            self.assertGreater(result["confidence"], 0.6)

            polygon = processed["ruler"]["polygon"]
            self.assertGreater(abs(float(polygon[0][0]) - float(polygon[3][0])), 10)

    def test_scale_normalization_uses_centimetres_per_pixel(self):
        image = Image.new("RGB", (200, 100), "white")
        normalized, factor = normalize_image_scale(
            image,
            source_cm_per_pixel=0.5,
            target_cm_per_pixel=1.0,
        )

        self.assertEqual((100, 50), normalized.size)
        self.assertEqual(0.5, factor)


if __name__ == "__main__":
    unittest.main()
