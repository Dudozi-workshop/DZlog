#!/usr/bin/env python3
"""Build local, offline, photograph-based onboard hero assets.
The static artwork is decorative only: headings, navigation, record tables,
template choices and permissions remain live Compose UI.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageOps
ASSETS = Path("app/src/main/res/drawable-nodpi")

def cover(src,size,centering=(.5,.5)):
    return ImageOps.fit(src.convert("RGB"),size,method=Image.Resampling.LANCZOS,centering=centering).convert("RGBA")

def rounded(src,r):
    m=Image.new("L",src.size,0)
    ImageDraw.Draw(m).rounded_rectangle((0,0,src.width-1,src.height-1),radius=r,fill=255)
    src.putalpha(m)
    return src

def build():
    plant=Image.open(ASSETS/"onboarding_hero_photo.jpg")
    detail=Image.open(ASSETS/"onboarding_starter_photo.jpg")
    gallery=Image.open(ASSETS/"onboarding_gallery_photo.jpg")
    art=cover(plant,(1080,900),(.49,.58))
    art=Image.alpha_composite(art,Image.new("RGBA",art.size,(250,236,216,35)))
    phone=Image.new("RGBA",(390,622))
    d=ImageDraw.Draw(phone)
    d.rounded_rectangle((7,6,381,610),radius=51,fill="#272824")
    phone.alpha_composite(rounded(cover(detail,(350,567),(.42,.5)),36),(21,23))
    d=ImageDraw.Draw(phone)
    d.rounded_rectangle((148,35,240,50),radius=10,fill="#242622")
    d.ellipse((153,544,228,619),fill="#fffaf3",outline="#353a34",width=7)
    phone=phone.rotate(12,Image.Resampling.BICUBIC,expand=True)
    shadow=Image.new("RGBA",art.size)
    m=Image.new("L",art.size)
    ImageDraw.Draw(m).rounded_rectangle((72,250,540,857),radius=40,fill=115)
    shadow.putalpha(m.filter(ImageFilter.GaussianBlur(24)))
    art=Image.alpha_composite(art,shadow)
    art.alpha_composite(phone,(38,205))
    for i,photo in enumerate((plant,detail,gallery)):
        tile=Image.new("RGBA",(160,166),"#fffaf5")
        tile.alpha_composite(rounded(cover(photo,(146,127)),10),(7,7))
        tile=tile.rotate((-7,0,6)[i],Image.Resampling.BICUBIC,expand=True)
        art.alpha_composite(tile,(544+134*i,695-14*i))
    art.convert("RGB").save(ASSETS/"onboarding_reference_hero.webp","WEBP",quality=84,method=6)
    # Starter has an open area for the *real* table and selection preview.
    starter=cover(detail,(1080,640),(.5,.51))
    starter=Image.alpha_composite(starter,Image.new("RGBA",starter.size,(240,227,209,28)))
    for i,photo in enumerate((detail,gallery,plant)):
        starter.alpha_composite(rounded(cover(photo,(150,132)),12),(46+162*i,480))
    starter.convert("RGB").save(ASSETS/"onboarding_reference_starter.webp","WEBP",quality=82,method=6)
    print("Created embedded hero/starter artwork; live UI remains separate")

if __name__=="__main__":
    build()
