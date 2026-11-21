"""Prepare the actual emulator faces for a simulator-only font measurement experiment."""
import json
from pathlib import Path
import subprocess
import tempfile
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

out = Path("font-probe")
out.mkdir(exist_ok=True)
names = []
manifest = json.loads(Path("measured-faces.json").read_text())
with tempfile.TemporaryDirectory() as scratch:
    for index, face in enumerate(manifest):
        source = Path(face["path"])
        if source.parent not in (Path("/system/fonts"), Path("/product/fonts")):
            raise ValueError(f"Unexpected emulator font path {source}")
        local = Path(scratch) / source.name
        if not local.exists():
            subprocess.run(["adb", "pull", str(source), str(local)], check=True)
        font = TTFont(local, fontNumber=face["index"])
        if "fvar" in font:
            axes = {a.axisTag: a.defaultValue for a in font["fvar"].axes}
            axes.update(face["axes"])
            font = instantiateVariableFont(font, axes, inplace=True)
        name = font["name"].getDebugName(6)
        if not name or name in names:
            raise ValueError(f"Expected an identifiable, distinct font face: {face}")
        names.append(name)
        ext = "otf" if font.sfntVersion == "OTTO" else "ttf"
        font.save(out / f"face-{index}.{ext}")
(out / "names.json").write_text(json.dumps(names))
print(json.dumps(manifest, ensure_ascii=False, indent=2))
