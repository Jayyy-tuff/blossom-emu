#!/usr/bin/env python3
# SPDX-FileCopyrightText: Copyright 2026 Blossom Emu contributors
# SPDX-License-Identifier: GPL-3.0-or-later
"""Turn Azahar's Android *application* sources into sources for an Android *library*.

Azahar is kept as an untouched upstream checkout. This script copies its Kotlin
sources, resources and manifest into the Blossom 3DS library module and applies
the small, mechanical changes needed for it to live inside the Blossom app next
to the Switch core:

  1. Every resource whose (type, name) also exists in the Switch app is renamed
     with a prefix, so the app's resources cannot silently override the 3DS
     core's layouts, strings or drawables when the two are merged.
  2. The default SharedPreferences file is replaced by a dedicated one, so the
     two cores never share preference keys.
  3. The manifest loses its <application> attributes and launcher entry.
  4. CitraApplication gains an entry point so the host app can initialise it.

Usage: prepare.py --azahar DIR --host-res DIR --out DIR
"""
import argparse
import collections
import os
import re
import shutil
import sys
import xml.etree.ElementTree as ET

PREFIX = "c3ds_"
PREFS_NAME = "blossom_3ds_preferences"
VALUE_TAGS = {
    "string": "string", "color": "color", "dimen": "dimen", "bool": "bool",
    "integer": "integer", "string-array": "array", "integer-array": "array",
    "array": "array", "plurals": "plurals", "style": "style", "fraction": "fraction",
}
SKIP_TYPES = {"id", "attr", "styleable"}


def scan(res):
    """Return {type: {names}} for every resource under a res directory."""
    out = collections.defaultdict(set)
    for d in sorted(os.listdir(res)):
        p = os.path.join(res, d)
        if not os.path.isdir(p):
            continue
        rtype = d.split("-")[0]
        for f in sorted(os.listdir(p)):
            if rtype != "values":
                out[rtype].add(f.split(".")[0])
                continue
            if not f.endswith(".xml"):
                continue
            for el in ET.parse(os.path.join(p, f)).getroot():
                name = el.get("name")
                if not name:
                    continue
                tag = el.tag
                if tag == "item":
                    tag = el.get("type", "item")
                if tag == "declare-styleable":
                    out["styleable"].add(name)
                    for a in el:
                        if a.get("name") and ":" not in a.get("name"):
                            out["attr"].add(a.get("name"))
                    continue
                out[VALUE_TAGS.get(tag, tag)].add(name)
    return out


