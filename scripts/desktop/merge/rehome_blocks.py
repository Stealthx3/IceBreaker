import re
import sys
from pathlib import Path

C = Path("shared/src/commonMain/kotlin/com/nendo/argosy")
A = Path("shared/src/androidMain/kotlin/com/nendo/argosy")


def block_span(text, name):
    m = re.search(r"^(?:@\w+(?:\([^)]*\))?\s*\n?)*(?:public |internal )?(?:data |sealed |enum |abstract |open |value )?(?:class|interface|object) " + re.escape(name) + r"\b", text, re.M)
    if not m:
        return None
    start = m.start()
    lines_before = text[:start].split("\n")
    k = len(lines_before) - 1
    doc_start = start
    if k >= 1 and lines_before[k - 1].strip().endswith("*/"):
        j = k - 1
        while j >= 0 and not lines_before[j].strip().startswith("/**"):
            j -= 1
        doc_start = len("\n".join(lines_before[:j])) + (1 if j > 0 else 0)
    i = m.end()
    depth = 0
    while i < len(text):
        c = text[i]
        if c in "({":
            depth += 1
        elif c in ")}":
            depth -= 1
        elif c == "\n" and depth == 0:
            rest = text[i + 1:]
            stripped = rest.lstrip(" ")
            prev = text[:i].rstrip()
            if not (prev.endswith((",", "=", "(", ":")) or stripped.startswith((":", "{", ")", "."))):
                break
        i += 1
    return doc_start, i


def rehome(android_rel, common_rel, names):
    ap = A / android_rel
    cp = C / common_rel
    at = ap.read_text(encoding="utf-8")
    ct = cp.read_text(encoding="utf-8")
    for n in names:
        a_span = block_span(at, n)
        c_span = block_span(ct, n)
        assert a_span, (android_rel, n)
        block = at[a_span[0]:a_span[1]].strip("\n")
        if c_span:
            ct = ct[:c_span[0]] + block + ct[c_span[1]:]
        else:
            ct = ct.rstrip("\n") + "\n\n" + block + "\n"
        at = at[:a_span[0]].rstrip("\n") + "\n\n" + at[a_span[1]:].lstrip("\n")
        print(f"rehomed {n}: {android_rel} -> {common_rel}")
    ap.write_text(re.sub(r"\n{3,}", "\n\n", at), encoding="utf-8")
    cp.write_text(re.sub(r"\n{3,}", "\n\n", ct), encoding="utf-8")


rehome("data/emulator/GameLauncher.kt", "data/emulator/LaunchResult.kt", ["DiscOption", "LaunchResult"])
rehome("data/installer/ManagedInstallerManager.kt", "data/installer/InstallerJobs.kt", ["InstallerJob", "InstallerJobState"])
rehome("data/update/ApkInstallManager.kt", "data/update/ApkInstallTypes.kt", ["ApkInstallState"])
rehome("hardware/CompanionPanel.kt", "hardware/CompanionInGameState.kt", ["CompanionInGameState", "CompanionPanel"])
rehome("ui/input/HapticFeedback.kt", "ui/input/HapticPatterns.kt", ["HapticPattern"])
