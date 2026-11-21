"""
Which repository a shell command's `git push` or `gh pr create` acts on, so the project's push and
PR guards check only this project. A command counts as targeting another repository only when that
is certain: an explicit `gh --repo`/`-R` naming another GitHub repository, or a push run (after
`cd <dir>`, through `git -C <dir>`, or from the hook's working directory) inside a git repository
that does not share this project's git directory. Worktrees of this project share it and still
count as this project. Anything unclear counts as this project.
"""
import os
import re
import subprocess

from git_command import command_words, split_segments


def _git(args, cwd):
    try:
        r = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True, timeout=15)
    except Exception:
        return None
    return r.stdout.strip() if r.returncode == 0 else None


def _common_dir(path):
    out = _git(["rev-parse", "--path-format=absolute", "--git-common-dir"], path)
    return os.path.realpath(out) if out else None


def _github_slug(url):
    m = re.search(r"github\.com[:/]([^/]+/[^/]+?)(?:\.git)?/?$", url or "")
    return m.group(1).lower() if m else None


def _expand(path, base):
    path = os.path.expanduser(os.path.expandvars(path))
    return os.path.normpath(path if os.path.isabs(path) else os.path.join(base, path))


def _invocation_dirs(command, start):
    """(program words, directory it runs in) for each simple command, following `cd`."""
    cwd = start
    found = []
    for segment in split_segments(command):
        words = command_words(segment)
        if not words:
            continue
        if words[0] == "cd" and len(words) > 1:
            cwd = _expand(words[1], cwd)
            continue
        here = cwd
        if words[0] == "git" and "-C" in words[1:]:
            i = words.index("-C", 1)
            if i + 1 < len(words):
                here = _expand(words[i + 1], cwd)
        found.append((words, here))
    return found


def targets_other_repository(payload, command, root, program):
    """True when every `program` ("git" push or "gh" pr create) call in `command` is certainly for another repository."""
    start = payload.get("cwd") or os.getcwd()
    project_dir = _common_dir(root)
    project_slug = _github_slug(_git(["remote", "get-url", "origin"], root))
    calls = [(w, d) for w, d in _invocation_dirs(command, start) if w and w[0] == program]
    if not calls or project_dir is None:
        return False
    for words, here in calls:
        if program == "gh":
            repo = None
            for opt in ("--repo", "-R"):
                if opt in words:
                    i = words.index(opt)
                    repo = words[i + 1].lower() if i + 1 < len(words) else None
            if repo and project_slug and repo != project_slug:
                continue
            if repo is None and _common_dir(here) not in (None, project_dir):
                continue
            return False
        other = _common_dir(here)
        if other is None or other == project_dir:
            return False
    return True
