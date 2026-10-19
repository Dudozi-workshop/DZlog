#!/usr/bin/env python3
"""Build licensed, offline onboarding photography. Run only for explicitly curated URLs.

No user gallery or camera data is read. Artwork text and controls remain Compose
elements, not baked into these images.
"""
from io import BytesIO
from pathlib import Path
from urllib.request import Request, urlopen
from PIL import Image, ImageOps

OUT = Path("app/src/main/res/drawable-nodpi")
# Photos served by Unsplash; standard Unsplash license for downloaded photos.
# Preserve stable photo IDs for auditing instead of scraping a search page.
PHOTOS = {
    "onboarding_hero_photo.jpg": "photo-1501004318641-b39e6451bec6",
    "onboarding_starter_photo.jpg": "photo-1485955900006-10f4d324d411",
    "onboarding_gallery_photo.jpg": "photo-1497250681960-ef046c08a56e",
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for filename, photo_id in PHOTOS.items():
        url = f"https://images.unsplash.com/{photo_id}?w=1360&q=86&fit=crop"
        request = Request(url, headers={"User-Agent": "DZlog-Onboarding-Asset-Builder/1.0"})
        with urlopen(request, timeout=35) as response:
            if response.status != 200:
                raise RuntimeError(f"Failed to fetch licensed photo {photo_id}")
            content = response.read(6 * 1024 * 1024 + 1)
        if not (30_000 < len(content) < 6 * 1024 * 1024):
            raise RuntimeError(f"Unexpected photo byte count for {photo_id}: {len(content)}")
        with Image.open(BytesIO(content)) as source:
            source = ImageOps.exif_transpose(source).convert("RGB")
            if source.width < 800 or source.height < 500:
                raise RuntimeError(f"Image is too small for onboarding: {source.size}")
            # Portrait-friendly framing with native Android drawable resources.
            photo = ImageOps.fit(source, (1200, 1000), method=Image.Resampling.LANCZOS)
            photo.save(OUT / filename, "JPEG", quality=84, optimize=True, progressive=True)
            print(f"Prepared {filename}: {photo.size}, {(OUT / filename).stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
