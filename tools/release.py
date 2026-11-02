"""Single release entry: reserve -> commit/push -> freeze -> build -> verify.

Uses the existing authenticated App2Docker pipeline. Never deploys implicitly.
"""
from __future__ import annotations
import argparse
from datetime import datetime, timezone, timedelta
import json
from pathlib import Path
import re
import subprocess
import sys
import yaml
from check_mainline import git, validate as check_mainline

ROOT = Path(__file__).resolve().parents[1]


def catalog(root=ROOT):
    return json.loads((root / 'deploy/release-catalog.json').read_text(encoding='utf-8'))


def next_version(series, date, refs):
    prefix = series + '-' + date + '.'
    numbers = [int(m.group(1)) for ref in refs for m in [re.search(re.escape(prefix) + r'([1-9][0-9]*)$', ref)] if m]
    return prefix + str(max(numbers, default=0) + 1)


def profile(config, component, version):
    args = {**component.get('buildArgs', {}), 'RELEASE_VERSION': version, 'JBM_COMMIT': '{full_commit}',
            'RELEASE_TARGET': component['target']}
    return {'version': '1.0', 'build': {'project_type': 'python', 'use_project_dockerfile': True,
            'dockerfile_name': component['dockerfile'], 'platforms': ['linux/amd64', 'linux/arm64'], 'build_args': args},
            'git': {'branch': config['mainline']},
            'image': {'name': component['image'], 'tag': version, 'push': False}, 'push_mode': 'single',
            'release': {'enabled': True, 'platform': config['platform'], 'mainline': config['mainline'],
                        'version_file': 'VERSION', 'timezone': 'Asia/Shanghai', 'immutable': True, 'require_git_tag': True}}


def render(root, config, version):
    for name, component in config['components'].items():
        path = root / ('.app2docker-release-' + name + '.yaml')
        path.write_text(yaml.safe_dump(profile(config, component, version), sort_keys=False), encoding='utf-8', newline='\n')


def check_generated(root=ROOT):
    config = catalog(root)
    version = (root / 'VERSION').read_text().strip()
    match = re.fullmatch(config['newFormalTagPattern'], version)
    if not match or config['mainline'] != config['versionSeries']:
        raise RuntimeError('VERSION 或主线不符合发布规则')
    datetime.strptime(match.group(1), '%Y%m%d')
    for name, component in config['components'].items():
        actual = yaml.safe_load((root / ('.app2docker-release-' + name + '.yaml')).read_text(encoding='utf-8'))
        if actual != profile(config, component, version):
            raise RuntimeError('生成的发布配置有差异；不要手改 profile：' + name)
    return version


def reserve():
    config = catalog()
    check_mainline(ROOT, release=True)
    refs = git(ROOT, 'ls-remote', '--tags', 'origin').splitlines()
    date = datetime.now(timezone(timedelta(hours=8))).strftime('%Y%m%d')
    version = next_version(config['mainline'], date, refs)
    ref = 'refs/tags/release-reservations/' + version
    # Atomic Git create arbitrates concurrent allocators; a collision aborts.
    git(ROOT, 'push', 'origin', 'HEAD:' + ref)
    git(ROOT, 'fetch', 'origin', ref + ':' + ref)
    (ROOT / 'VERSION').write_text(version + '\n', encoding='utf-8')
    render(ROOT, config, version)
    print(json.dumps({'version': version, 'reserved': True, 'next': 'commit/push generated files, then freeze'}))


def freeze():
    version = check_generated()
    proof = check_mainline(ROOT, release=True)
    reservation = git(ROOT, 'ls-remote', 'origin', 'refs/tags/release-reservations/' + version)
    if not reservation:
        raise RuntimeError('未预留版本，请先 reserve')
    ref = 'refs/tags/' + version
    if git(ROOT, 'ls-remote', 'origin', ref):
        raise RuntimeError('版本已冻结，不可复用；请分配下一版本')
    git(ROOT, 'push', 'origin', proof['commit'] + ':' + ref)
    git(ROOT, 'fetch', 'origin', ref + ':' + ref)
    print(json.dumps({**proof, 'version': version, 'frozen': True}))


def client():
    from app2docker_cli.cli import APIClient, load_config
    return APIClient(load_config())


