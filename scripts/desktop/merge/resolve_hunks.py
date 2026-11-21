import re
import sys

mode = sys.argv[1]
files = sys.argv[2:]


def is_import_block(lines):
    return all(re.match(r"^\s*(import |$)", x) for x in lines)


def resolve(path):
    text = open(path, encoding="utf-8").read()
    out = []
    cur = None
    left = 0
    changed = 0
    for line in text.split("\n"):
        if line.startswith("<<<<<<< "):
            cur = {"ours": [], "base": [], "theirs": [], "sec": "ours", "raw": [line]}
            continue
        if cur is not None:
            cur["raw"].append(line)
            if line.startswith("||||||| "):
                cur["sec"] = "base"
                continue
            if line.startswith("=======") and cur["sec"] in ("ours", "base"):
                cur["sec"] = "theirs"
                continue
            if line.startswith(">>>>>>> "):
                ours, theirs = cur["ours"], cur["theirs"]
                if is_import_block(ours) and is_import_block(theirs):
                    merged = [x for x in ours]
                    for x in theirs:
                        if x.strip() and x not in merged:
                            merged.append(x)
                    out.extend(merged)
                    changed += 1
                elif mode == "theirs":
                    out.extend(theirs)
                    changed += 1
                else:
                    out.extend(cur["raw"])
                    left += 1
                cur = None
                continue
            cur[cur["sec"]].append(line)
            continue
        out.append(line)
    open(path, "w", encoding="utf-8").write("\n".join(out))
    return changed, left


for f in files:
    c, l = resolve(f)
    print(f"{f}\tresolved={c}\tleft={l}")
