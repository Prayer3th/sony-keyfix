#!/usr/bin/env python3
"""Package only known public source paths, without keys, tools, APKs or dumps."""
import hashlib
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
FILES = ["AndroidManifest.xml", "build.py", "README.md", "README.en.md", "LICENSE.txt",
         "AdbLib-LICENSE.txt", "AdbLib-REVISION.txt", "NOTICE.txt", "CHANGELOG.md",
         "CONTRIBUTING.md", "SECURITY.md", ".gitignore", ".gitattributes"]
FOLDERS = {"src": {".java"}, "res": {".xml"}, "tests": {".java"},
           "tools": {".py"}, "docs": {".md", ".png"}, ".github": {".yml"}}


def main():
    selected = [ROOT / name for name in FILES]
    for folder, suffixes in FOLDERS.items():
        selected.extend(p for p in (ROOT / folder).rglob("*") if p.is_file() and p.suffix in suffixes)
    for path in selected:
        if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(ROOT):
            raise SystemExit(f"Unsafe or missing source path: {path}")
    version = ET.parse(ROOT / "AndroidManifest.xml").getroot().attrib["{http://schemas.android.com/apk/res/android}versionName"]
    if not all(c in "0123456789." for c in version):
        raise SystemExit("Invalid release version")
    dist = ROOT / "dist"
    if dist.is_symlink():
        raise SystemExit("dist must not be a symlink")
    dist.mkdir(exist_ok=True)
    output = dist / f"Sony-Keyfix-{version}-source.zip"
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(selected):
            archive.write(path, "sony-keyfix/" + path.relative_to(ROOT).as_posix())
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    output.with_suffix(".zip.sha256").write_text(f"{digest}  {output.name}\n", encoding="utf-8")
    print(f"Packaged {len(selected)} public files: {output}")


if __name__ == "__main__":
    main()
