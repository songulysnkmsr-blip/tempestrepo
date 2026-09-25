#!/usr/bin/env python3
"""
Tachimanga / Tachiyomi Extension Repository Index Generator.
Generates:
  - repo/jar/tachiyomi-tr.<slug>-v<ver>.jar  (iOS fast-path, bypasses dex2jar)
  - repo/apk/tachiyomi-tr.<slug>-v<ver>.apk  (Android / fallback)
  - repo/icon/<pkg>.png
  - repo/index.pb   (gzip-compressed protobuf with jarUrl, for Tachimanga iOS)
  - repo/index.json (human-readable JSON mirror of index.pb)
  - repo/index.min.json (legacy flat format for older Tachiyomi forks)
  - repo/repo.json  (points to index_v2 = index.pb)
"""

import os
import sys
import json
import gzip
import shutil
import hashlib
import struct
import re
import subprocess
import zipfile
from pathlib import Path

# ─── Protobuf bindings (tools/index_pb2.py) ────────────────────────────────
sys.path.insert(0, str(Path(__file__).resolve().parent))
try:
    import index_pb2
    from google.protobuf import json_format
    HAS_PROTO = True
except ImportError:
    HAS_PROTO = False
    print("[WARN] protobuf not installed – index.pb will NOT be generated.")
    print("       Run: pip install protobuf")

# ─── Helpers ────────────────────────────────────────────────────────────────

FIXED_JAR_TIME = (2024, 1, 6, 0, 0, 0)  # Keiyoushi birthday (for reproducibility)

def sha256_file(filepath: Path) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def generate_source_id(name: str, lang: str, version_id: int = 1) -> int:
    key = f"{name.lower()}/{lang}/{version_id}".encode("utf-8")
    md5 = hashlib.md5(key).digest()
    val = struct.unpack(">Q", md5[:8])[0] & 0x7FFFFFFFFFFFFFFF
    return val

def get_signing_fp(p12_path: Path, password: str = "tempestpass") -> str:
    default = "01e252e0645243a25e8726a236f2305c00fab708a98384727fbb1cfc840a960f"
    if not p12_path.exists():
        return default
    try:
        res = subprocess.run(
            ["openssl", "pkcs12", "-in", str(p12_path), "-nokeys", "-nodes", "-passin", f"pass:{password}"],
            capture_output=True, text=True
        )
        res2 = subprocess.run(
            ["openssl", "x509", "-outform", "DER"],
            input=res.stdout.encode(), capture_output=True
        )
        if res2.stdout:
            return hashlib.sha256(res2.stdout).hexdigest()
    except Exception:
        pass
    return default

# ─── Extension metadata ─────────────────────────────────────────────────────

EXTENSIONS_META = {
    "juratempest": {
        "name": "Jura Tempest",
        "lang": "tr",
        "baseUrl": "https://juratempe.st",
        "pkgSuffix": "tr.juratempest",
        "nsfw": 0,
        "class": "eu.kanade.tachiyomi.extension.tr.juratempest.JuraTempest",
    },
    "mangtto": {
        "name": "Mangtto",
        "lang": "tr",
        "baseUrl": "https://mangtto.com",
        "pkgSuffix": "tr.mangtto",
        "nsfw": 0,
        "class": "eu.kanade.tachiyomi.extension.tr.mangtto.Mangtto",
    },
    "orimanga": {
        "name": "Ori Manga",
        "lang": "tr",
        "baseUrl": "https://orimanga.net",
        "pkgSuffix": "tr.orimanga",
        "nsfw": 0,
        "class": "eu.kanade.tachiyomi.extension.tr.orimanga.OriManga",
    },
    "golgebahcesi": {
        "name": "Gölge Bahçesi",
        "lang": "tr",
        "baseUrl": "https://golgebahcesi.com",
        "pkgSuffix": "tr.golgebahcesi",
        "nsfw": 0,
        "class": "eu.kanade.tachiyomi.extension.tr.golgebahcesi.GolgeBahcesi",
    },
}

