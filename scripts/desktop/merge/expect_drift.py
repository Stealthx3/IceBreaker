import re
import sys
from pathlib import Path

COMMON = Path("shared/src/commonMain/kotlin")
ANDROID = Path("shared/src/androidMain/kotlin")
MEMBER = re.compile(r"^\s{4}(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:actual|override|open|suspend|inline|operator|internal|const|lateinit)\s+)*(fun|val|var)\s+(?:<[^>]+>\s+)?(?:[\w.<>?, ]+\.)?(`?\w+`?)")


def members(text, cls):
    m = re.search(r"^(?:@\w+\s+)*(?:expect |actual )?(?:data |sealed |abstract |open )?(class|object|interface) " + cls + r"\b", text, re.M)
    if not m:
        return None
    i = m.end()
    paren = 0
    while i < len(text):
        c = text[i]
        if c == "(":
            paren += 1
        elif c == ")":
            paren -= 1
        elif c == "{" and paren == 0:
            break
        elif c == "\n" and paren == 0 and text[i + 1:i + 2] not in (" ", ")", ":"):
            return set()
        i += 1
    i += 1
    depth = 1
    body = []
    line = ""
    while i < len(text) and depth:
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
        if depth == 1:
            line += c
            if c == "\n":
                body.append(line)
                line = ""
        elif depth > 1 and c == "\n" and line:
            body.append(line)
            line = ""
        i += 1
    names = set()
    for l in body:
        if re.match(r"^\s{4}private ", l):
            continue
        mm = MEMBER.match(l)
        if mm:
            names.add(mm.group(2))
    return names


only = set(sys.argv[1:])
for exp in sorted(COMMON.rglob("*.kt")):
    t = exp.read_text(encoding="utf-8")
    for m in re.finditer(r"^expect (?:\w+ )*(class|object) (\w+)", t, re.M):
        cls = m.group(2)
        if only and cls not in only:
            continue
        actual = None
        for a in ANDROID.rglob("*.kt"):
            at = a.read_text(encoding="utf-8")
            if re.search(r"^(?:@\w+\s+)*actual (?:\w+ )*(class|object) " + cls + r"\b", at, re.M):
                actual = (a, at)
                break
        if not actual:
            continue
        em = members(t, cls) or set()
        am = members(actual[1], cls) or set()
        missing = sorted(am - em)
        if missing:
            print(f"{cls} ({exp.relative_to(COMMON)}): {' '.join(missing)}")
