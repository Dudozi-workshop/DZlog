import json
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageOps

labels = Path(__file__).with_name("cells.txt").read_text().splitlines()
canvas = Image.new("RGB", (1200, 275), "white")
draw = ImageDraw.Draw(canvas)
results = {}
issues = []

for index, platform in enumerate(("android", "iphone")):
    im = Image.open(f"{platform}.jpg").convert("RGB")
    gray = ImageOps.grayscale(im)
    bounds = gray.point(lambda p: 255 if p < 45 else 0).getbbox()
    if im.size != (800, 600) or not bounds or max(abs(a-b) for a, b in zip(bounds, (200, 360, 800, 600))) > 2:
        issues.append(f"{platform}: unexpected table bounds {bounds}")
    draw.text((600 * index + 8, 8), platform, fill="black")
    canvas.paste(im.crop((200, 360, 800, 600)), (600 * index, 32))
    masks = []
    boxes = []
    for i, label in enumerate(labels):
        x = 200 + (i % 3) * 200
        y = 360 + (i // 3) * 80
        mask = gray.crop((x + 3, y + 3, x + 197, y + 77)).point(lambda p: 255 if p >= 160 else 0)
        box = mask.getbbox()
        boxes.append(box)
        masks.append(mask)
        if bool(box) != bool(label):
            issues.append(f"{platform}: missing text or ink in empty cell {i}")
        if box and i != 6 and (box[0] == 0 or box[2] == mask.width or box[1] == 0 or box[3] == mask.height):
            issues.append(f"{platform}: unexpected clipping in cell {i}")
    # Equal syllable-sized cells showing identical ink indicate a missing-glyph box.
    if boxes[0] and boxes[1]:
        first, second = [m.crop(m.getbbox()).resize((40, 40), Image.Resampling.NEAREST) for m in masks[:2]]
        delta = ImageChops.difference(first, second)
        union = ImageChops.lighter(first, second)
        difference = 1 - delta.histogram()[0] / (40 * 40)
        coverage = 1 - union.histogram()[0] / (40 * 40)
        if difference < 0.1 * coverage:
            issues.append(f"{platform}: 가 and 나 have the same glyph shape")
    results[platform] = dict(table_bounds=bounds, ink_bounds=boxes)
for i, (a, b) in enumerate(zip(results["android"]["ink_bounds"], results["iphone"]["ink_bounds"])):
    if a and b:
        # System fonts differ. Flag a center displacement larger than 5 pixels for inspection.
        if any(abs((a[d] + a[d+2]) - (b[d] + b[d+2])) > 10 for d in (0, 1)):
            issues.append(f"cross-platform: inspect text alignment in cell {i}")
canvas.save("comparison.png")
Path("measurements.json").write_text(json.dumps(dict(results=results, issues=issues), ensure_ascii=False, indent=2))
print(Path("measurements.json").read_text())
if issues:
    raise SystemExit(1)