RAW_BASE = "https://raw.githubusercontent.com/songulysnkmsr-blip/tempestrepo/repo"

# ─── JAR builder ─────────────────────────────────────────────────────────────

def build_jar_from_apk_and_classes(
    slug: str,
    meta: dict,
    apk_path: Path,
    class_dirs: list[Path],
    version_name: str,
    version_code: int,
    jar_out: Path,
) -> bool:
    """
    Create a .jar that Tachimanga iOS can load directly (no dex2jar).

    Structure mirrors Keiyoushi's signed jars:
      AndroidManifest.xml  – plain-text XML with fully-qualified class name
      eu/kanade/.../Foo.class  – clean kotlinc bytecode
      res/…                – from APK
      resources.arsc       – from APK
    """
    pkg = f"eu.kanade.tachiyomi.extension.{meta['pkgSuffix']}"
    fqn = meta["class"]           # fully-qualified class name
    lib_version = "1.4"

    manifest_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="{pkg}"
    android:versionCode="{version_code}"
    android:versionName="{version_name}" >

    <uses-sdk
        android:minSdkVersion="26"
        android:targetSdkVersion="34" />

    <uses-feature android:name="tachiyomi.extension" />

    <application
        android:allowBackup="false"
        android:extractNativeLibs="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Tachiyomi: {meta['name']}" >
        <meta-data
            android:name="tachiyomix.name"
            android:value="{meta['name']}" />
        <meta-data
            android:name="tachiyomi.extension.class"
            android:value="{fqn}" />
        <meta-data
            android:name="tachiyomi.extension.version"
            android:value="{version_name}" />
        <meta-data
            android:name="tachiyomi.extension.nsfw"
            android:value="{meta['nsfw']}" />
        <meta-data
            android:name="tachiyomix.contentWarning"
            android:value="{meta['nsfw']}" />
        <meta-data
            android:name="tachiyomix.extensionLib"
            android:value="{lib_version}" />
    </application>

