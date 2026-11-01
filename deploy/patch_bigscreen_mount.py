"""Install the tested publishing transaction while preserving the deployed runtime."""
import ast
import hashlib
import sys
from pathlib import Path


def bounds(source):
    cls = next(node for node in ast.parse(source).body if isinstance(node, ast.ClassDef) and node.name == 'BigscreenService')
    method = next(node for node in cls.body if isinstance(node, ast.FunctionDef) and node.name == '_deploy_archive')
    return min([method.lineno] + [node.lineno for node in method.decorator_list]) - 1, method.end_lineno


source_path, target_path = map(Path, sys.argv[1:3])
source = source_path.read_text(encoding='utf-8')
target = target_path.read_text(encoding='utf-8')
assert hashlib.sha256(target.encode()).hexdigest() == sys.argv[3], 'Base runtime differs; inspect before patching'
first, last = bounds(source)
begin, end = bounds(target)
replacement = source.splitlines(keepends=True)[first:last]
assert 'incoming_zip.replace(zip_path)' in ''.join(replacement)
lines = target.splitlines(keepends=True)
result = ''.join(lines[:begin] + replacement + lines[end:])
compile(result, str(target_path), 'exec')
target_path.write_text(result, encoding='utf-8')
assert result.startswith(''.join(lines[:begin])) and result.endswith(''.join(lines[end:]))
source_path.unlink()
