"""Check the complete stack against its immutable, public release manifest.

Input snapshot: docker service inspect $(docker service ls -q).
Snapshots contain environment secrets: keep them outside Git and never print them.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import re


def validate(snapshot, manifest):
    expected = {row['service']: row for row in manifest['services']}
    if len(expected) != len(manifest['services']):
        return {'passed': False, 'issues': [{'issue': 'duplicate-manifest-service'}], 'businessWrites': 0}
    namespaces = set(manifest['scopeStacks'])
    actual = {row['Spec']['Name']: row for row in snapshot if (
        row['Spec'].get('Labels', {}).get('com.docker.stack.namespace') in namespaces
        or any(row['Spec']['Name'].startswith(stack + '_') for stack in namespaces))}
    issues = []
    for name in sorted(set(expected) - set(actual)):
        issues.append({'service': name, 'issue': 'missing-service'})
    for name in sorted(set(actual) - set(expected)):
        issues.append({'service': name, 'issue': 'unmanaged-or-retired-service'})
    counts = {}
    for name, target in expected.items():
        if name not in actual:
            continue
        spec = actual[name]['Spec']
        labels = spec.get('Labels', {})
        image = spec['TaskTemplate']['ContainerSpec']['Image']
        stack = target['stack']
        counts[stack] = counts.get(stack, 0) + 1
        if labels.get('com.docker.stack.namespace') != stack:
            issues.append({'service': name, 'issue': 'wrong-or-missing-stack-label'})
        if image != target['image'] or not re.search(r'@sha256:[0-9a-f]{64}$', image):
            issues.append({'service': name, 'issue': 'image-or-digest-differs'})
        if ':' + target['version'] + '@sha256:' not in image:
            issues.append({'service': name, 'issue': 'wrong-image-version'})
        for key, value in {
            'com.docker.stack.image': target['image'],
            'io.feige.release.version': target['version'],
            'io.feige.release.revision': target['sourceCommit'],
            'io.feige.release.branch': target['mainline'],
        }.items():
            if labels.get(key) != value:
                issues.append({'service': name, 'issue': 'release-metadata-differs', 'field': key})
        if not re.fullmatch(re.escape(target['mainline']) + r'-\d{8}\.[1-9][0-9]*', target['version']):
            issues.append({'service': name, 'issue': 'mainline-version-format'})
        state = actual[name].get('UpdateStatus', {}).get('State')
        if state in ['updating', 'paused', 'rollback_started', 'rollback_paused']:
            issues.append({'service': name, 'issue': 'deployment-not-completed', 'state': state})
        policy = target.get('deployment', {})
        if policy.get('singleActiveWorker'):
            replicas = spec.get('Mode', {}).get('Replicated', {}).get('Replicas')
            if replicas != 1:
                issues.append({'service': name, 'issue': 'single-active-worker-replicas'})
        for key, value in target.get('healthcheck', {}).items():
            if spec['TaskTemplate']['ContainerSpec'].get('Healthcheck', {}).get(key) != value:
                issues.append({'service': name, 'issue': 'role-healthcheck-differs', 'field': key})
        if 'replicas' in target and spec.get('Mode', {}).get('Replicated', {}).get('Replicas') != target['replicas']:
            issues.append({'service': name, 'issue': 'replica-count-differs'})
    return {'passed': not issues, 'expectedServices': len(expected), 'actualServices': len(actual),
            'stackServiceCounts': counts, 'issues': issues, 'businessWrites': 0}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--snapshot', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    args = parser.parse_args()
    result = validate(json.loads(args.snapshot.read_text(encoding='utf-8-sig')),
                      json.loads(args.manifest.read_text(encoding='utf-8-sig')))
    print(json.dumps(result, ensure_ascii=False))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
