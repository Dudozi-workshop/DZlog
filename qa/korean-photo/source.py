from PIL import Image

# A flat, bright field makes the exact dark table bounds measurable after JPEG encoding.
Image.new("RGB", (800, 600), (150, 182, 165)).save("source.jpg", quality=95)
