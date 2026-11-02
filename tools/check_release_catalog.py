"""Check the current release entry catalog and archived-file integrity."""
from __future__ import annotations

import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import re

import yaml


def validate(root: Path, version: str | None = None) -> dict:
    root = root.resolve()
    catalog = json.loads((root / 'deploy/release-catalog.json').read_text(encoding='utf-8'))

    def local(relative):
        path = (root / relative).resolve()
        if not path.is_relative_to(root):
            raise ValueError('Release path escapes repository')
        return path

    if version is not None:
        match = re.fullmatch(catalog['newFormalTagPattern'], version)
        if not match:
            raise ValueError('正式镜像版本必须为 ' + catalog['versionSeries'] + '-YYYYMMDD.N')
        try:
            datetime.strptime(match.group(1), '%Y%m%d')
        except ValueError:
            raise ValueError('正式镜像版本中的日期无效') from None
    if not local(catalog['standard']).is_file():
        raise ValueError('Missing release standard')
    declared = {row['file'] for row in catalog['profiles']}
    actual = {path.name for path in root.glob('.app2docker*.yaml')} | {path.name for path in root.glob('.app2docker*.yml')}
    if actual != declared:
        raise ValueError('Root release profiles differ from the current catalog')
    for row in catalog['profiles']:
        config = yaml.safe_load(local(row['file']).read_text(encoding='utf-8-sig'))
        if row.get('defaultPushDisabled') and (
            config.get('image', {}).get('push') is not False
            or config.get('image', {}).get('tag') != 'release-version-required'
            or any(item.get('push') is not False for item in config.get('services', []))
        ):
            raise ValueError('Default template must not publish unassigned versions')
        if not local(config['build'].get('dockerfile_name', 'Dockerfile')).is_file():
            raise ValueError('Missing active Dockerfile: ' + row['file'])
    archive = json.loads(local(catalog['archive']).read_text(encoding='utf-8'))
    for item in archive['files']:
        if local(item['original']).exists():
            raise ValueError('Archived file unexpectedly remains an active entry: ' + item['original'])
        path = local(item['archived'])
        raw = path.read_bytes() if path.is_file() else b''
        expected = item.get('sha256Lf', item['sha256'])
        actual = hashlib.sha256(raw.replace(b'\r\n', b'\n') if 'sha256Lf' in item else raw).hexdigest()
        if not path.is_file() or actual != expected:
            raise ValueError('Archived file changed or is missing: ' + item['archived'])
    if catalog.get('versionAutomation') == 'tools/release.py':
        from release import check_generated
        checked = check_generated(root)
        if version is not None and version != checked:
            raise ValueError('VERSION 与待发布版本不一致')
    return {'valid': True, 'platform': catalog['platform'], 'mainline': catalog['mainline'],
            'versionSeries': catalog['versionSeries'], 'checkedVersion': version,
            'activeProfiles': len(declared), 'verifiedArchivedFiles': len(archive['files']),
            'mainlineMigrationPending': catalog['mainline'] != catalog['versionSeries'],
            'scope': 'catalog references and naming; not a deployment or remote version reservation'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--version')
    args = parser.parse_args()
    try:
        result = validate(args.repo, args.version)
    except (ValueError, OSError, KeyError) as error:
        parser.exit(1, str(error) + '\n')
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
