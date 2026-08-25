from __future__ import annotations

import re
from typing import Any, Mapping


def validate_password(password: str, policy: Mapping[str, Any] | None = None) -> None:
    settings = dict(policy or {})
    minimum = int(settings.get("min-length") or 8)
    maximum = int(settings.get("max-length") or 128)
    required_classes = int(settings.get("required-character-classes") or 3)
    value = str(password or "")
    if len(value) < minimum:
        raise ValueError(f"密码长度不能少于 {minimum} 位")
    if len(value) > maximum:
        raise ValueError(f"密码长度不能超过 {maximum} 位")
    character_classes = sum(
        bool(re.search(pattern, value))
        for pattern in (r"[a-z]", r"[A-Z]", r"\d", r"[^A-Za-z0-9]")
    )
    if character_classes < required_classes:
        raise ValueError(
            f"密码必须包含大写字母、小写字母、数字、特殊字符中的至少 {required_classes} 类"
        )
