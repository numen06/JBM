"""Keep existing Push integrations while adding tenant administrator audiences."""
import ast
from pathlib import Path


def find_class(tree, name):
    return next(node for node in tree.body if isinstance(node, ast.ClassDef) and node.name == name)


root = Path('/app/push/src/jbm_cluster_py/platform/push')
for filename, cls_name, names in [
    ('service.py', 'PushService', {'handle_push_event', '_deliver_tenant_admins', '_defer_tenant_message'}),
    ('push_message_repository.py', 'PushMessageRepository', {'tenant_admin_recipients'}),
]:
    path = root / filename
    source = path.read_text()
    desired = (Path(__file__).parent / filename).read_text()
    before = ast.parse(source)
    current_cls = find_class(before, cls_name)
    new_cls = find_class(ast.parse(desired), cls_name)
    current_methods = {node.name: node for node in current_cls.body if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))}
    new_methods = {node.name: node for node in new_cls.body if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))}
    lines = source.splitlines(keepends=True)
    changes = []
    for name in names:
        new = new_methods[name]
        replacement = desired.splitlines(keepends=True)[new.lineno - 1:new.end_lineno]
        if name in current_methods:
            old = current_methods[name]
            changes.append((old.lineno - 1, old.end_lineno, replacement))
        else:
            changes.append((current_cls.end_lineno, current_cls.end_lineno, ['\n\n'] + replacement + ['\n']))
    for start, end, replacement in sorted(changes, key=lambda item: item[0], reverse=True):
        lines[start:end] = replacement
    patched = ''.join(lines)
    after = ast.parse(patched)
    for tree in [before, after]:
        cls = find_class(tree, cls_name)
        cls.body = [node for node in cls.body if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) or node.name not in names]
    assert ast.dump(before) == ast.dump(after), 'Unrelated Push code changed'
    compile(patched, str(path), 'exec')
    path.write_text(patched)
print('Tenant notification audience installed; existing recipient isolation retained')
