"""Generate a marketing hero using an actual DZlog emulator camera screenshot.
Only the black camera feed is replaced with staged plant photography.
The record table and onboarding controls stay live Jetpack Compose UI.
"""
from argparse import ArgumentParser
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageOps

SIZE = (1400, 1050)

def cover(src, dimensions, centering=(.5, .5)):
    return ImageOps.fit(src.convert("RGB"), dimensions, method=Image.Resampling.LANCZOS,
                        centering=centering).convert("RGBA")

def make_phone(camera_file, plant_file):
    source = Image.open(camera_file).convert("RGBA")
    width, height = source.size
    if width < 500 or height < 1100:
        raise ValueError(f"Unexpected actual DZlog camera capture: {source.size}")
    top, bottom = round(height * .204), round(height * .803)
    feed = cover(Image.open(plant_file), (width, bottom - top), centering=(.54, .46))
    shot = source.copy()
    shot.alpha_composite(feed, (0, top))
    # Restore visible buttons/record labels from the real camera screenshot.
    original = source.crop((0, top, width, bottom))
    mask = original.convert("L").point(lambda value: 255 if value > 70 else 0)
    shot.paste(original, (0, top), mask.filter(ImageFilter.GaussianBlur(.4)))
    face = cover(shot, (410, 915))
    phone = Image.new("RGBA", (436, 939))
    draw = ImageDraw.Draw(phone)
    draw.rounded_rectangle((0, 0, 435, 938), radius=47, fill=(28, 29, 27, 255))
    clip = Image.new("L", face.size, 0)
    ImageDraw.Draw(clip).rounded_rectangle((0, 0, 409, 914), radius=34, fill=255)
    phone.paste(face, (13, 12), clip)
    draw.rounded_rectangle((174, 18, 265, 35), radius=10, fill=(25, 25, 25, 240))
    return phone.rotate(9, Image.Resampling.BICUBIC, expand=True)

def main():
    parser = ArgumentParser()
    parser.add_argument("--desk", required=True)
    parser.add_argument("--plant", required=True)
    parser.add_argument("--camera", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    bg = cover(Image.open(args.desk), SIZE, centering=(.54, .56))
    bg = Image.alpha_composite(bg, Image.new("RGBA", SIZE, (250, 241, 225, 28)))
    phone = make_phone(args.camera, args.plant)
    # The final Android hero uses a narrow portrait crop of this landscape art.
    # Position the actual DZlog phone inside the crop, not beyond its left edge.
    phone = phone.resize((round(phone.width * .84), round(phone.height * .84)), Image.Resampling.LANCZOS)
    px, py = 466, 114
    shadow = Image.new("RGBA", phone.size, (17, 20, 14, 0))
    shadow.putalpha(phone.getchannel("A").point(lambda a: round(a * .26)))
    layer = Image.new("RGBA", SIZE)
    layer.alpha_composite(shadow, (px + 18, py + 20))
    bg = Image.alpha_composite(bg, layer.filter(ImageFilter.GaussianBlur(22)))
    bg.alpha_composite(phone, (px, py))
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    bg.convert("RGB").save(output, "WEBP", quality=87, method=6)
    print(f"Saved {output} {SIZE}, {output.stat().st_size} bytes")

if __name__ == "__main__":
    main()