def build(component):
    config = catalog()
    version = check_generated()
    proof = check_mainline(ROOT, release=True)
    if component not in config['components']:
        raise RuntimeError('未知组件')
    ref = git(ROOT, 'ls-remote', 'origin', 'refs/tags/' + version)
    if not ref or ref.split()[0] != proof['commit']:
        raise RuntimeError('必须从已经冻结的主线提交构建')
    api = client()
    pipeline_id = config['pipelineId']
    rows = api.request('GET', '/api/pipelines', query={'team_id': config['teamId'], 'page_size': 1000})['pipelines']
    pipeline = next(p for p in rows if p['pipeline_id'] == pipeline_id)
    if pipeline.get('current_task_id') or pipeline.get('task_queue'):
        raise RuntimeError('原流水线仍有任务，请先完成当前任务')
    out = ROOT / '.release-output' / version
    out.mkdir(parents=True, exist_ok=True)
    state = out / (component + '.json')
    if state.exists():
        raise RuntimeError('组件已提交构建；请查看 status，失败重新分配版本，不重复推送')
    row = config['components'][component]
    # The registry must report explicit absence; auth/network errors fail closed.
    probe = subprocess.run(['docker', 'manifest', 'inspect', row['image'] + ':' + version], capture_output=True, text=True)
    if probe.returncode == 0 or not any(x in probe.stderr.lower() for x in ('no such manifest', 'manifest unknown', 'manifest_unknown')):
        raise RuntimeError('镜像版本已存在，或仓库无法核对；禁止覆盖，先检查仓库')
    update = {'branch': config['mainline'], 'profile': 'release-' + component, 'tag': version, 'push': True,
              'project_type': 'python', 'use_project_dockerfile': True, 'dockerfile_name': row['dockerfile'],
              'selected_services': [], 'service_push_config': {}, 'push_mode': 'multi',
              'tag_build_enabled': True, 'image_name': row['image']}
    api.request('PUT', '/api/pipelines/' + pipeline_id, body=update)
    state.write_text(json.dumps({'platform': config['platform'], 'component': component, 'version': version,
                               'commit': proof['commit'], 'status': 'submitting'}), encoding='utf-8')
    try:
        task = api.request('POST', '/api/pipelines/' + pipeline_id + '/run',
                           body={'ref_type': 'tag', 'ref_name': version, 'trigger_source': 'cli'})
        state.write_text(json.dumps({'platform': config['platform'], 'component': component, 'version': version,
                                    'commit': proof['commit'], **task}), encoding='utf-8')
    finally:
        api.request('PUT', '/api/pipelines/' + pipeline_id, body={'profile': 'default', 'tag': 'release-version-required',
                    'push': False, 'tag_build_enabled': False, 'image_name': '', 'selected_services': [], 'service_push_config': {}})
    print(state.read_text())


def status(component):
    config = catalog()
    version = check_generated()
    state = ROOT / '.release-output' / version / (component + '.json')
    saved = json.loads(state.read_text())
    api = client()
    tasks = []
    for task_id in saved.get('task_ids') or [saved['task_id']]:
        task = api.request('GET', '/api/build-tasks/' + task_id, query={'team_id': config['teamId']})
        (state.parent / (component + '-task.private.json')).write_text(json.dumps(task), encoding='utf-8')
        resolved = task.get('task_config') or {}
        commit = resolved.get('config_commit')
        if task['status'] == 'completed' and (commit != saved['commit'] or task['tag'] != version):
            raise RuntimeError('构建源码或版本与冻结清单不符')
        tasks.append({'task_id': task_id, 'status': task['status'], 'version': task.get('tag'), 'commit': commit,
                      'error': task.get('error')})
    print(json.dumps(tasks, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['reserve', 'freeze', 'check', 'build', 'status'])
    parser.add_argument('component', nargs='?')
    args = parser.parse_args()
    try:
        if args.action in ('build', 'status'):
            if not args.component:
                parser.error('需要组件名')
            globals()[args.action](args.component)
        elif args.action == 'check':
            print(json.dumps({'version': check_generated(), **check_mainline(ROOT, release=True)}))
        else:
            globals()[args.action]()
    except Exception as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
