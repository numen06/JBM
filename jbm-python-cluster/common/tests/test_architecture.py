import ast
from pathlib import Path

ROOT = Path(__file__).parents[2]
APPS = ("auth", "bigscreen", "center", "doc", "gateway", "job", "logs", "push")


def test_deployable_apps_do_not_import_each_other() -> None:
    violations: list[str] = []
    for app in APPS:
        source = ROOT / app / "src"
        for path in source.rglob("*.py"):
            tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
            for node in ast.walk(tree):
                modules = []
                if isinstance(node, ast.ImportFrom) and node.module:
                    modules.append(node.module)
                elif isinstance(node, ast.Import):
                    modules.extend(alias.name for alias in node.names)
                for module in modules:
                    parts = module.split(".")
                    if len(parts) > 2 and parts[:2] == ["jbm_cluster_py", "platform"]:
                        imported_app = parts[2]
                        if imported_app in APPS and imported_app != app:
                            violations.append(
                                f"{path.relative_to(ROOT)}:{node.lineno} imports {imported_app}"
                            )

    assert not violations, "cross-application imports:\n" + "\n".join(violations)