</manifest>"""

    jar_out.parent.mkdir(parents=True, exist_ok=True)

    written = set()

    with zipfile.ZipFile(jar_out, "w", compression=zipfile.ZIP_DEFLATED) as jf:
        # 1. AndroidManifest.xml (plain text)
        zi = zipfile.ZipInfo("AndroidManifest.xml")
        zi.date_time = FIXED_JAR_TIME
        jf.writestr(zi, manifest_xml.encode("utf-8"))
        written.add("AndroidManifest.xml")

        # 2. .class files from kotlinc output directories
        for class_dir in class_dirs:
            if not class_dir.is_dir():
                continue
            for cls_file in sorted(class_dir.rglob("*.class")):
                rel = cls_file.relative_to(class_dir).as_posix()
                if rel not in written:
                    zi = zipfile.ZipInfo(rel)
                    zi.date_time = FIXED_JAR_TIME
                    zi.compress_type = zipfile.ZIP_DEFLATED
                    jf.writestr(zi, cls_file.read_bytes())
                    written.add(rel)

        # 3. res/ and resources.arsc from APK
        if apk_path and apk_path.exists():
            with zipfile.ZipFile(apk_path, "r") as apk:
                for entry in sorted(apk.infolist(), key=lambda e: e.filename):
                    name = entry.filename
                    if entry.is_dir():
                        continue
                    if (name.startswith("res/") or name.startswith("assets/") or name == "resources.arsc"):
                        if name not in written:
                            zi = zipfile.ZipInfo(name)
                            zi.date_time = FIXED_JAR_TIME
                            zi.compress_type = zipfile.ZIP_DEFLATED
                            jf.writestr(zi, apk.read(name))
                            written.add(name)

    has_classes = any(n.endswith(".class") for n in written)
    if not has_classes:
        print(f"  [WARN] JAR for '{slug}' has NO .class files – iOS install will fail!")
        return False

    print(f"  [JAR] {jar_out.name} ({jar_out.stat().st_size // 1024} KB, {sum(1 for n in written if n.endswith('.class'))} classes)")
    return True

# ─── Main ─────────────────────────────────────────────────────────────────────

def main():
    root_dir = Path(__file__).resolve().parent.parent
    repo_out = root_dir / "repo"
    apk_out  = repo_out / "apk"
    jar_out  = repo_out / "jar"
    icon_out = repo_out / "icon"

    for d in [repo_out, apk_out, jar_out, icon_out]:
        d.mkdir(parents=True, exist_ok=True)

    signing_fp = get_signing_fp(root_dir / "signingkey.p12")

    proto_extensions = []   # for index.pb
    index_entries    = []   # for index.min.json (legacy)

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
            "class": f"eu.kanade.tachiyomi.extension.tr.{slug}.{slug.capitalize()}",
        })

        # ── Read version from build.gradle.kts ──────────────────────────────
        build_gradle = ext_dir / "build.gradle.kts"
        ext_ver_code = 140004
        version_name = "1.4.4"
        if build_gradle.exists():
            bg = build_gradle.read_text(encoding="utf-8")
            m = re.search(r'versionName\s*=\s*"([^"]+)"', bg)
            if m:
                version_name = m.group(1)
            m = re.search(r'versionCode\s*=\s*([0-9]+)', bg)
            if m:
                ext_ver_code = int(m.group(1))

        pkg          = f"eu.kanade.tachiyomi.extension.{meta['pkgSuffix']}"
        apk_filename = f"tachiyomi-{meta['pkgSuffix']}-v{version_name}.apk"
        jar_filename = f"tachiyomi-{meta['pkgSuffix']}-v{version_name}.jar"

        # ── Copy icon ────────────────────────────────────────────────────────
        for icon_rel in ["res/mipmap-xxhdpi/ic_launcher.png", "res/mipmap-xhdpi/ic_launcher.png"]:
            src_icon = ext_dir / icon_rel
            if src_icon.exists():
                shutil.copy2(src_icon, icon_out / f"{pkg}.png")
                break

        # ── Locate built APK ─────────────────────────────────────────────────
        found_apk = None
        release_dir = ext_dir / "build" / "outputs" / "apk" / "release"
        if release_dir.exists():
            for c in sorted(release_dir.glob("*.apk")):
                if not c.name.endswith("-unaligned.apk"):
                    found_apk = c
                    break
        if not found_apk and (apk_out / apk_filename).exists():
            found_apk = apk_out / apk_filename

        if found_apk and found_apk != apk_out / apk_filename:
            shutil.copy2(found_apk, apk_out / apk_filename)
        apk_dest = apk_out / apk_filename
        sha256 = sha256_file(apk_dest) if apk_dest.exists() else ""

        # ── Locate Kotlin .class dirs ────────────────────────────────────────
        # Gradle puts kotlinc output in build/tmp/kotlin-classes/<variant>/
        class_dirs = []
        for candidate in [
            ext_dir / "build" / "tmp" / "kotlin-classes" / "release",
            ext_dir / "build" / "intermediates" / "javac" / "release" / "classes",
            ext_dir / "build" / "intermediates" / "compile_app_classes_jar" / "release",
        ]:
            if candidate.is_dir():
                class_dirs.append(candidate)

        # ── Build JAR ────────────────────────────────────────────────────────
        jar_dest = jar_out / jar_filename
        jar_ok = build_jar_from_apk_and_classes(
            slug=slug,
            meta=meta,
            apk_path=apk_dest if apk_dest.exists() else None,
            class_dirs=class_dirs,
            version_name=version_name,
            version_code=ext_ver_code,
            jar_out=jar_dest,
        )

        # ── URLs ─────────────────────────────────────────────────────────────
        apk_url  = f"{RAW_BASE}/apk/{apk_filename}"
        jar_url  = f"{RAW_BASE}/jar/{jar_filename}"
        icon_url = f"{RAW_BASE}/icon/{pkg}.png"

        # ── Proto entry ──────────────────────────────────────────────────────
        if HAS_PROTO:
            proto_ext = index_pb2.Extension()
            proto_ext.name        = meta["name"]
            proto_ext.packageName = pkg
            proto_ext.resources.apkUrl  = apk_url
            proto_ext.resources.iconUrl = icon_url
            if jar_ok:
                proto_ext.resources.jarUrl = jar_url
            proto_ext.extensionLib = "1.4"
            proto_ext.versionCode  = ext_ver_code
            proto_ext.versionName  = version_name
            proto_ext.contentWarning = index_pb2.CONTENT_WARNING_SAFE

            src_proto = proto_ext.sources.add()
            src_proto.id       = generate_source_id(meta["name"], meta["lang"])
            src_proto.name     = meta["name"]
            src_proto.language = meta["lang"]
            src_proto.homeUrl  = meta["baseUrl"]

            proto_extensions.append(proto_ext)

        # ── Legacy index entry ───────────────────────────────────────────────
        source_id_str = str(generate_source_id(meta["name"], meta["lang"]))
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
            "sources": [{
                "name": meta["name"],
                "lang": meta["lang"],
                "id": source_id_str,
                "baseUrl": meta["baseUrl"],
            }],
        }
        if sha256:
            entry["sha256"] = sha256
        if jar_ok:
            entry["jarUrl"] = jar_url
        index_entries.append(entry)

    # ── Write index.pb (gzip-compressed protobuf with jarUrl) ────────────────
    index_pb_path = repo_out / "index.pb"
    index_json_path = repo_out / "index.json"
    if HAS_PROTO and proto_extensions:
        proto_index = index_pb2.Index()
        proto_index.name       = "Tempest Repo"
        proto_index.badgeLabel = "TR"
        proto_index.signingKey = signing_fp
        proto_index.contact.website = "https://github.com/songulysnkmsr-blip/tempestrepo"
        for ext in proto_extensions:
            proto_index.extensionList.extensions.append(ext)

        with open(index_pb_path, "wb") as f:
            f.write(gzip.compress(proto_index.SerializeToString(deterministic=True), mtime=0))
        print(f"[index.pb] Written ({index_pb_path.stat().st_size} bytes gzipped)")

        with open(index_json_path, "w", encoding="utf-8") as f:
            f.write(json_format.MessageToJson(
                proto_index,
                always_print_fields_with_no_presence=False,
                preserving_proto_field_name=True,
                indent=2,
            ))
        print(f"[index.json] Written")
    else:
        print("[WARN] Skipping index.pb generation (no protobuf or no extensions)")

    # ── Write index.min.json (legacy) ─────────────────────────────────────────
    with open(repo_out / "index.min.json", "w", encoding="utf-8") as f:
        json.dump(index_entries, f, separators=(",", ":"), ensure_ascii=False)
    print(f"[index.min.json] Written ({len(index_entries)} extensions)")

    # ── Write repo.json (with index_v2 pointing to index.pb) ─────────────────
    repo_json = {
        "index_v2": f"{RAW_BASE}/index.pb",
        "meta": {
            "name": "Tempest Repo",
            "shortName": "TR",
            "website": "https://github.com/songulysnkmsr-blip/tempestrepo",
            "signingKeyFingerprint": signing_fp,
        },
    }
    with open(repo_out / "repo.json", "w", encoding="utf-8") as f:
        json.dump(repo_json, f, indent=2, ensure_ascii=False)
    print(f"[repo.json] Written (index_v2={repo_json['index_v2']})")

    print(f"\nRepository generated with {len(index_entries)} extensions in {repo_out}")
    for e in index_entries:
        jar_info = "✓ jar" if "jarUrl" in e else "✗ jar MISSING"
        print(f"  - {e['name']} ({e['pkg']}) v{e['version']}  [{jar_info}]")

if __name__ == "__main__":
    main()
