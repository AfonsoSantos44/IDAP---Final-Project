from PIL import Image, ImageDraw, ImageFont

from image_inputs import RESAMPLE_FILTER
from measurement_geometry import inverse_ruler_point


def annotate_image(image, ruler, selection, damage, confidence, method):
    draw = ImageDraw.Draw(image)
    stroke = max(3, round(image.width / 450))
    marker_radius = max(5, round(image.width / 300))
    font = load_scaled_font(image.width)

    polygon = [tuple(map(float, point)) for point in ruler["polygon"]]
    draw.line(polygon + [polygon[0]], fill=(0, 180, 0), width=stroke)
    draw.rectangle(
        (selection["x1"], selection["y1"], selection["x2"], selection["y2"]),
        outline=(255, 0, 0),
        width=stroke,
    )
    anchor_x, anchor_y = damage["anchor_point"]
    draw.line((0, anchor_y, image.width, anchor_y), fill=(0, 110, 255), width=max(2, stroke - 1))
    draw.ellipse(
        (
            anchor_x - marker_radius,
            anchor_y - marker_radius,
            anchor_x + marker_radius,
            anchor_y + marker_radius,
        ),
        fill=(0, 180, 0),
    )

    label = (
        f"height={damage['center_height_cm']:.2f}cm "
        f"range={damage['min_height_cm']:.2f}-{damage['max_height_cm']:.2f}cm "
        f"confidence={confidence:.2f} calibration={method}"
    )
    origin = (max(10, round(image.width * 0.012)), max(10, round(image.width * 0.012)))
    draw_label_with_background(draw, origin, label, font)
    return image


def load_scaled_font(image_width):
    size = max(16, round(image_width / 70))
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        # Older Pillow versions don't support a `size` argument here.
        return ImageFont.load_default()


def draw_label_with_background(draw, origin, text, font):
    bbox = draw.textbbox(origin, text, font=font)
    padding = 6
    draw.rectangle(
        (bbox[0] - padding, bbox[1] - padding, bbox[2] + padding, bbox[3] + padding),
        fill=(255, 255, 255),
    )
    draw.text(origin, text, fill=(200, 0, 0), font=font)


def save_aligned_comparison(output_dir, base_name, primary, comparison):
    output_dir.mkdir(parents=True, exist_ok=True)
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
    comparison_anchor = (
            comparison_anchor_point[1] * comparison["scale_to_original"] * comparison_factor
    )

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