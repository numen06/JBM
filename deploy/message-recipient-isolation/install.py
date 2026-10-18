"""Patch only the recipient checks; keep other deployed push integrations intact."""
import ast
from pathlib import Path


def methods(source):
    cls = next(node for node in ast.parse(source).body if isinstance(node, ast.ClassDef) and node.name == 'PushService')
    return {node.name: node for node in cls.body if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))}


root = Path('/app/push/src/jbm_cluster_py/platform/push')
path = root / 'push_message_repository.py'
source = path.read_text()
for previous, replacement in [
    ('(i.rec_user_id = :current_user_id OR i.rec_user_id = 0)', 'i.rec_user_id = :current_user_id'),
    ('(i.rec_user_id = :user_id OR i.rec_user_id = 0)', 'i.rec_user_id = :user_id'),
]:
    assert source.count(previous) == 1, 'Unexpected repository recipient predicate'
    source = source.replace(previous, replacement)
compile(source, str(path), 'exec')
path.write_text(source)

path = root / 'service.py'
source = path.read_text()
desired = (Path(__file__).parent / 'service.py').read_text()
current_methods, new_methods = methods(source), methods(desired)
names = {'add_subscriber', 'broadcast', 'page_messages', 'unread_count'}
lines = source.splitlines(keepends=True)
for name in sorted(names, key=lambda key: current_methods[key].lineno, reverse=True):
    old, new = current_methods[name], new_methods[name]
    lines[old.lineno - 1:old.end_lineno] = desired.splitlines(keepends=True)[new.lineno - 1:new.end_lineno]
patched = ''.join(lines)
before, after = ast.parse(source), ast.parse(patched)
for tree in [before, after]:
    cls = next(node for node in tree.body if isinstance(node, ast.ClassDef) and node.name == 'PushService')
    cls.body = [node for node in cls.body if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) or node.name not in names]
assert ast.dump(before) == ast.dump(after), 'Unrelated push code changed'
compile(patched, str(path), 'exec')
path.write_text(patched)
print('Push recipient isolation installed; unrelated runtime retained')
