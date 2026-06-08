from engine_support import transform_point


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