def camel(name):
    return "".join(part[:1].upper() + part[1:] for part in name.split("_"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--azahar", required=True)
    ap.add_argument("--host-res", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    src = os.path.join(args.azahar, "src/android/app/src/main")
    out = args.out
    if os.path.exists(out):
        shutil.rmtree(out)
    os.makedirs(out)
    shutil.copytree(os.path.join(src, "java"), os.path.join(out, "java"))
    shutil.copytree(os.path.join(src, "res"), os.path.join(out, "res"))
    shutil.copy(os.path.join(src, "AndroidManifest.xml"), os.path.join(out, "AndroidManifest.xml"))
    res = os.path.join(out, "res")
    props = os.path.join(res, "resources.properties")
    if os.path.exists(props):
        os.remove(props)

    host, core = scan(args.host_res), scan(res)
    clash = {t: host[t] & core[t] for t in core if t in host and host[t] & core[t]}
    for t in SKIP_TYPES:
        if t in clash and t != "id":
            sys.exit(f"Unhandled {t} collisions: {sorted(clash[t])}")
        clash.pop(t, None)
    total = sum(len(v) for v in clash.values())
    print(f"Renaming {total} colliding resources: " + ", ".join(f"{t}={len(v)}" for t, v in sorted(clash.items())))

    def hit(rtype, name):
        return name in clash.get(rtype, ())

    # 1a. Rename resource files.
    for d in sorted(os.listdir(res)):
        rtype = d.split("-")[0]
        if rtype == "values":
            continue
        for f in sorted(os.listdir(os.path.join(res, d))):
            if hit(rtype, f.split(".")[0]):
                os.rename(os.path.join(res, d, f), os.path.join(res, d, PREFIX + f))

    # 1b. Rewrite definitions and references in XML.
    ref_re = re.compile(r"@(\+?)([a-z]+)/([A-Za-z0-9_.]+)")
    def_re = re.compile(r"(<([a-z-]+)\b[^<>]*?\bname=\")([^\"]+)(\"[^<>]*>)")

    def fix_ref(m):
        return f"@{m.group(1)}{m.group(2)}/{PREFIX}{m.group(3)}" if hit(m.group(2), m.group(3)) else m.group(0)

    def fix_def(m):
        tag, name = m.group(2), m.group(3)
        if tag == "item":
            t = re.search(r"\btype=\"([a-z]+)\"", m.group(0))
            rtype = t.group(1) if t else None
        else:
            rtype = VALUE_TAGS.get(tag)
        if rtype and hit(rtype, name):
            return m.group(1) + PREFIX + name + m.group(4)
        return m.group(0)

    xml_files = [os.path.join(out, "AndroidManifest.xml")]
    for root, _, files in os.walk(res):
        xml_files += [os.path.join(root, f) for f in files if f.endswith(".xml")]
    for path in xml_files:
        text = open(path, encoding="utf-8").read()
        # Upstream has a file with a second XML declaration after the root element.
        # The app build tolerates it; the stricter library resource parser does not.
        new = text[:5] + re.sub(r"<\?xml[^>]*\?>", "", text[5:])
        new = ref_re.sub(fix_ref, new)
        if os.path.basename(os.path.dirname(path)).split("-")[0] == "values":
            new = def_re.sub(fix_def, new)
        if new != text:
            open(path, "w", encoding="utf-8").write(new)

    for path in xml_files:
        try:
            ET.parse(path)
        except ET.ParseError as e:
            sys.exit(f"Malformed XML after preparation: {path}: {e}")

    # 1c. Rewrite references in Kotlin, including view-binding class names.
    r_re = re.compile(r"\bR\.([a-z]+)\.([A-Za-z0-9_]+)")
    bindings = {camel(n) + "Binding": camel(PREFIX + n) + "Binding" for n in clash.get("layout", ())}
    bind_re = re.compile(r"\b(" + "|".join(map(re.escape, sorted(bindings))) + r")\b") if bindings else None
    prefs_re = re.compile(r"PreferenceManager\s*\.\s*getDefaultSharedPreferences\(\s*([^()]*?)\s*\)")

    def fix_r(m):
        return f"R.{m.group(1)}.{PREFIX}{m.group(2)}" if hit(m.group(1), m.group(2)) else m.group(0)

    prefs_hits = 0
    for root, _, files in os.walk(os.path.join(out, "java")):
        for f in files:
            if not f.endswith(".kt"):
                continue
            path = os.path.join(root, f)
            text = open(path, encoding="utf-8").read()
            new = r_re.sub(fix_r, text)
            if bind_re:
                new = bind_re.sub(lambda m: bindings[m.group(1)], new)
            # 2. Dedicated preferences file.
            new, n = prefs_re.subn(lambda m: f'{m.group(1)}.getSharedPreferences("{PREFS_NAME}", 0)', new)
            prefs_hits += n
            if new != text:
                open(path, "w", encoding="utf-8").write(new)
    print(f"Redirected {prefs_hits} default-preferences lookups to {PREFS_NAME}")
    left = [p for p, _, fs in os.walk(os.path.join(out, "java")) for f in fs
            if "getDefaultSharedPreferences" in open(os.path.join(p, f), encoding="utf-8").read()]
    if left:
        sys.exit(f"Unconverted getDefaultSharedPreferences calls remain in: {left}")

    # 3. Manifest: library-safe <application>, no launcher entry.
    path = os.path.join(out, "AndroidManifest.xml")
    text = open(path, encoding="utf-8").read()
    text, n1 = re.subn(r"<application\b[^>]*>", "<application>", text, count=1)
    text, n2 = re.subn(r"\s*<meta-data android:name=\"android\.game_mode_config\"[^>]*/>", "", text)
    text, n3 = re.subn(r"\s*(<!--[^>]*-->\s*)?<intent-filter>\s*<action android:name=\"android\.intent\.action\.MAIN\"/>.*?</intent-filter>",
                       "", text, count=1, flags=re.S)
    text, n4 = re.subn(r"(android:name=\"org\.citra\.citra_emu\.ui\.main\.MainActivity\")",
                       r'\1\n            android:label="Blossom 3DS"', text, count=1)
    if (n1, n2, n3, n4) != (1, 1, 1, 1):
        sys.exit(f"Manifest patch did not apply cleanly: {(n1, n2, n3, n4)}")
    open(path, "w", encoding="utf-8").write(text)

    # 4. Host entry point on CitraApplication.
    path = os.path.join(out, "java/org/citra/citra_emu/CitraApplication.kt")
    text = open(path, encoding="utf-8").read()
    anchor = "    override fun onCreate() {"
    if text.count(anchor) != 1:
        sys.exit("CitraApplication.onCreate anchor not found")
    hook = (
        "    /**\n"
        "     * Blossom: the host app owns the real Application object, so it creates this\n"
        "     * class itself and attaches the host context before running the normal start-up.\n"
        "     */\n"
        "    fun blossomAttach(base: Context) {\n"
        "        attachBaseContext(base)\n"
        "        onCreate()\n"
        "    }\n\n"
    )
    open(path, "w", encoding="utf-8").write(text.replace(anchor, hook + anchor))
    print("Prepared 3DS core sources in", out)


if __name__ == "__main__":
    main()
