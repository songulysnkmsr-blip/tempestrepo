#!/usr/bin/env python3
"""
Tachimanga / Tachiyomi Extension Repository Index Generator.
Generates repo.json, index.json, and index.min.json compatible with Tachimanga (iOS) and Mihon/Tachiyomi.
"""

import os
import sys
import json
import shutil
import hashlib
import struct
import re
import subprocess
from pathlib import Path

def generate_source_id(name: str, lang: str, version_id: int = 1) -> str:
    key = f"{name.lower()}/{lang}/{version_id}".encode("utf-8")
    md5 = hashlib.md5(key).digest()
    val = struct.unpack(">Q", md5[:8])[0] & 0x7FFFFFFFFFFFFFFF
    return str(val)

def sha256_file(filepath: Path) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

EXTENSIONS_META = {
    "juratempest": {
        "name": "Jura Tempest",
        "lang": "tr",
        "baseUrl": "https://juratempe.st",
        "pkgSuffix": "tr.juratempest",
        "nsfw": 0,
    },
    "mangtto": {
        "name": "Mangtto",
        "lang": "tr",
        "baseUrl": "https://mangtto.com",
        "pkgSuffix": "tr.mangtto",
        "nsfw": 0,
    },
    "orimanga": {
        "name": "Ori Manga",
        "lang": "tr",
        "baseUrl": "https://orimanga.net",
        "pkgSuffix": "tr.orimanga",
        "nsfw": 0,
    },
    "golgebahcesi": {
        "name": "Gölge Bahçesi",
        "lang": "tr",
        "baseUrl": "https://golgebahcesi.com",
        "pkgSuffix": "tr.golgebahcesi",
        "nsfw": 0,
    },
}

def main():
    root_dir = Path(__file__).resolve().parent.parent
    repo_out = root_dir / "repo"
    apk_out = repo_out / "apk"
    icon_out = repo_out / "icon"

    repo_out.mkdir(parents=True, exist_ok=True)
    apk_out.mkdir(parents=True, exist_ok=True)
    icon_out.mkdir(parents=True, exist_ok=True)

    signing_fp = "01e252e0645243a25e8726a236f2305c00fab708a98384727fbb1cfc840a960f"
    p12_path = root_dir / "signingkey.p12"
    if p12_path.exists():
        try:
            res = subprocess.run([
                'openssl', 'pkcs12', '-in', str(p12_path), '-nokeys', '-nodes', '-passin', 'pass:tempestpass'
            ], capture_output=True, text=True)
            res2 = subprocess.run([
                'openssl', 'x509', '-outform', 'DER'
            ], input=res.stdout.encode(), capture_output=True)
            if res2.stdout:
                signing_fp = hashlib.sha256(res2.stdout).hexdigest()
        except Exception:
            pass

    repo_json = {
        "meta": {
            "name": "Tempest Repo",
            "shortName": "TR",
            "website": "https://github.com/songulysnkmsr-blip/tempestrepo",
            "signingKeyFingerprint": signing_fp
        }
    }

    with open(repo_out / "repo.json", "w", encoding="utf-8") as f:
        json.dump(repo_json, f, indent=2, ensure_ascii=False)

    index_entries = []

    src_tr = root_dir / "src" / "tr"
    for ext_dir in sorted(src_tr.iterdir()):
        if not ext_dir.is_dir():
            continue

        slug = ext_dir.name
        meta = EXTENSIONS_META.get(slug, {
            "name": slug.capitalize(),
            "lang": "tr",
            "baseUrl": "",
            "pkgSuffix": f"tr.{slug}",
            "nsfw": 0,
        })

        build_gradle = ext_dir / "build.gradle.kts"
        lib_ver = "1.4"
        ext_ver_code = 1

        if build_gradle.exists():
            bg_content = build_gradle.read_text(encoding="utf-8")
            lib_match = re.search(r'set\("libVersion",\s*"([^"]+)"\)', bg_content)
            if lib_match:
                lib_ver = lib_match.group(1)
            code_match = re.search(r'set\("extVersionCode",\s*([0-9]+)\)', bg_content)
            if code_match:
                ext_ver_code = int(code_match.group(1))

        pkg = f"eu.kanade.tachiyomi.extension.{meta['pkgSuffix']}"
        version_name = f"{lib_ver}.{ext_ver_code}"
        apk_filename = f"tachiyomi-{meta['pkgSuffix']}-v{version_name}.apk"

        # Copy icon
        src_icon = ext_dir / "res" / "mipmap-xxhdpi" / "ic_launcher.png"
        if src_icon.exists():
            dest_icon = icon_out / f"{pkg}.png"
            shutil.copy2(src_icon, dest_icon)

        # Check for APK
        sha256 = ""
        found_apk = None
        release_dir = ext_dir / "build" / "outputs" / "apk" / "release"
        if release_dir.exists():
            for apk_cand in sorted(release_dir.glob("*.apk")):
                if not apk_cand.name.endswith("-unaligned.apk"):
                    found_apk = apk_cand
                    break

        if not found_apk and (apk_out / apk_filename).exists():
            found_apk = apk_out / apk_filename

        if found_apk and found_apk != apk_out / apk_filename:
            shutil.copy2(found_apk, apk_out / apk_filename)
            sha256 = sha256_file(apk_out / apk_filename)
        elif (apk_out / apk_filename).exists():
            sha256 = sha256_file(apk_out / apk_filename)

        source_id = generate_source_id(meta["name"], meta["lang"], 1)

        entry = {
            "name": meta["name"],
            "pkg": pkg,
            "apk": apk_filename,
            "lang": meta["lang"],
            "code": ext_ver_code,
            "version": version_name,
            "nsfw": meta["nsfw"],
            "hasReadme": 0,
            "hasChangelog": 0,
            "sources": [
                {
                    "name": meta["name"],
                    "lang": meta["lang"],
                    "id": source_id,
                    "baseUrl": meta["baseUrl"],
                }
            ]
        }

        if sha256:
            entry["sha256"] = sha256

        index_entries.append(entry)

    # Write index.json
    with open(repo_out / "index.json", "w", encoding="utf-8") as f:
        json.dump(index_entries, f, indent=2, ensure_ascii=False)

    # Write index.min.json
    with open(repo_out / "index.min.json", "w", encoding="utf-8") as f:
        json.dump(index_entries, f, separators=(",", ":"), ensure_ascii=False)

    print(f"Repository generated with {len(index_entries)} extensions in {repo_out}")
    for entry in index_entries:
        print(f" - {entry['name']} ({entry['pkg']}) v{entry['version']}")

if __name__ == "__main__":
    main()
