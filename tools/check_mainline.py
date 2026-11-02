"""Enforce the platform mainline and immutable release tags from the catalog."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import subprocess
import sys

DEVELOPMENT_PREFIXES = ('feat/', 'fix/', 'hotfix/')


def git(root, *args):
    result = subprocess.run(['git', *args], cwd=root, text=True, encoding='utf-8', capture_output=True)
    if result.returncode:
        raise RuntimeError('Git 检查失败：' + ' '.join(args[:2]))
    return result.stdout.strip()


def validate(root, *, release=False, pushed_refs=None):
    catalog = json.loads((root / 'deploy/release-catalog.json').read_text(encoding='utf-8'))
    mainline = catalog['mainline']
    formal = {mainline, 'dev', 'master', *catalog.get('historicalMainlines', [])}
    current = git(root, 'branch', '--show-current')
    if current != mainline:
        if release or not current.startswith(DEVELOPMENT_PREFIXES) or not current.split('/', 1)[1]:
            raise RuntimeError('正式发布必须在融合版主线 ' + mainline + '；开发可用 feat/fix/hotfix 分支。')
        git(root, 'merge-base', '--is-ancestor', mainline, 'HEAD')
    for line in pushed_refs or []:
        _, local_sha, remote_ref, remote_sha = line.split()
        deleted = set(local_sha) == {'0'}
        if remote_ref.startswith('refs/tags/'):
            if remote_ref.startswith(('refs/tags/' + mainline + '-', 'refs/tags/release-reservations/')):
                if deleted or set(remote_sha) != {'0'}:
                    raise RuntimeError('发布标签不可覆盖或删除')
                git(root, 'merge-base', '--is-ancestor', local_sha, mainline)
        if not remote_ref.startswith('refs/heads/'):
            continue
        branch = remote_ref.removeprefix('refs/heads/')
        if deleted:
            if branch in formal:
                raise RuntimeError('不得自动删除正式分支：' + branch)
            git(root, 'merge-base', '--is-ancestor', remote_sha, mainline)
        elif branch != mainline:
            if not branch.startswith(DEVELOPMENT_PREFIXES) or not branch.split('/', 1)[1]:
                raise RuntimeError('不得覆盖旧版本或推送无规范开发分支：' + branch)
            git(root, 'merge-base', '--is-ancestor', mainline, local_sha)
        elif set(remote_sha) != {'0'}:
            git(root, 'merge-base', '--is-ancestor', remote_sha, local_sha)
    commit = git(root, 'rev-parse', 'HEAD')
    if release:
        if git(root, 'status', '--porcelain'):
            raise RuntimeError('发布前请先提交本次修改，不能遗漏工作区代码。')
        if git(root, 'ls-remote', 'origin', 'refs/heads/' + mainline).split()[0] != commit:
            raise RuntimeError('发布前请先推送 ' + mainline + '，构建源码必须对应远端同一提交。')
    return {'branch': current, 'mainline': mainline, 'commit': commit,
            'tree': git(root, 'rev-parse', 'HEAD^{tree}'), 'releaseReady': release}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release', action='store_true')
    parser.add_argument('--commit', action='store_true')
    parser.add_argument('--push', action='store_true')
    args = parser.parse_args()
    try:
        result = validate(Path(__file__).resolve().parents[1], release=args.release,
                          pushed_refs=sys.stdin.read().splitlines() if args.push else None)
    except (RuntimeError, OSError, ValueError, IndexError) as error:
        parser.exit(1, str(error) + '\n')
    if not args.commit and not args.push:
        print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
