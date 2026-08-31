#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PR 完整性检查

规则（see ADR-0004）：
1. 改动必有测试：PR 修改了 src/main 下的 .java 业务文件，必须同时存在对应的 *Test.java；
2. 无未完成标记：变更的 .java 文件不得包含 TODO / FIXME / XXX；
3. PR 描述：若有 PR 描述，"PR 类型"必须且只能勾选 1 项，"自查清单"不得存在未勾选项。

本地调试：
  $env:PR_BODY='...'; $env:BASE_SHA='<sha>'; $env:HEAD_SHA='<sha>'; python scripts/check_pr_completeness.py
"""
import os
import re
import subprocess
import sys
from pathlib import Path

MISSING_MARKERS = ("TODO", "FIXME", "XXX")
MAIN_SRC = "src/main/java"
TEST_SRC = "src/test/java"
NO_TEST_SUFFIXES = ("Test.java", "package-info.java", "module-info.java")


def git(args):
    return subprocess.run(["git"] + args, capture_output=True, text=True)


def changed_files(base, head):
    """返回 base..head 间变更的文件名列表（相对仓库根）。"""
    r = git(["diff", "--name-only", "{}...{}".format(base, head)])
    if r.returncode != 0:
        r = git(["diff", "--name-only", base, head])
        if r.returncode != 0:
            return []
    return [ln.strip() for ln in r.stdout.splitlines() if ln.strip()]


def expected_test(path):
    """src/main/java/.../Foo.java -> src/test/java/.../FooTest.java"""
    module, rel = path.split(MAIN_SRC, 1)
    return module + TEST_SRC + rel[:-len(".java")] + "Test.java"


def check_files(files, errors):
    for f in files:
        if not f.endswith(".java"):
            continue
        # 改动必有测试
        if MAIN_SRC in f and not f.endswith(NO_TEST_SUFFIXES):
            test = expected_test(f)
            if not Path(test).exists():
                errors.append("业务文件 {} 缺少对应测试 {} see ADR-0004"
                              .format(f, test))
        # 未完成标记
        p = Path(f)
        if p.exists():
            for i, line in enumerate(p.read_text(encoding="utf-8",
                                                 errors="ignore").splitlines(), 1):
                hits = [m for m in MISSING_MARKERS if m in line]
                if hits:
                    errors.append("{}:{} 含未完成标记 {}"
                                  .format(f, i, hits))


def extract_section(body, heading):
    """返回 PR 描述中 `## 标题` 下的文本（到下一个 `##` 或结尾为止）。"""
    lines = body.splitlines()
    in_section = False
    section = []
    for line in lines:
        if line.strip().startswith("## "):
            if in_section:
                break
            if heading in line:
                in_section = True
            continue
        if in_section:
            section.append(line)
    return "\n".join(section)


def check_pr_body(errors):
    body = os.environ.get("PR_BODY", "") or ""
    if not body.strip():
        return  # push 事件无 PR 描述，跳过
    # 1) PR 类型：必须且只能勾选 1 项
    type_section = extract_section(body, "PR 类型")
    checked_types = re.findall(r"-\s*\[[xX]\]", type_section)
    if len(checked_types) != 1:
        errors.append("PR 类型必须且只能勾选 1 项（当前勾选 {} 项）".format(len(checked_types)))
    # 2) 自查清单：不得有未勾选项
    checklist = extract_section(body, "自查清单")
    unchecked = re.findall(r"-\s*\[\s*\]", checklist)
    if unchecked:
        errors.append("自查清单还有 {} 项未勾选".format(len(unchecked)))


def main():
    base = os.environ.get("BASE_SHA", "")
    head = os.environ.get("HEAD_SHA", "")
    errors = []
    files = changed_files(base, head) if base and head else []
    check_files(files, errors)
    check_pr_body(errors)
    if errors:
        print("[FAIL] completeness 检查失败：")
        for e in errors:
            print("  - " + e)
        sys.exit(1)
    print("[SUCCESS] completeness 检查通过, {}文件变更".format(len(files)))


if __name__ == "__main__":
    main()
