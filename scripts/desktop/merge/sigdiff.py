import re
import sys
from pathlib import Path

COMMON = Path("shared/src/commonMain/kotlin")
ANDROID = Path("shared/src/androidMain/kotlin")


def header(text, name):
    m = re.search(r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*((?:(?:actual|override|internal|suspend|const|open|inline)\s+)*(?:fun|val|var)\s+(?:<[^>]+>\s+)?(?:[\w.<>?]+\.)?" + re.escape(name) + r"\b)", text, re.M)
    if not m:
        return None
    i = m.start(1)
    depth = 0
    j = i
    while j < len(text):
        c = text[j]
        if c in "(<":
            depth += 1
        elif c in ")>":
            depth -= 1
        elif depth == 0 and (c in "{=\n" and not (c == "\n" and text[j - 1] in ",(")):
            break
        j += 1
    return re.sub(r"\s+", " ", text[i:j]).strip()


for line in open(sys.argv[1]):
    m = re.match(r"(\w+) \(([^)]+)\): (.*)", line.strip())
    cls, f, names = m.groups()
    at = (ANDROID / f).read_text(encoding="utf-8")
    exp = None
    for e in COMMON.rglob("*.kt"):
        et = e.read_text(encoding="utf-8")
        if re.search(r"^expect (?:\w+ )*(?:class|object) " + cls + r"\b", et, re.M):
            exp = et
            break
    for n in names.split():
        a = header(at, n)
        e = header(exp, n) if exp else None
        if a is None:
            print(f"{cls}.{n}: REMOVED upstream | expect: {e}")
        else:
            a2 = re.sub(r"^actual ", "", a)
            print(f"{cls}.{n}:\n   expect:  {e}\n   android: {a2}")
