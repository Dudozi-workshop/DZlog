import json
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageOps

def grid_edges(image, low, high, cross_low, cross_high, vertical):
    candidates = []
    for n in range(low + 3, high - 3):
        values = [image.getpixel((n, m) if vertical else (m, n)) for m in range(cross_low + 2, cross_high - 2)]
        if sum(16 <= v <= 145 for v in values) > 0.7 * len(values):
            if candidates and n == candidates[-1][-1] + 1:
                candidates[-1].append(n)
            else:
                candidates.append([n])
    return [low, *(round(sum(group) / len(group)) for group in candidates), high]


def compare_fixture(fixture):
    grid = [row.split("|") for row in fixture.read_text().splitlines()]
    rows, cols = len(grid), len(grid[0])
    assert all(len(row) == cols for row in grid)
    labels = [label for row in grid for label in row]
    canvas = Image.new("RGB", (1200, 275), "white")
    draw = ImageDraw.Draw(canvas)
    results = {}
    issues = []
    for index, platform in enumerate(("android", "iphone")):
        im = Image.open(f"{platform}-{fixture.stem}.jpg").convert("RGB")
        gray = ImageOps.grayscale(im)
        bounds = gray.point(lambda p: 255 if p < 140 else 0).getbbox()
        draw.text((600 * index + 8, 8), platform, fill="black")
        canvas.paste(im.crop((200, 360, 800, 600)), (600 * index, 32))
        if im.size != (800, 600) or not bounds:
            issues.append(f"{platform}: missing table or wrong image size")
            continue
        left, top, right, bottom = bounds
        xs = grid_edges(gray, left, right, top, bottom, True)
        ys = grid_edges(gray, top, bottom, left, right, False)
        if len(xs) != cols + 1 or len(ys) != rows + 1:
            issues.append(f"{platform}: expected a {rows} by {cols} grid; got {xs}, {ys}")
            continue
        if abs(right - 800) > 2 or abs(bottom - 600) > 2:
            issues.append(f"{platform}: bottom-right anchor moved")
        masks = []
        boxes = []
        for i, label in enumerate(labels):
            r, c = divmod(i, cols)
            mask = gray.crop((xs[c] + 3, ys[r] + 3, xs[c+1] - 3, ys[r+1] - 3)).point(lambda p: 255 if p >= 160 else 0)
            box = mask.getbbox()
            boxes.append(box)
            masks.append(mask)
            if bool(box) != bool(label):
                issues.append(f"{platform}: missing text or ink in empty cell {i}")
            if box and (box[0] == 0 or box[2] == mask.width or box[1] == 0 or box[3] == mask.height):
                issues.append(f"{platform}: unexpected clipping in cell {i}")
        # Equal syllable-sized cells showing identical ink indicate a missing-glyph box.
        if labels[:2] == ["가", "나"] and boxes[0] and boxes[1]:
            first, second = [m.crop(m.getbbox()).resize((40, 40), Image.Resampling.NEAREST) for m in masks[:2]]
            delta = ImageChops.difference(first, second)
            union = ImageChops.lighter(first, second)
            difference = 1 - delta.histogram()[0] / (40 * 40)
            coverage = 1 - union.histogram()[0] / (40 * 40)
            if difference < 0.1 * coverage:
                issues.append(f"{platform}: 가 and 나 have the same glyph shape")
        results[platform] = dict(table_bounds=bounds, x_edges=xs, y_edges=ys, ink_bounds=boxes)
    if len(results) == 2:
        for key in ("x_edges", "y_edges"):
            delta = max(abs(a-b) for a,b in zip(results["android"][key], results["iphone"][key]))
            if delta > 6:
                issues.append(f"cross-platform: {key} differ by {delta}px")
        for i, (a,b) in enumerate(zip(results["android"]["ink_bounds"], results["iphone"]["ink_bounds"])):
            if a and b and any(abs((a[d]+a[d+2]) - (b[d]+b[d+2])) > 12 for d in (0, 1)):
                issues.append(f"cross-platform: inspect text alignment in cell {i}")
    canvas.save(f"comparison-{fixture.stem}.png")
    return dict(results=results, issues=issues)


if __name__ == "__main__":
    measurements = {path.stem: compare_fixture(path) for path in sorted(Path(__file__).parent.glob("cells*.txt"))}
    assert measurements
    report = json.dumps(measurements, ensure_ascii=False, indent=2)
    Path("measurements.json").write_text(report)
    print(report)
    if any(result["issues"] for result in measurements.values()):
        raise SystemExit(1)
