import re
from pathlib import Path

COMMON = Path("shared/src/commonMain/kotlin")
ANDROID = Path("shared/src/androidMain/kotlin")
DECL = re.compile(r"^\s{4,8}(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:override|open|suspend|inline|operator|internal|const|lateinit)\s+)*(fun|val|var)\s+(?:<[^>]+>\s+)?(?:[\w.<>?, ]+\.)?(`?\w+`?)", re.M)

actual_text = {}
for a in ANDROID.rglob("*.kt"):
    actual_text[a] = a.read_text(encoding="utf-8")

for exp in sorted(COMMON.rglob("*.kt")):
    t = exp.read_text(encoding="utf-8")
    for m in re.finditer(r"^expect (?:\w+ )*(class|object|interface) (\w+)", t, re.M):
        cls = m.group(2)
        holder = None
        for a, at in actual_text.items():
            if re.search(r"^(?:@\w+\s+)*actual (?:\w+ )*(?:class|object|interface|typealias) " + cls + r"\b", at, re.M):
                holder = (a, at)
                break
        if not holder or re.search(r"actual typealias " + cls + r"\b", holder[1]):
            continue
        start = m.end()
        nxt = re.search(r"^(?:expect|fun|val|class|object|enum|data|internal|private)\b", t[start:], re.M)
        body = t[start:start + nxt.start()] if nxt else t[start:]
        names = {x.group(2) for x in DECL.finditer(body)}
        missing = sorted(n for n in names if not re.search(r"actual\s+(?:\w+\s+)*(?:fun|val|var)\s+(?:<[^>]+>\s+)?(?:[\w.<>?, ]+\.)?" + re.escape(n) + r"\b", holder[1]))
        if missing:
            print(f"{cls} ({holder[0].relative_to(ANDROID)}): {' '.join(missing)}")
